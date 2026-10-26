package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.neueda.leap.enums.OrderSide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Tests")
class OrderTest {

    private Order order;
    private UUID testOrderId;
    private UUID testAccountId;
    private UUID testInstrumentId;
    private BigDecimal testQuantity;
    private OffsetDateTime testDateTime;
    private String testIdempotencyKey = "key-12345";

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testAccountId = UUID.randomUUID();
        testInstrumentId = UUID.randomUUID();
        testQuantity = new BigDecimal("100.50");
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty Order")
    void testDefaultConstructor() {
        order = new Order();
        assertNull(order.getOrderId());
        assertNull(order.getAccountId());
        assertNull(order.getInstrumentId());
        assertNull(order.getSide());
        assertNull(order.getQuantity());
        assertNull(order.getIdempotencyKey());
        assertNull(order.getSubmittedAt());
    }

    @Test
    @DisplayName("Constructor with accountId, instrumentId, side, quantity, and idempotencyKey")
    void testConstructorWithBasicFields() {
        order = new Order(testAccountId, testInstrumentId, OrderSide.BUY, testQuantity, testIdempotencyKey);
        
        assertNull(order.getOrderId());
        assertEquals(testAccountId, order.getAccountId());
        assertEquals(testInstrumentId, order.getInstrumentId());
        assertEquals(OrderSide.BUY, order.getSide());
        assertEquals(testQuantity, order.getQuantity());
        assertEquals(testIdempotencyKey, order.getIdempotencyKey());
        assertNull(order.getSubmittedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including ID and submittedAt")
    void testConstructorWithAllFields() {
        order = new Order(testOrderId, testAccountId, testInstrumentId, OrderSide.SELL, testQuantity, testIdempotencyKey, testDateTime);
        
        assertEquals(testOrderId, order.getOrderId());
        assertEquals(testAccountId, order.getAccountId());
        assertEquals(testInstrumentId, order.getInstrumentId());
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(testQuantity, order.getQuantity());
        assertEquals(testIdempotencyKey, order.getIdempotencyKey());
        assertEquals(testDateTime, order.getSubmittedAt());
    }

    @Test
    @DisplayName("setOrderId and getOrderId")
    void testSetAndGetOrderId() {
        order = new Order();
        order.setOrderId(testOrderId);
        assertEquals(testOrderId, order.getOrderId());
    }

    @Test
    @DisplayName("setAccountId and getAccountId")
    void testSetAndGetAccountId() {
        order = new Order();
        order.setAccountId(testAccountId);
        assertEquals(testAccountId, order.getAccountId());
    }

    @Test
    @DisplayName("setInstrumentId and getInstrumentId")
    void testSetAndGetInstrumentId() {
        order = new Order();
        order.setInstrumentId(testInstrumentId);
        assertEquals(testInstrumentId, order.getInstrumentId());
    }

    @Test
    @DisplayName("setSide and getSide")
    void testSetAndGetSide() {
        order = new Order();
        order.setSide(OrderSide.BUY);
        assertEquals(OrderSide.BUY, order.getSide());
    }

    @Test
    @DisplayName("setQuantity and getQuantity")
    void testSetAndGetQuantity() {
        order = new Order();
        order.setQuantity(new BigDecimal("250.75"));
        assertEquals(new BigDecimal("250.75"), order.getQuantity());
    }

    @Test
    @DisplayName("setIdempotencyKey and getIdempotencyKey")
    void testSetAndGetIdempotencyKey() {
        order = new Order();
        order.setIdempotencyKey("new-key-67890");
        assertEquals("new-key-67890", order.getIdempotencyKey());
    }

    @Test
    @DisplayName("setSubmittedAt and getSubmittedAt")
    void testSetAndGetSubmittedAt() {
        order = new Order();
        order.setSubmittedAt(testDateTime);
        assertEquals(testDateTime, order.getSubmittedAt());
    }
}
