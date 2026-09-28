package com.neueda.leap;

import com.neueda.leap.enums.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Entity - totalPrice Field Tests")
class OrderTotalPriceTest {

    private Order order;
    private UUID testOrderId;
    private UUID testAccountId;
    private UUID testInstrumentId;
    private BigDecimal testQuantity;
    private BigDecimal testTotalPrice;
    private OffsetDateTime testDateTime;
    private String testIdempotencyKey;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testAccountId = UUID.randomUUID();
        testInstrumentId = UUID.randomUUID();
        testQuantity = new BigDecimal("100.50");
        testTotalPrice = new BigDecimal("10050.50000");
        testDateTime = OffsetDateTime.now();
        testIdempotencyKey = "key-12345";
    }

    @Test
    @DisplayName("getTotalPrice returns the total price")
    void testGetTotalPrice() {
        order = new Order();
        order.setTotalPrice(testTotalPrice);
        assertEquals(testTotalPrice, order.getTotalPrice());
    }

    @Test
    @DisplayName("setTotalPrice sets the total price")
    void testSetTotalPrice() {
        order = new Order();
        order.setTotalPrice(testTotalPrice);
        assertNotNull(order.getTotalPrice());
        assertEquals(testTotalPrice, order.getTotalPrice());
    }

    @Test
    @DisplayName("totalPrice defaults to null when not set")
    void testTotalPriceDefaultsToNull() {
        order = new Order();
        assertNull(order.getTotalPrice());
    }

    @Test
    @DisplayName("Constructor with basic fields does not set totalPrice")
    void testConstructorBasicFieldsNoTotalPrice() {
        order = new Order(testAccountId, testInstrumentId, OrderSide.BUY, testQuantity, testIdempotencyKey);
        assertNull(order.getTotalPrice());
    }

    @Test
    @DisplayName("Constructor with all fields does not set totalPrice (only provided in DB retrieval)")
    void testConstructorAllFieldsNoTotalPrice() {
        order = new Order(testOrderId, testAccountId, testInstrumentId, OrderSide.BUY, testQuantity, testIdempotencyKey, testDateTime);
        assertNull(order.getTotalPrice());
    }

    @Test
    @DisplayName("totalPrice can be set independently after construction")
    void testTotalPriceSetAfterConstruction() {
        order = new Order(testAccountId, testInstrumentId, OrderSide.BUY, testQuantity, testIdempotencyKey);
        assertNull(order.getTotalPrice());
        
        order.setTotalPrice(testTotalPrice);
        assertEquals(testTotalPrice, order.getTotalPrice());
    }

    @Test
    @DisplayName("totalPrice maintains 5 decimal place precision")
    void testTotalPricePrecision() {
        order = new Order();
        BigDecimal precisePrice = new BigDecimal("12345.12345");
        order.setTotalPrice(precisePrice);
        
        assertEquals(5, order.getTotalPrice().scale());
        assertEquals(precisePrice, order.getTotalPrice());
    }

    @Test
    @DisplayName("totalPrice can represent various monetary amounts")
    void testTotalPriceVariousAmounts() {
        order = new Order();
        
        BigDecimal[] amounts = {
                new BigDecimal("0.50000"),      // Small fee
                new BigDecimal("100.00000"),    // Round amount
                new BigDecimal("1234.56789"),   // Precise amount
                new BigDecimal("999999.99999")  // Large amount
        };
        
        for (BigDecimal amount : amounts) {
            order.setTotalPrice(amount);
            assertEquals(amount, order.getTotalPrice());
        }
    }

    @Test
    @DisplayName("totalPrice is not included in equality check (side effect: object reference equality)")
    void testTotalPriceNotInConstructorWithAllFields() {
        Order order1 = new Order(testOrderId, testAccountId, testInstrumentId, OrderSide.BUY, testQuantity, testIdempotencyKey, testDateTime);
        order1.setTotalPrice(new BigDecimal("100.00000"));
        
        Order order2 = new Order(testOrderId, testAccountId, testInstrumentId, OrderSide.BUY, testQuantity, testIdempotencyKey, testDateTime);
        order2.setTotalPrice(new BigDecimal("200.00000"));
        
        // Both orders have same constructor parameters
        assertEquals(order1.getOrderId(), order2.getOrderId());
        // But different totalPrice (this is OK - totalPrice is calculated field, not stored in DB)
        assertNotEquals(order1.getTotalPrice(), order2.getTotalPrice());
    }
}
