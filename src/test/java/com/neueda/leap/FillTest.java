package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.neueda.leap.enums.FillStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fill Tests")
class FillTest {

    private Fill fill;
    private UUID testFillId;
    private UUID testOrderId;
    private BigDecimal testPrice;
    private BigDecimal testQuantity;
    private OffsetDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testFillId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testPrice = new BigDecimal("150.25");
        testQuantity = new BigDecimal("50.00");
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty Fill")
    void testDefaultConstructor() {
        fill = new Fill();
        assertNull(fill.getFillId());
        assertNull(fill.getOrderId());
        assertNull(fill.getPrice());
        assertNull(fill.getQuantity());
        assertNull(fill.getStatus());
        assertNull(fill.getExecutedAt());
    }

    @Test
    @DisplayName("Constructor with orderId, price, quantity, and status")
    void testConstructorWithBasicFields() {
        fill = new Fill(testOrderId, testPrice, testQuantity, FillStatus.Filled);
        
        assertNull(fill.getFillId());
        assertEquals(testOrderId, fill.getOrderId());
        assertEquals(testPrice, fill.getPrice());
        assertEquals(testQuantity, fill.getQuantity());
        assertEquals(FillStatus.Filled, fill.getStatus());
        assertNull(fill.getExecutedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including ID and executedAt")
    void testConstructorWithAllFields() {
        fill = new Fill(testFillId, testOrderId, testPrice, testQuantity, FillStatus.Filled, testDateTime);
        
        assertEquals(testFillId, fill.getFillId());
        assertEquals(testOrderId, fill.getOrderId());
        assertEquals(testPrice, fill.getPrice());
        assertEquals(testQuantity, fill.getQuantity());
        assertEquals(FillStatus.Filled, fill.getStatus());
        assertEquals(testDateTime, fill.getExecutedAt());
    }

    @Test
    @DisplayName("setFillId and getFillId")
    void testSetAndGetFillId() {
        fill = new Fill();
        fill.setFillId(testFillId);
        assertEquals(testFillId, fill.getFillId());
    }

    @Test
    @DisplayName("setOrderId and getOrderId")
    void testSetAndGetOrderId() {
        fill = new Fill();
        fill.setOrderId(testOrderId);
        assertEquals(testOrderId, fill.getOrderId());
    }

    @Test
    @DisplayName("setPrice and getPrice")
    void testSetAndGetPrice() {
        fill = new Fill();
        fill.setPrice(new BigDecimal("200.50"));
        assertEquals(new BigDecimal("200.50"), fill.getPrice());
    }

    @Test
    @DisplayName("setQuantity and getQuantity")
    void testSetAndGetQuantity() {
        fill = new Fill();
        fill.setQuantity(new BigDecimal("75.25"));
        assertEquals(new BigDecimal("75.25"), fill.getQuantity());
    }

    @Test
    @DisplayName("setStatus and getStatus")
    void testSetAndGetStatus() {
        fill = new Fill();
        fill.setStatus(FillStatus.Failed);
        assertEquals(FillStatus.Failed, fill.getStatus());
    }

    @Test
    @DisplayName("setExecutedAt and getExecutedAt")
    void testSetAndGetExecutedAt() {
        fill = new Fill();
        fill.setExecutedAt(testDateTime);
        assertEquals(testDateTime, fill.getExecutedAt());
    }
}
