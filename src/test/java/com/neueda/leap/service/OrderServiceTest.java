package com.neueda.leap.service;

import com.neueda.leap.Account;
import com.neueda.leap.Instrument;
import com.neueda.leap.Order;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService.createOrder() Tests")
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;
    
    @Mock
    private OrderMapper orderMapper;
    
    @Mock
    private InstrumentPricingService pricingService;
    
    @Mock
    private MarketStatusService marketStatusService;
    
    @Mock
    private AccountService accountService;
    
    @Mock
    private InstrumentService instrumentService;

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
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("createOrder successfully creates order with valid inputs")
    void testCreateOrderSuccess() {
        // Setup
        String idempotencyKey = "key-123";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("10.00"), idempotencyKey, OffsetDateTime.now());
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        // Execute
        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "10.00", idempotencyKey);

        // Verify
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertNotNull(result.getTotalPrice());
        assertTrue(result.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
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
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        // Execute
        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "10.00", idempotencyKey);

        // Verify
        assertNotNull(result.getTotalPrice());
        // price * quantity = 100 * 10 = 1000
        // fee = max(1000 * 0.0005, 1.00) = max(0.50, 1.00) = 1.00
        // total = 1000 + 1.00 = 1001.00
        assertEquals(0, result.getTotalPrice().compareTo(new BigDecimal("1001.00000")));
    }

    @Test
    @DisplayName("createOrder handles SELL orders")
    void testCreateOrderSellSide() {
        String idempotencyKey = "sell-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.SELL, 
                                        new BigDecimal("5.00"), idempotencyKey, OffsetDateTime.now());
        
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "SELL", "5.00", idempotencyKey);

        assertNotNull(result);
        assertNotNull(result.getTotalPrice());
    }

    @Test
    @DisplayName("createOrder handles different asset classes (CRYPTO, FX)")
    void testCreateOrderDifferentAssetClasses() {
        // Test CRYPTO
        tradableInstrument.setAssetClass(AssetClass.CRYPTO);
        when(pricingService.getPrice(AssetClass.CRYPTO, instrumentId)).thenReturn(new BigDecimal("1000.00000"));

        String idempotencyKey = "crypto-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("1.00"), idempotencyKey, OffsetDateTime.now());
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "1.00", idempotencyKey);
        assertNotNull(result.getTotalPrice());

        // Test FX
        tradableInstrument.setAssetClass(AssetClass.FX);
        when(pricingService.getPrice(AssetClass.FX, instrumentId)).thenReturn(new BigDecimal("1.25000"));

        Order resultFx = orderService.createOrder(accountId, instrumentId, "BUY", "100.00", idempotencyKey);
        assertNotNull(resultFx.getTotalPrice());
    }

    // ==================== VALIDATION ERROR TESTS ====================

    @Test
    @DisplayName("createOrder rejects invalid side (not BUY or SELL)")
    void testCreateOrderInvalidSide() {
        assertThrows(IllegalArgumentException.class, 
            () -> orderService.createOrder(accountId, instrumentId, "HOLD", "10.00", "key"),
            "Should reject invalid order side");
    }

    @Test
    @DisplayName("createOrder rejects null side")
    void testCreateOrderNullSide() {
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, null, "10.00", "key"),
            "Should reject null order side");
    }

    @Test
    @DisplayName("createOrder rejects zero quantity")
    void testCreateOrderZeroQuantity() {
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "0", "key"),
            "Should reject zero quantity");
    }

    @Test
    @DisplayName("createOrder rejects negative quantity")
    void testCreateOrderNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(accountId, instrumentId, "BUY", "-5.00", "key"),
            "Should reject negative quantity");
    }

    @Test
    @DisplayName("createOrder rejects invalid quantity (not a number)")
    void testCreateOrderInvalidQuantity() {
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
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "3.33", idempotencyKey);

        assertEquals(5, result.getTotalPrice().scale(), "Total price should have 5 decimal places");
    }

    @Test
    @DisplayName("createOrder handles decimal quantity with high precision")
    void testCreateOrderDecimalQuantityPrecision() {
        String idempotencyKey = "decimal-qty-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("0.12345"), idempotencyKey, OffsetDateTime.now());
        
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("100.00000"));
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "0.12345", idempotencyKey);
        assertNotNull(result.getTotalPrice());
    }

    // ==================== LARGE ORDER TESTS ====================

    @Test
    @DisplayName("createOrder handles large order values correctly")
    void testCreateOrderLargeOrderValue() {
        String idempotencyKey = "large-order-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("1000000.00"), idempotencyKey, OffsetDateTime.now());
        
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("500.00000"));
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "1000000.00", idempotencyKey);
        assertNotNull(result.getTotalPrice());
        // order value = 500 * 1,000,000 = 500,000,000
        // totalPrice should be > order value (includes fee)
        assertTrue(result.getTotalPrice().compareTo(new BigDecimal("500000000")) > 0, 
                   "Large order totalPrice should exceed order value due to fee");
    }

    @Test
    @DisplayName("createOrder handles very small order values with minimum fee")
    void testCreateOrderSmallOrderValue() {
        String idempotencyKey = "small-order-test";
        Order expectedOrder = new Order(orderId, accountId, instrumentId, OrderSide.BUY, 
                                        new BigDecimal("0.01"), idempotencyKey, OffsetDateTime.now());
        
        when(pricingService.getPrice(AssetClass.EQUITY, instrumentId)).thenReturn(new BigDecimal("10.00000"));
        doNothing().when(orderMapper).insertOrder(any(), any(), anyString(), anyString(), anyString());
        when(orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey)).thenReturn(expectedOrder);

        Order result = orderService.createOrder(accountId, instrumentId, "BUY", "0.01", idempotencyKey);
        
        // order value = 10 * 0.01 = 0.10
        // fee = max(0.10 * 0.0005, 1.00) = 1.00 (minimum applies)
        // total = 0.10 + 1.00 = 1.10
        assertNotNull(result.getTotalPrice());
        assertTrue(result.getTotalPrice().compareTo(new BigDecimal("1.00000")) >= 0, 
                   "Should apply minimum fee for small orders");
    }
}
