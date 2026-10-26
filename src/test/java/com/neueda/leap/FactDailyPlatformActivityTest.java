package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FactDailyPlatformActivity Tests")
class FactDailyPlatformActivityTest {

    private FactDailyPlatformActivity activity;
    private LocalDate testActivityDate;
    private OffsetDateTime testDateTime;
    private Integer testOrderCount = 500;
    private BigDecimal testFilledQuantity;
    private BigDecimal testGrossAmount;

    @BeforeEach
    void setUp() {
        testActivityDate = LocalDate.now();
        testDateTime = OffsetDateTime.now();
        testFilledQuantity = new BigDecimal("10000.50");
        testGrossAmount = new BigDecimal("1500000.75");
    }

    @Test
    @DisplayName("Default constructor creates empty FactDailyPlatformActivity")
    void testDefaultConstructor() {
        activity = new FactDailyPlatformActivity();
        assertNull(activity.getActivityDate());
        assertNull(activity.getOrderCount());
        assertNull(activity.getFilledQuantity());
        assertNull(activity.getGrossAmount());
        assertNull(activity.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with activityDate, orderCount, filledQuantity, and grossAmount")
    void testConstructorWithBasicFields() {
        activity = new FactDailyPlatformActivity(testActivityDate, testOrderCount, testFilledQuantity, testGrossAmount);
        
        assertEquals(testActivityDate, activity.getActivityDate());
        assertEquals(testOrderCount, activity.getOrderCount());
        assertEquals(testFilledQuantity, activity.getFilledQuantity());
        assertEquals(testGrossAmount, activity.getGrossAmount());
        assertNull(activity.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including updatedAt")
    void testConstructorWithAllFields() {
        activity = new FactDailyPlatformActivity(testActivityDate, testOrderCount, testFilledQuantity, testGrossAmount, testDateTime);
        
        assertEquals(testActivityDate, activity.getActivityDate());
        assertEquals(testOrderCount, activity.getOrderCount());
        assertEquals(testFilledQuantity, activity.getFilledQuantity());
        assertEquals(testGrossAmount, activity.getGrossAmount());
        assertEquals(testDateTime, activity.getUpdatedAt());
    }

    @Test
    @DisplayName("setActivityDate and getActivityDate")
    void testSetAndGetActivityDate() {
        activity = new FactDailyPlatformActivity();
        activity.setActivityDate(testActivityDate);
        assertEquals(testActivityDate, activity.getActivityDate());
    }

    @Test
    @DisplayName("setOrderCount and getOrderCount")
    void testSetAndGetOrderCount() {
        activity = new FactDailyPlatformActivity();
        activity.setOrderCount(1000);
        assertEquals(1000, activity.getOrderCount());
    }

    @Test
    @DisplayName("setFilledQuantity and getFilledQuantity")
    void testSetAndGetFilledQuantity() {
        activity = new FactDailyPlatformActivity();
        activity.setFilledQuantity(new BigDecimal("20000.75"));
        assertEquals(new BigDecimal("20000.75"), activity.getFilledQuantity());
    }

    @Test
    @DisplayName("setGrossAmount and getGrossAmount")
    void testSetAndGetGrossAmount() {
        activity = new FactDailyPlatformActivity();
        activity.setGrossAmount(new BigDecimal("3000000.50"));
        assertEquals(new BigDecimal("3000000.50"), activity.getGrossAmount());
    }

    @Test
    @DisplayName("setUpdatedAt and getUpdatedAt")
    void testSetAndGetUpdatedAt() {
        activity = new FactDailyPlatformActivity();
        activity.setUpdatedAt(testDateTime);
        assertEquals(testDateTime, activity.getUpdatedAt());
    }
}
