package com.neueda.leap.service;

import com.neueda.leap.Account;
import com.neueda.leap.Instrument;
import com.neueda.leap.Order;
import com.neueda.leap.Fill;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.FillStatus;
import com.neueda.leap.repository.OrderRepository;
import com.neueda.leap.security.AuthenticatedUser;
import com.neueda.leap.security.SecurityAccess;
import com.neueda.leap.validator.OrderValidator;
import com.neueda.leap.validator.OrderValidationRequest;
import com.neueda.leap.pricing.FeeStrategy;
import com.neueda.leap.pricing.FeeStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Order operations
 * Handles business logic, validation, and database access
 */
@Service
public class OrderService {

    public static final String ORDER_TYPE_MARKET = "MARKET";
    public static final String ORDER_TYPE_LIMIT = "LIMIT";

    private final OrderRepository orderRepository;
    private final InstrumentPricingService pricingService;
    private final MarketStatusService marketStatusService;
    private final AccountService accountService;
    private final InstrumentService instrumentService;
    private final TradeEventService tradeEventService;
    private final OrderValidator orderValidator;
    private final FeeStrategyFactory feeStrategyFactory;
    private final FillService fillService;
    private final HoldingsService holdingsService;
    private final CashEventService cashEventService;

    public OrderService(OrderRepository orderRepository,
                       InstrumentPricingService pricingService,
                       MarketStatusService marketStatusService,
                       AccountService accountService,
                       InstrumentService instrumentService,
                       TradeEventService tradeEventService,
                       OrderValidator orderValidator,
                       FeeStrategyFactory feeStrategyFactory,
                       FillService fillService,
                       HoldingsService holdingsService) {
        this(orderRepository, pricingService, marketStatusService, accountService, instrumentService,
                tradeEventService, orderValidator, feeStrategyFactory, fillService, holdingsService, null);
    }

    @Autowired
    public OrderService(OrderRepository orderRepository,
                       InstrumentPricingService pricingService,
                       MarketStatusService marketStatusService,
                       AccountService accountService,
                       InstrumentService instrumentService,
                       TradeEventService tradeEventService,
                       OrderValidator orderValidator,
                       FeeStrategyFactory feeStrategyFactory,
                       FillService fillService,
                       HoldingsService holdingsService,
                       CashEventService cashEventService) {
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.marketStatusService = marketStatusService;
        this.accountService = accountService;
        this.instrumentService = instrumentService;
        this.tradeEventService = tradeEventService;
        this.orderValidator = orderValidator;
        this.feeStrategyFactory = feeStrategyFactory;
        this.fillService = fillService;
        this.holdingsService = holdingsService;
        this.cashEventService = cashEventService;
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
     * List all orders with an optional submission time range.
     */
    public List<Order> listAllOrders(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt,
                                     int limit, int offset) {
        return orderRepository.findAll(fromSubmittedAt, toSubmittedAt, limit, offset);
    }

    /**
     * Get total count of orders for an account
     */
    public int countOrdersByAccount(UUID accountId) {
        return orderRepository.countByAccountId(accountId);
    }

    public int countAllOrders(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt) {
        return orderRepository.countAll(fromSubmittedAt, toSubmittedAt);
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
        return createOrder(accountId, instrumentId, side, quantity, idempotencyKey, ORDER_TYPE_MARKET, null);
    }

    /**
     * Create a MARKET or LIMIT order. The order is only accepted and recorded here; it is priced
     * and executed afterwards by OrderExecutionService (BR-06). A LIMIT order is executed only if
     * the live quote is at or better than its limit, otherwise it is rejected.
     */
    public Order createOrder(UUID accountId, UUID instrumentId, String side, String quantity, String idempotencyKey,
                             String orderType, BigDecimal limitPrice) {
        // Step 1: Validate order input using validator
        orderValidator.validate(new OrderValidationRequest(side, quantity));
        String normalizedOrderType = normalizeOrderType(orderType, limitPrice);

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

        // Validate the instrument's own market is open (crypto 24/7, FX, US/UK/Indian equity sessions)
        if (!marketStatusService.isMarketOpen(instrument)) {
            throw new IllegalArgumentException("Cannot place order: Market is closed");
        }

        // Step 5: Fetch instrument price
        BigDecimal instrumentPrice = pricingService.getPrice(instrument.getAssetClass(), instrumentId);
        
        // Defensive check: Ensure pricing service returned a valid price
        if (instrumentPrice == null) {
            throw new RuntimeException("Pricing service returned null price for instrument: " + instrumentId);
        }

        // Step 6: Parse quantity and calculate order value (a LIMIT BUY can cost up to its limit)
        BigDecimal orderQuantity = new BigDecimal(quantity);
        BigDecimal referencePrice = ORDER_TYPE_LIMIT.equals(normalizedOrderType) ? limitPrice : instrumentPrice;
        BigDecimal orderValue = referencePrice.multiply(orderQuantity);

        // Step 7: Calculate fee using strategy pattern
        FeeStrategy feeStrategy = feeStrategyFactory.getStrategy(instrument.getAssetClass());
        BigDecimal fee = feeStrategy.calculateFee(orderValue);

        // Step 8: Calculate total order price
        BigDecimal totalPrice = orderValue.add(fee).setScale(5, RoundingMode.HALF_UP);

        // Reject unaffordable BUY orders before they are persisted. The fill path
        // performs the authoritative atomic debit again to handle concurrent orders.
        if (OrderSide.BUY.equals(OrderSide.valueOf(side.toUpperCase()))
                && (account.getBalance() == null
                || account.getBalance().compareTo(totalPrice) < 0)) {
            throw new IllegalArgumentException("Insufficient balance for BUY order. Required: "
                    + totalPrice + ", Available: " + account.getBalance());
        }

        // Reject SELL orders for more than is held, counting sells already awaiting execution (BR-05)
        if (OrderSide.SELL.equals(OrderSide.valueOf(side.toUpperCase()))) {
            BigDecimal held = holdingsService.getHolding(accountId, instrumentId)
                    .map(holding -> holding.getQuantity())
                    .orElse(BigDecimal.ZERO);
            BigDecimal pendingSells = orderRepository.pendingSellQuantity(accountId, instrumentId);
            BigDecimal available = held.subtract(pendingSells != null ? pendingSells : BigDecimal.ZERO);
            if (available.compareTo(orderQuantity) < 0) {
                throw new IllegalArgumentException("Insufficient holdings. Available: "
                        + available.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString() + ", Requested: " + quantity);
            }
        }

        // Step 9: Insert order into database
        orderRepository.save(accountId, instrumentId, side, quantity, idempotencyKey, normalizedOrderType,
                ORDER_TYPE_LIMIT.equals(normalizedOrderType) ? limitPrice : null);

        // Step 10: Retrieve and return the created order
        Order order = getOrderByIdempotencyKey(accountId, idempotencyKey);
        
        // Defensive check: Ensure order was persisted and retrieved successfully
        if (order == null) {
            throw new RuntimeException("Order insertion failed: order not found after insert for idempotencyKey: " + idempotencyKey);
        }
        
        // Step 11: Set totalPrice on the order object (not persisted to DB, calculated at submission time)
        order.setTotalPrice(totalPrice);

        String eventDetails = buildOrderEventDetails(account.getClientId(), accountId, instrumentId, side, quantity,
                idempotencyKey, normalizedOrderType, limitPrice);
        tradeEventService.recordOrderSubmitted(account.getClientId(), order.getOrderId(), eventDetails);
        // The order passed the trading rules and is recorded as a firm commitment (BR-06/BR-07)
        tradeEventService.recordOrderAccepted(account.getClientId(), order.getOrderId(), eventDetails);
        
        return order;
    }

    private static String normalizeOrderType(String orderType, BigDecimal limitPrice) {
        String type = orderType == null || orderType.isBlank() ? ORDER_TYPE_MARKET : orderType.trim().toUpperCase();
        if (!ORDER_TYPE_MARKET.equals(type) && !ORDER_TYPE_LIMIT.equals(type)) {
            throw new IllegalArgumentException("Unsupported order type: " + orderType);
        }
        if (ORDER_TYPE_LIMIT.equals(type) && (limitPrice == null || limitPrice.signum() <= 0)) {
            throw new IllegalArgumentException("A limit order needs a limit price greater than zero");
        }
        return type;
    }

    private String buildOrderEventDetails(UUID ownerClientId, UUID accountId, UUID instrumentId,
                                          String side, String quantity, String idempotencyKey,
                                          String orderType, BigDecimal limitPrice) {
        AuthenticatedUser authenticatedUser = SecurityAccess.currentUser();
        UUID authenticatedUserId = authenticatedUser != null && authenticatedUser.getClientId() != null
                ? authenticatedUser.getClientId()
                : ownerClientId;
        return "{\"authenticatedUserId\":\"" + authenticatedUserId
                + "\",\"accountId\":\"" + accountId
                + "\",\"instrumentId\":\"" + instrumentId
                + "\",\"side\":\"" + side
                + "\",\"quantity\":\"" + quantity
                + "\",\"idempotencyKey\":\"" + idempotencyKey
                + "\",\"orderType\":\"" + orderType
                + (limitPrice != null && ORDER_TYPE_LIMIT.equals(orderType) ? "\",\"limitPrice\":\"" + limitPrice.toPlainString() : "")
                + "\"}";
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
    @Transactional
    public Fill fillOrder(UUID orderId, BigDecimal fillPrice) {
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        if (fillPrice == null || fillPrice.signum() <= 0) {
            throw new IllegalArgumentException("Fill price must be greater than zero");
        }
        if (cashEventService == null) {
            throw new IllegalStateException("Cash event service is not configured");
        }

        BigDecimal fillQuantity = order.getQuantity();
        BigDecimal fillAmount = fillPrice.multiply(fillQuantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = calculateFillFee(order.getInstrumentId(), fillAmount);

        // BUY pays value + fee; SELL receives value - fee.
        BigDecimal cashDelta = OrderSide.BUY.equals(order.getSide())
                ? fillAmount.add(fee).negate()
                : fillAmount.subtract(fee);
        String cashEventType = OrderSide.BUY.equals(order.getSide()) ? "BUY" : "SELL";
        // Apply the cash change first. The conditional account update rejects a BUY
        // with insufficient funds; the surrounding transaction rolls back any
        // subsequent holding or fill changes if execution fails.
        cashEventService.post(order.getAccountId(), order.getOrderId(), cashEventType, cashDelta, "{}");

        holdingsService.updateHoldingFromFill(order.getAccountId(), order.getInstrumentId(),
                order.getSide(), fillQuantity);

        // Create fill record
        return fillService.createFill(orderId, fillPrice.toPlainString(), fillQuantity.toPlainString(),
                FillStatus.Filled.name(), fee.toPlainString());
    }

    private BigDecimal calculateFillFee(UUID instrumentId, BigDecimal fillAmount) {
        Instrument instrument = instrumentService.getInstrumentById(instrumentId);
        if (instrument == null || instrument.getAssetClass() == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        FeeStrategy feeStrategy = feeStrategyFactory.getStrategy(instrument.getAssetClass());
        if (feeStrategy == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return feeStrategy.calculateFee(fillAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
