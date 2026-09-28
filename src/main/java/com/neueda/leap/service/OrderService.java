package com.neueda.leap.service;

import com.neueda.leap.Account;
import com.neueda.leap.Instrument;
import com.neueda.leap.Order;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.AssetClassFeeStructure;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.mapper.OrderMapper;
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

    private final OrderMapper orderMapper;
    private final InstrumentPricingService pricingService;
    private final MarketStatusService marketStatusService;
    private final AccountService accountService;
    private final InstrumentService instrumentService;

    public OrderService(OrderMapper orderMapper, 
                       InstrumentPricingService pricingService,
                       MarketStatusService marketStatusService,
                       AccountService accountService,
                       InstrumentService instrumentService) {
        this.orderMapper = orderMapper;
        this.pricingService = pricingService;
        this.marketStatusService = marketStatusService;
        this.accountService = accountService;
        this.instrumentService = instrumentService;
    }

    /**
     * Retrieve a single order by ID
     */
    public Order getOrderById(UUID orderId) {
        return orderMapper.selectOrderById(orderId);
    }

    /**
     * List orders for an account with pagination
     */
    public List<Order> listOrdersByAccount(UUID accountId, int limit, int offset) {
        return orderMapper.selectOrdersByAccountId(accountId, limit, offset);
    }

    /**
     * Get total count of orders for an account
     */
    public int countOrdersByAccount(UUID accountId) {
        return orderMapper.countOrdersByAccountId(accountId);
    }

    /**
     * Check if an order with the given idempotency key already exists for the account
     */
    public Order getOrderByIdempotencyKey(UUID accountId, String idempotencyKey) {
        return orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey);
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
        // Step 1: Validate order input
        validateOrderInput(side, quantity);

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

        // Step 6: Parse quantity and calculate order value
        BigDecimal orderQuantity = new BigDecimal(quantity);
        BigDecimal orderValue = instrumentPrice.multiply(orderQuantity);

        // Step 7: Calculate fee based on instrument asset class
        AssetClassFeeStructure feeStructure = AssetClassFeeStructure.fromAssetClass(instrument.getAssetClass());
        BigDecimal fee = feeStructure.calculateFee(orderValue);

        // Step 8: Calculate total order price
        BigDecimal totalPrice = orderValue.add(fee).setScale(5, RoundingMode.HALF_UP);

        // Step 9: Insert order into database
        orderMapper.insertOrder(accountId, instrumentId, side, quantity, idempotencyKey);

        // Step 10: Retrieve and return the created order
        Order order = getOrderByIdempotencyKey(accountId, idempotencyKey);
        
        // Step 11: Set totalPrice on the order object (not persisted to DB, calculated at submission time)
        order.setTotalPrice(totalPrice);
        
        return order;
    }

    /**
     * Validate order input
     */
    private void validateOrderInput(String side, String quantity) {
        if (side == null || (!side.equals("BUY") && !side.equals("SELL"))) {
            throw new IllegalArgumentException("Invalid order side. Must be BUY or SELL.");
        }

        try {
            java.math.BigDecimal qty = new java.math.BigDecimal(quantity);
            if (qty.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a valid decimal number");
        }
    }

    /**
     * Update order status (used internally for state transitions)
     */
    public void updateOrderStatus(UUID orderId, String status) {
        orderMapper.updateOrderStatus(orderId, status);
    }
}
