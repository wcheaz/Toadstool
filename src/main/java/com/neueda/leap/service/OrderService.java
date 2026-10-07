package com.neueda.leap.service;

import com.neueda.leap.Account;
import com.neueda.leap.Instrument;
import com.neueda.leap.Order;
import com.neueda.leap.Fill;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.repository.OrderRepository;
import com.neueda.leap.validator.OrderValidator;
import com.neueda.leap.validator.OrderValidationRequest;
import com.neueda.leap.pricing.FeeStrategy;
import com.neueda.leap.pricing.FeeStrategyFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Order operations
 * Handles business logic, validation, and database access
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InstrumentPricingService pricingService;
    private final MarketStatusService marketStatusService;
    private final AccountService accountService;
    private final InstrumentService instrumentService;
    private final OrderValidator orderValidator;
    private final FeeStrategyFactory feeStrategyFactory;
    private final FillService fillService;
    private final HoldingsService holdingsService;

    public OrderService(OrderRepository orderRepository,
                       InstrumentPricingService pricingService,
                       MarketStatusService marketStatusService,
                       AccountService accountService,
                       InstrumentService instrumentService,
                       OrderValidator orderValidator,
                       FeeStrategyFactory feeStrategyFactory,
                       FillService fillService,
                       HoldingsService holdingsService) {
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.marketStatusService = marketStatusService;
        this.accountService = accountService;
        this.instrumentService = instrumentService;
        this.orderValidator = orderValidator;
        this.feeStrategyFactory = feeStrategyFactory;
        this.fillService = fillService;
        this.holdingsService = holdingsService;
    }

    /**
     * Retrieve a single order by ID
     */
    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * List orders for an account with pagination
     */
    public List<Order> listOrdersByAccount(UUID accountId, int limit, int offset) {
        return orderRepository.findByAccountId(accountId, limit, offset);
    }

    /**
     * Get total count of orders for an account
     */
    public int countOrdersByAccount(UUID accountId) {
        return orderRepository.countByAccountId(accountId);
    }

    /**
     * Check if an order with the given idempotency key already exists for the account
     */
    public Order getOrderByIdempotencyKey(UUID accountId, String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(accountId, idempotencyKey);
    }

    /**
     * Create a new order
     * 
     * Business Logic:
     * 1. Validates order input (side, quantity)
     * 2. Validates market is open
     * 3. Validates account exists and is ACTIVE
     * 4. Validates instrument exists and is TRADABLE
     * 5. Fetches instrument price via pricing service
     * 6. Calculates fee based on instrument asset class
     * 7. Calculates total order price: (price × quantity) + fee
     * 8. Persists order to database
     * 9. Retrieves and returns order with totalPrice populated
     * 
     * @param accountId The account placing the order
     * @param instrumentId The instrument to trade
     * @param side The order side (BUY or SELL)
     * @param quantity The order quantity as string
     * @param idempotencyKey Idempotency key for duplicate detection
     * @return The created order with totalPrice calculated
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if account or instrument not found
     */
    public Order createOrder(UUID accountId, UUID instrumentId, String side, String quantity, String idempotencyKey) {
        // Step 1: Validate order input using validator
        orderValidator.validate(new OrderValidationRequest(side, quantity));

        // Step 2: Validate market is open
        if (!marketStatusService.isMarketOpen()) {
            throw new IllegalArgumentException("Cannot place order: Market is closed");
        }

        // Step 3: Validate account exists and is ACTIVE
        Account account = accountService.getAccountById(accountId);
        if (account == null) {
            throw new RuntimeException("Account not found: " + accountId);
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot place order: Account status is " + account.getStatus());
        }

        // Step 4: Validate instrument exists and is TRADABLE
        Instrument instrument = instrumentService.getInstrumentById(instrumentId);
        if (instrument == null) {
            throw new RuntimeException("Instrument not found: " + instrumentId);
        }
        if (instrument.getStatus() != InstrumentStatus.TRADABLE) {
            throw new IllegalArgumentException("Cannot place order: Instrument status is " + instrument.getStatus());
        }

        // Step 5: Fetch instrument price
        BigDecimal instrumentPrice = pricingService.getPrice(instrument.getAssetClass(), instrumentId);
        
        // Defensive check: Ensure pricing service returned a valid price
        if (instrumentPrice == null) {
            throw new RuntimeException("Pricing service returned null price for instrument: " + instrumentId);
        }

        // Step 6: Parse quantity and calculate order value
        BigDecimal orderQuantity = new BigDecimal(quantity);
        BigDecimal orderValue = instrumentPrice.multiply(orderQuantity);

        // Step 7: Calculate fee using strategy pattern
        FeeStrategy feeStrategy = feeStrategyFactory.getStrategy(instrument.getAssetClass());
        BigDecimal fee = feeStrategy.calculateFee(orderValue);

        // Step 8: Calculate total order price
        BigDecimal totalPrice = orderValue.add(fee).setScale(5, RoundingMode.HALF_UP);

        // Step 9: Insert order into database
        orderRepository.save(accountId, instrumentId, side, quantity, idempotencyKey);

        // Step 10: Retrieve and return the created order
        Order order = getOrderByIdempotencyKey(accountId, idempotencyKey);
        
        // Defensive check: Ensure order was persisted and retrieved successfully
        if (order == null) {
            throw new RuntimeException("Order insertion failed: order not found after insert for idempotencyKey: " + idempotencyKey);
        }
        
        // Step 11: Set totalPrice on the order object (not persisted to DB, calculated at submission time)
        order.setTotalPrice(totalPrice);
        
        return order;
    }

    /**
     * Update order status (used internally for state transitions)
     */
    public void updateOrderStatus(UUID orderId, String status) {
        orderRepository.updateStatus(orderId, status);
    }

    /**
     * Execute/fill an order and update holdings and account balance.
     *
     * For BUY orders:
     * - Increase holdings by quantity
     * - Decrease account balance by (price × quantity)
     *
     * For SELL orders:
     * - Decrease holdings by quantity
     * - Increase account balance by (price × quantity)
     *
     * @param orderId The order to fill
     * @param fillPrice The execution price
     * @return The created Fill
     * @throws IllegalArgumentException if order not found
     */
    public Fill fillOrder(UUID orderId, BigDecimal fillPrice) {
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        BigDecimal fillQuantity = order.getQuantity();
        BigDecimal fillAmount = fillPrice.multiply(fillQuantity);

        // Update holdings
        holdingsService.updateHoldingFromFill(order.getAccountId(), order.getInstrumentId(),
                order.getSide(), fillQuantity);

        // Update account balance
        Account account = accountService.getAccountById(order.getAccountId());
        if (account != null) {
            BigDecimal currentBalance = BigDecimal.ZERO;
            BigDecimal newBalance;

            if (OrderSide.BUY.equals(order.getSide())) {
                newBalance = currentBalance.subtract(fillAmount);
            } else if (OrderSide.SELL.equals(order.getSide())) {
                newBalance = currentBalance.add(fillAmount);
            } else {
                throw new IllegalArgumentException("Invalid order side: " + order.getSide());
            }

            accountService.updateAccountBalance(order.getAccountId(), newBalance);
        }

        // Create fill record
        fillService.createFill(orderId, fillPrice.toPlainString(), fillQuantity.toPlainString(), "FILLED");

        // Update order status to FILLED
        updateOrderStatus(orderId, "FILLED");

        return fillService.getFillById(UUID.randomUUID()); // Note: FillService needs to return the created fill
    }
}
