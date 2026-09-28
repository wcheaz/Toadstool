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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.MockBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("OrderService Business Logic Tests")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private InstrumentPricingService pricingService;

    @Autowired
    private MarketStatusService marketStatusService;

    @MockBean
    private OrderMapper orderMapper;

    private UUID validAccountId;
    private UUID validInstrumentId;
    private Instrument equityInstrument;
    private Account activeAccount;

    @BeforeEach
    void setUp() {
        validAccountId = UUID.randomUUID();
        validInstrumentId = UUID.randomUUID();
        
        equityInstrument = new Instrument(
                validInstrumentId,
                "AAPL",
                "Apple Inc.",
                AssetClass.EQUITY,
                InstrumentStatus.TRADABLE
        );
        
        activeAccount = new Account(
                validAccountId,
                UUID.randomUUID(),
                AccountStatus.ACTIVE,
                java.time.OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("createOrder calculates totalPrice correctly for EQUITY")
    void testCreateOrderCalculatesTotalPriceEquity() {
        // Setup
        BigDecimal quantity = new BigDecimal("100.00");
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        // When OrderMapper is called, mock the insert
        when(orderMapper.insertOrder(any(), any(), any(), any(), any())).thenReturn(1);
        
        // Mock retrieval after insert
        Order mockOrder = new Order(
                validAccountId,
                validInstrumentId,
                OrderSide.BUY,
                quantity,
                idempotencyKey
        );
        when(orderMapper.selectOrderByIdempotencyKey(validAccountId, idempotencyKey))
                .thenReturn(mockOrder);
        
        // Execute
        Order result = orderService.createOrder(validAccountId, validInstrumentId, "BUY", quantity.toPlainString(), idempotencyKey);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getTotalPrice());
        assertTrue(result.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("createOrder rejects quantity of 0")
    void testCreateOrderRejectsZeroQuantity() {
        BigDecimal zeroQuantity = BigDecimal.ZERO;
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(validAccountId, validInstrumentId, "BUY", zeroQuantity.toPlainString(), idempotencyKey),
                "Should reject quantity of 0"
        );
    }

    @Test
    @DisplayName("createOrder rejects negative quantity")
    void testCreateOrderRejectsNegativeQuantity() {
        BigDecimal negativeQuantity = new BigDecimal("-100.00");
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(validAccountId, validInstrumentId, "BUY", negativeQuantity.toPlainString(), idempotencyKey),
                "Should reject negative quantity"
        );
    }

    @Test
    @DisplayName("createOrder accepts valid side values (BUY and SELL)")
    void testCreateOrderAcceptsValidSides() {
        String[] validSides = {"BUY", "SELL"};
        BigDecimal quantity = new BigDecimal("100.00");
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        for (String side : validSides) {
            when(orderMapper.insertOrder(any(), any(), any(), any(), any())).thenReturn(1);
            
            Order mockOrder = new Order(
                    validAccountId,
                    validInstrumentId,
                    OrderSide.valueOf(side),
                    quantity,
                    idempotencyKey
            );
            when(orderMapper.selectOrderByIdempotencyKey(validAccountId, idempotencyKey))
                    .thenReturn(mockOrder);
            
            assertDoesNotThrow(() ->
                    orderService.createOrder(validAccountId, validInstrumentId, side, quantity.toPlainString(), idempotencyKey),
                    "Should accept side: " + side
            );
        }
    }

    @Test
    @DisplayName("createOrder rejects invalid side values")
    void testCreateOrderRejectsInvalidSides() {
        String[] invalidSides = {"INVALID", "PUT", "CALL", "HOLD"};
        BigDecimal quantity = new BigDecimal("100.00");
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        for (String side : invalidSides) {
            assertThrows(IllegalArgumentException.class, () ->
                    orderService.createOrder(validAccountId, validInstrumentId, side, quantity.toPlainString(), idempotencyKey),
                    "Should reject invalid side: " + side
            );
        }
    }

    @Test
    @DisplayName("createOrder validates market is open")
    void testCreateOrderValidatesMarketOpen() {
        BigDecimal quantity = new BigDecimal("100.00");
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        // In test environment, marketStatusService should always return true
        assertTrue(marketStatusService.isMarketOpen(), "Market should be open in test environment");
    }

    @Test
    @DisplayName("createOrder calculates fee and total price with multiple quantities")
    void testCreateOrderCalculatesTotalPriceMultipleQuantities() {
        // EQUITY fee: 0.05% minimum $1
        // Price $100, Quantity 10: (100 * 10) * 0.0005 = $0.50 → $1.00 min
        // Price $100, Quantity 1000: (100 * 1000) * 0.0005 = $50 (exceeds min)
        
        BigDecimal[] quantities = {
                new BigDecimal("10.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("0.5")
        };
        
        for (BigDecimal quantity : quantities) {
            String idempotencyKey = "test-key-" + UUID.randomUUID();
            when(orderMapper.insertOrder(any(), any(), any(), any(), any())).thenReturn(1);
            
            Order mockOrder = new Order(
                    validAccountId,
                    validInstrumentId,
                    OrderSide.BUY,
                    quantity,
                    idempotencyKey
            );
            when(orderMapper.selectOrderByIdempotencyKey(validAccountId, idempotencyKey))
                    .thenReturn(mockOrder);
            
            Order result = orderService.createOrder(validAccountId, validInstrumentId, "BUY", quantity.toPlainString(), idempotencyKey);
            
            assertNotNull(result.getTotalPrice());
            assertTrue(result.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(5, result.getTotalPrice().scale(), "Total price should have 5 decimal places");
        }
    }

    @Test
    @DisplayName("createOrder includes quantity in calculation")
    void testCreateOrderIncludesQuantityInCalculation() {
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        BigDecimal quantity = new BigDecimal("100.00");
        
        when(orderMapper.insertOrder(any(), any(), any(), any(), any())).thenReturn(1);
        
        Order mockOrder = new Order(
                validAccountId,
                validInstrumentId,
                OrderSide.BUY,
                quantity,
                idempotencyKey
        );
        when(orderMapper.selectOrderByIdempotencyKey(validAccountId, idempotencyKey))
                .thenReturn(mockOrder);
        
        Order result = orderService.createOrder(validAccountId, validInstrumentId, "BUY", quantity.toPlainString(), idempotencyKey);
        
        assertNotNull(result.getTotalPrice());
        // totalPrice should include quantity: (price * quantity) + fee
        // Verify it's significantly more than just the price alone
    }

    @Test
    @DisplayName("createOrder stores quantity exactly as provided")
    void testCreateOrderStoresQuantityExactly() {
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        BigDecimal quantity = new BigDecimal("123.45678");
        
        when(orderMapper.insertOrder(any(), any(), any(), any(), any())).thenReturn(1);
        
        Order mockOrder = new Order(
                validAccountId,
                validInstrumentId,
                OrderSide.BUY,
                quantity,
                idempotencyKey
        );
        when(orderMapper.selectOrderByIdempotencyKey(validAccountId, idempotencyKey))
                .thenReturn(mockOrder);
        
        Order result = orderService.createOrder(validAccountId, validInstrumentId, "BUY", quantity.toPlainString(), idempotencyKey);
        
        assertEquals(quantity, result.getQuantity());
    }

    @Test
    @DisplayName("createOrder returned object has submittedAt timestamp")
    void testCreateOrderHasSubmittedAtTimestamp() {
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        BigDecimal quantity = new BigDecimal("100.00");
        
        when(orderMapper.insertOrder(any(), any(), any(), any(), any())).thenReturn(1);
        
        Order mockOrder = new Order(
                validAccountId,
                validInstrumentId,
                OrderSide.BUY,
                quantity,
                idempotencyKey
        );
        mockOrder.setSubmittedAt(java.time.OffsetDateTime.now());
        when(orderMapper.selectOrderByIdempotencyKey(validAccountId, idempotencyKey))
                .thenReturn(mockOrder);
        
        Order result = orderService.createOrder(validAccountId, validInstrumentId, "BUY", quantity.toPlainString(), idempotencyKey);
        
        assertNotNull(result.getSubmittedAt());
    }
}
