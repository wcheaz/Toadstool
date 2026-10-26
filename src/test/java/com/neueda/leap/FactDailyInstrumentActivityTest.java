package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FactDailyInstrumentActivity Tests")
class FactDailyInstrumentActivityTest {

    private FactDailyInstrumentActivity activity;
    private UUID testInstrumentId;
    private LocalDate testActivityDate;
    private OffsetDateTime testDateTime;
    private Integer testOrderCount = 50;
    private BigDecimal testFilledQuantity;
    private BigDecimal testGrossAmount;

    @BeforeEach
    void setUp() {
        testInstrumentId = UUID.randomUUID();
        testActivityDate = LocalDate.now();
        testDateTime = OffsetDateTime.now();
        testFilledQuantity = new BigDecimal("1000.50");
        testGrossAmount = new BigDecimal("150000.75");
    }

    @Test
    @DisplayName("Default constructor creates empty FactDailyInstrumentActivity")
    void testDefaultConstructor() {
        activity = new FactDailyInstrumentActivity();
        assertNull(activity.getActivityDate());
        assertNull(activity.getInstrumentId());
        assertNull(activity.getOrderCount());
        assertNull(activity.getFilledQuantity());
        assertNull(activity.getGrossAmount());
        assertNull(activity.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with activityDate, instrumentId, orderCount, filledQuantity, and grossAmount")
    void testConstructorWithBasicFields() {
        activity = new FactDailyInstrumentActivity(testActivityDate, testInstrumentId, testOrderCount, testFilledQuantity, testGrossAmount);
        
        assertEquals(testActivityDate, activity.getActivityDate());
        assertEquals(testInstrumentId, activity.getInstrumentId());
        assertEquals(testOrderCount, activity.getOrderCount());
        assertEquals(testFilledQuantity, activity.getFilledQuantity());
        assertEquals(testGrossAmount, activity.getGrossAmount());
        assertNull(activity.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including updatedAt")
    void testConstructorWithAllFields() {
        activity = new FactDailyInstrumentActivity(testActivityDate, testInstrumentId, testOrderCount, testFilledQuantity, testGrossAmount, testDateTime);
        
        assertEquals(testActivityDate, activity.getActivityDate());
        assertEquals(testInstrumentId, activity.getInstrumentId());
        assertEquals(testOrderCount, activity.getOrderCount());
        assertEquals(testFilledQuantity, activity.getFilledQuantity());
        assertEquals(testGrossAmount, activity.getGrossAmount());
        assertEquals(testDateTime, activity.getUpdatedAt());
    }

    @Test
    @DisplayName("setActivityDate and getActivityDate")
    void testSetAndGetActivityDate() {
        activity = new FactDailyInstrumentActivity();
        activity.setActivityDate(testActivityDate);
        assertEquals(testActivityDate, activity.getActivityDate());
    }

    @Test
    @DisplayName("setInstrumentId and getInstrumentId")
    void testSetAndGetInstrumentId() {
        activity = new FactDailyInstrumentActivity();
        activity.setInstrumentId(testInstrumentId);
        assertEquals(testInstrumentId, activity.getInstrumentId());
    }

    @Test
    @DisplayName("setOrderCount and getOrderCount")
    void testSetAndGetOrderCount() {
        activity = new FactDailyInstrumentActivity();
        activity.setOrderCount(100);
        assertEquals(100, activity.getOrderCount());
    }

    @Test
    @DisplayName("setFilledQuantity and getFilledQuantity")
    void testSetAndGetFilledQuantity() {
        activity = new FactDailyInstrumentActivity();
        activity.setFilledQuantity(new BigDecimal("2000.75"));
        assertEquals(new BigDecimal("2000.75"), activity.getFilledQuantity());
    }

    @Test
    @DisplayName("setGrossAmount and getGrossAmount")
    void testSetAndGetGrossAmount() {
        activity = new FactDailyInstrumentActivity();
        activity.setGrossAmount(new BigDecimal("300000.50"));
        assertEquals(new BigDecimal("300000.50"), activity.getGrossAmount());
    }

    @Test
    @DisplayName("setUpdatedAt and getUpdatedAt")
    void testSetAndGetUpdatedAt() {
        activity = new FactDailyInstrumentActivity();
        activity.setUpdatedAt(testDateTime);
        assertEquals(testDateTime, activity.getUpdatedAt());
    }
}
