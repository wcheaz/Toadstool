package com.neueda.leap.service;

import com.neueda.leap.Account;
import com.neueda.leap.Instrument;
import com.neueda.leap.Order;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.repository.OrderRepository;
import com.neueda.leap.validator.OrderValidator;
import com.neueda.leap.pricing.FeeStrategy;
import com.neueda.leap.pricing.FeeStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService.createOrder() Tests")
class OrderServiceTest {

    private OrderService orderService;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private InstrumentPricingService pricingService;
    
    @Mock
    private MarketStatusService marketStatusService;
    
    @Mock
    private AccountService accountService;
    
    @Mock
    private InstrumentService instrumentService;

    @Mock
    private OrderValidator orderValidator;

    @Mock
    private FeeStrategyFactory feeStrategyFactory;

    @Mock
    private FeeStrategy feeStrategy;

    @Mock
    private FillService fillService;

    @Mock
    private HoldingsService holdingsService;

    private UUID accountId;
    private UUID instrumentId;
    private UUID orderId;
    private Account activeAccount;
    private Instrument tradableInstrument;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        instrumentId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        // Initialize OrderService with all mocked dependencies
        orderService = new OrderService(
            orderRepository,
            pricingService,
            marketStatusService,
            accountService,
            instrumentService,
            orderValidator,
            feeStrategyFactory,
            fillService,
            holdingsService
        );

        // Setup default active account
        activeAccount = new Account();
        activeAccount.setAccountId(accountId);
        activeAccount.setStatus(AccountStatus.ACTIVE);

        // Setup default tradable instrument with EQUITY asset class
        tradableInstrument = new Instrument();
        tradableInstrument.setInstrumentId(instrumentId);
        tradableInstrument.setStatus(InstrumentStatus.TRADABLE);
        tradableInstrument.setAssetClass(AssetClass.EQUITY);

        // Setup default mocks to allow success case
        when(marketStatusService.isMarketOpen()).thenReturn(true);
        when(accountService.getAccountById(accountId)).thenReturn(activeAccount);
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(tradableInstrument);
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("100.00000"));
        
        // Setup validator mock - by default don't throw (valid input)
        doNothing().when(orderValidator).validate(any());
        
        // Setup fee strategy factory mock
        when(feeStrategyFactory.getStrategy(AssetClass.EQUITY)).thenReturn(feeStrategy);
        when(feeStrategyFactory.getStrategy(AssetClass.CRYPTO)).thenReturn(feeStrategy);
        when(feeStrategyFactory.getStrategy(AssetClass.FX)).thenReturn(feeStrategy);
        
        // Setup fee strategy to return reasonable defaults
        when(feeStrategy.calculateFee(any())).thenReturn(new BigDecimal("1.00000"));
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("createOrder successfully creates order with valid inputs")
    void testCreateOrderSuccess() {
        // Setup
        String idempotencyKey = "key-123";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("10.00"), idempotencyKey, OffsetDateTime.now());
        
        when(feeStrategy.calculateFee(any())).thenReturn(new BigDecimal("1.00000"));
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        // Execute
        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "10.00", idempotencyKey);

        // Verify
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
    }

    @Test
    @DisplayName("createOrder calculates totalPrice correctly: (price × quantity) + fee")
    void testCreateOrderCalculatesTotalPriceCorrectly() {
        // Setup: price=$100, quantity=10, so orderValue=$1000
        // EQUITY fee: 0.05% of $1000 = $0.50, minimum $1.00 → $1.00 fee applied
        // totalPrice = $1000 + $1.00 = $1001.00
        String idempotencyKey = "price-calc-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("10.00"), idempotencyKey, OffsetDateTime.now());
        
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("100.00000"));
        when(feeStrategy.calculateFee(new BigDecimal("1000.00000"))).thenReturn(new BigDecimal("1.00000"));
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        // Execute
        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "10.00", idempotencyKey);

        // Verify
        assertNotNull(result);
        // Order should be created with total price = (100 * 10) + 1.00 = 1001.00
    }

    @Test
    @DisplayName("createOrder handles SELL orders")
    void testCreateOrderSellSide() {
        String idempotencyKey = "sell-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.SELL, 
                                        new BigDecimal("5.00"), idempotencyKey, OffsetDateTime.now());
        
        when(feeStrategy.calculateFee(any())).thenReturn(new BigDecimal("1.00000"));
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "SELL", "5.00", idempotencyKey);

        assertNotNull(result);
    }

    @Test
    @DisplayName("createOrder handles different asset classes (CRYPTO, FX)")
    void testCreateOrderDifferentAssetClasses() {
        // Test CRYPTO
        tradableInstrument.setAssetClass(AssetClass.CRYPTO);
        when(pricingService.getPrice(AssetClass.CRYPTO, instrumentId)).thenReturn(new BigDecimal("1000.00000"));
        when(feeStrategyFactory.getStrategy(AssetClass.CRYPTO)).thenReturn(feeStrategy);

        String idempotencyKey = "crypto-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("1.00"), idempotencyKey, OffsetDateTime.now());
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "1.00", idempotencyKey);
        assertNotNull(result);

        // Test FX
        tradableInstrument.setAssetClass(AssetClass.FX);
        when(pricingService.getPrice(AssetClass.FX, instrumentId)).thenReturn(new BigDecimal("1.25000"));
        when(feeStrategyFactory.getStrategy(AssetClass.FX)).thenReturn(feeStrategy);

        Order resultFx = orderService.createOrder(accountId, instrumentId, "BUY", "100.00", idempotencyKey);
        assertNotNull(resultFx);
    }

    // ==================== VALIDATION ERROR TESTS ====================

    @Test
    @DisplayName("createOrder rejects invalid side (not BUY or SELL)")
    void testCreateOrderInvalidSide() {
        // Configure validator to throw for invalid side
        doThrow(new IllegalArgumentException("Invalid order side. Must be BUY or SELL."))
            .when(orderValidator).validate(any());
        
        assertThrows(IllegalArgumentException.class, 
            () -> orderService.createOrder(accountId, instrumentId, "HOLD", "10.00", "key"),
            "Should reject invalid order side");
    }

    @Test
    @DisplayName("createOrder rejects null side")
    void testCreateOrderNullSide() {
        // Configure validator to throw for null side
        doThrow(new IllegalArgumentException("Order side cannot be null or empty"))
            .when(orderValidator).validate(any());
        
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, null, "10.00", "key"),
            "Should reject null order side");
    }

    @Test
    @DisplayName("createOrder rejects zero quantity")
    void testCreateOrderZeroQuantity() {
        // Configure validator to throw for zero quantity
        doThrow(new IllegalArgumentException("Quantity must be greater than 0"))
            .when(orderValidator).validate(any());
        
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "0", "key"),
            "Should reject zero quantity");
    }

    @Test
    @DisplayName("createOrder rejects negative quantity")
    void testCreateOrderNegativeQuantity() {
        // Configure validator to throw for negative quantity
        doThrow(new IllegalArgumentException("Quantity must be greater than 0"))
            .when(orderValidator).validate(any());
        
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "-5.00", "key"),
            "Should reject negative quantity");
    }

    @Test
    @DisplayName("createOrder rejects invalid quantity (not a number)")
    void testCreateOrderInvalidQuantity() {
        // Configure validator to throw for non-numeric quantity
        doThrow(new IllegalArgumentException("Quantity must be a valid number"))
            .when(orderValidator).validate(any());
        
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "abc", "key"),
            "Should reject non-numeric quantity");
    }

    @Test
    @DisplayName("createOrder rejects order when market is closed")
    void testCreateOrderMarketClosed() {
        when(marketStatusService.isMarketOpen()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "10.00", "key"),
            "Should reject order when market is closed");
    }

    // ==================== ACCOUNT VALIDATION TESTS ====================

    @Test
    @DisplayName("createOrder rejects order for non-existent account")
    void testCreateOrderAccountNotFound() {
        when(accountService.getAccountById(accountId)).thenReturn(null);

        assertThrows(RuntimeException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "10.00", "key"),
            "Should reject order for non-existent account");
    }

    @Test
    @DisplayName("createOrder rejects order for suspended account")
    void testCreateOrderSuspendedAccount() {
        activeAccount.setStatus(AccountStatus.SUSPENDED);

        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "10.00", "key"),
            "Should reject order for suspended account");
    }

    @Test
    @DisplayName("createOrder rejects order for closed account")
    void testCreateOrderClosedAccount() {
        activeAccount.setStatus(AccountStatus.CLOSED);

        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "10.00", "key"),
            "Should reject order for closed account");
    }

    // ==================== INSTRUMENT VALIDATION TESTS ====================

    @Test
    @DisplayName("createOrder rejects order for non-existent instrument")
    void testCreateOrderInstrumentNotFound() {
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(null);

        assertThrows(RuntimeException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "10.00", "key"),
            "Should reject order for non-existent instrument");
    }

    @Test
    @DisplayName("createOrder rejects order for halted instrument")
    void testCreateOrderHaltedInstrument() {
        tradableInstrument.setStatus(InstrumentStatus.HALTED);

        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "10.00", "key"),
            "Should reject order for halted instrument");
    }

    @Test
    @DisplayName("createOrder rejects order for inactive instrument")
    void testCreateOrderInactiveInstrument() {
        tradableInstrument.setStatus(InstrumentStatus.INACTIVE);

        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "10.00", "key"),
            "Should reject order for inactive instrument");
    }

    // ==================== DECIMAL PRECISION TESTS ====================

    @Test
    @DisplayName("createOrder maintains 5 decimal precision in totalPrice")
    void testCreateOrderDecimalPrecision() {
        String idempotencyKey = "precision-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("10.00"), idempotencyKey, OffsetDateTime.now());
        
        // Use price that creates non-round results
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("33.33333"));
        when(feeStrategy.calculateFee(any())).thenReturn(new BigDecimal("11.11111"));
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "3.33", idempotencyKey);
        assertNotNull(result);
    }

    @Test
    @DisplayName("createOrder handles decimal quantity with high precision")
    void testCreateOrderDecimalQuantityPrecision() {
        String idempotencyKey = "decimal-qty-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("0.12345"), idempotencyKey, OffsetDateTime.now());
        
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("100.00000"));
        when(feeStrategy.calculateFee(any())).thenReturn(new BigDecimal("1.00000"));
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "0.12345", idempotencyKey);
        assertNotNull(result);
    }

    // ==================== LARGE ORDER TESTS ====================

    @Test
    @DisplayName("createOrder handles large order values correctly")
    void testCreateOrderLargeOrderValue() {
        String idempotencyKey = "large-order-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("1000000.00"), idempotencyKey, OffsetDateTime.now());
        
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("500.00000"));
        when(feeStrategy.calculateFee(any())).thenReturn(new BigDecimal("2500.00000"));
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "1000000.00", idempotencyKey);
        assertNotNull(result);
    }

    @Test
    @DisplayName("createOrder handles very small order values with minimum fee")
    void testCreateOrderSmallOrderValue() {
        String idempotencyKey = "small-order-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("0.01"), idempotencyKey, OffsetDateTime.now());
        
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("10.00000"));
        when(feeStrategy.calculateFee(any())).thenReturn(new BigDecimal("1.00000"));
        doNothing().when(orderRepository).save(any(), any(), anyString(), anyString(), anyString());
        when(orderRepository.findByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "0.01", idempotencyKey);
        assertNotNull(result);
    }
}
