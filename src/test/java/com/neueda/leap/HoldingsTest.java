package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Holdings Tests")
class HoldingsTest {

    private Holdings holdings;
    private UUID testHoldingId;
    private UUID testAccountId;
    private UUID testInstrumentId;
    private BigDecimal testQuantity;
    private OffsetDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testHoldingId = UUID.randomUUID();
        testAccountId = UUID.randomUUID();
        testInstrumentId = UUID.randomUUID();
        testQuantity = new BigDecimal("1000.50");
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty Holdings")
    void testDefaultConstructor() {
        holdings = new Holdings();
        assertNull(holdings.getHoldingId());
        assertNull(holdings.getAccountId());
        assertNull(holdings.getInstrumentId());
        assertNull(holdings.getQuantity());
        assertNull(holdings.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with accountId, instrumentId, and quantity")
    void testConstructorWithBasicFields() {
        holdings = new Holdings(testAccountId, testInstrumentId, testQuantity);

        assertNull(holdings.getHoldingId());
        assertEquals(testAccountId, holdings.getAccountId());
        assertEquals(testInstrumentId, holdings.getInstrumentId());
        assertEquals(testQuantity, holdings.getQuantity());
        assertNull(holdings.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with accountId, instrumentId, quantity, and updatedAt")
    void testConstructorWithUpdatedAt() {
        holdings = new Holdings(testAccountId, testInstrumentId, testQuantity, testDateTime);

        assertNull(holdings.getHoldingId());
        assertEquals(testAccountId, holdings.getAccountId());
        assertEquals(testInstrumentId, holdings.getInstrumentId());
        assertEquals(testQuantity, holdings.getQuantity());
        assertEquals(testDateTime, holdings.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including holdingId")
    void testConstructorWithAllFields() {
        holdings = new Holdings(testHoldingId, testAccountId, testInstrumentId, testQuantity, testDateTime);

        assertEquals(testHoldingId, holdings.getHoldingId());
        assertEquals(testAccountId, holdings.getAccountId());
        assertEquals(testInstrumentId, holdings.getInstrumentId());
        assertEquals(testQuantity, holdings.getQuantity());
        assertEquals(testDateTime, holdings.getUpdatedAt());
    }

    @Test
    @DisplayName("setHoldingId and getHoldingId")
    void testSetAndGetHoldingId() {
        holdings = new Holdings();
        holdings.setHoldingId(testHoldingId);
        assertEquals(testHoldingId, holdings.getHoldingId());
    }

    @Test
    @DisplayName("setAccountId and getAccountId")
    void testSetAndGetAccountId() {
        holdings = new Holdings();
        holdings.setAccountId(testAccountId);
        assertEquals(testAccountId, holdings.getAccountId());
    }

    @Test
    @DisplayName("setInstrumentId and getInstrumentId")
    void testSetAndGetInstrumentId() {
        holdings = new Holdings();
        holdings.setInstrumentId(testInstrumentId);
        assertEquals(testInstrumentId, holdings.getInstrumentId());
    }

    @Test
    @DisplayName("setQuantity and getQuantity")
    void testSetAndGetQuantity() {
        holdings = new Holdings();
        BigDecimal newQuantity = new BigDecimal("500.25");
        holdings.setQuantity(newQuantity);
        assertEquals(newQuantity, holdings.getQuantity());
    }

    @Test
    @DisplayName("Quantity can be zero (liquidated position)")
    void testQuantityCanBeZero() {
        holdings = new Holdings(testAccountId, testInstrumentId, BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, holdings.getQuantity());
    }

    @Test
    @DisplayName("Quantity can be set to zero (liquidation)")
    void testSetQuantityToZero() {
        holdings = new Holdings(testAccountId, testInstrumentId, testQuantity);
        holdings.setQuantity(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, holdings.getQuantity());
    }

    @Test
    @DisplayName("setUpdatedAt and getUpdatedAt")
    void testSetAndGetUpdatedAt() {
        holdings = new Holdings();
        holdings.setUpdatedAt(testDateTime);
        assertEquals(testDateTime, holdings.getUpdatedAt());
    }

    @Test
    @DisplayName("UpdatedAt can be updated multiple times (tracking mutations)")
    void testUpdatedAtCanBeUpdatedMultipleTimes() {
        holdings = new Holdings(testAccountId, testInstrumentId, testQuantity, testDateTime);
        assertEquals(testDateTime, holdings.getUpdatedAt());

        OffsetDateTime laterTime = testDateTime.plusMinutes(5);
        holdings.setUpdatedAt(laterTime);
        assertEquals(laterTime, holdings.getUpdatedAt());
    }

    @Test
    @DisplayName("High precision quantity values")
    void testHighPrecisionQuantity() {
        BigDecimal highPrecisionQuantity = new BigDecimal("123456789.1234567890");
        holdings = new Holdings(testAccountId, testInstrumentId, highPrecisionQuantity);
        assertEquals(highPrecisionQuantity, holdings.getQuantity());
    }

    @Test
    @DisplayName("Different account-instrument combinations")
    void testMultipleHoldings() {
        UUID account1 = UUID.randomUUID();
        UUID account2 = UUID.randomUUID();
        UUID instrument1 = UUID.randomUUID();
        UUID instrument2 = UUID.randomUUID();

        Holdings holding1 = new Holdings(account1, instrument1, new BigDecimal("100"));
        Holdings holding2 = new Holdings(account1, instrument2, new BigDecimal("200"));
        Holdings holding3 = new Holdings(account2, instrument1, new BigDecimal("300"));

        assertEquals(account1, holding1.getAccountId());
        assertEquals(instrument1, holding1.getInstrumentId());
        assertEquals(new BigDecimal("100"), holding1.getQuantity());

        assertEquals(account1, holding2.getAccountId());
        assertEquals(instrument2, holding2.getInstrumentId());
        assertEquals(new BigDecimal("200"), holding2.getQuantity());

        assertEquals(account2, holding3.getAccountId());
        assertEquals(instrument1, holding3.getInstrumentId());
        assertEquals(new BigDecimal("300"), holding3.getQuantity());
    }
}
