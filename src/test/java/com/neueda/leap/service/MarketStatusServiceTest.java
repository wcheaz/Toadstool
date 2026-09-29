package com.neueda.leap.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("MarketStatusService Tests")
class MarketStatusServiceTest {

    @Autowired
    private MarketStatusService marketStatusService;

    // ==================== TEST ENVIRONMENT TESTS ====================

    @Test
    @DisplayName("isMarketOpen returns true in test environment")
    void testMarketOpenInTestEnvironment() {
        boolean isOpen = marketStatusService.isMarketOpen();
        assertTrue(isOpen, "Market should be open in test environment for testing purposes");
    }

    @Test
    @DisplayName("service is properly autowired")
    void testServiceAutowiring() {
        assertTrue(marketStatusService != null, "MarketStatusService should be autowired");
    }

    // ==================== MARKET OPEN HOUR TESTS ====================

    @Test
    @DisplayName("isMarketOpenAt returns true at market open time (9:30 AM)")
    void testMarketOpenAtOpenTime() {
        // Wednesday, 9:30 AM EST
        ZonedDateTime marketOpenTime = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(9, 30, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(marketOpenTime),
                   "Market should be open at exactly 9:30 AM");
    }

    @Test
    @DisplayName("isMarketOpenAt returns true at 10:00 AM")
    void testMarketOpenAt10Am() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(10, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(time),
                   "Market should be open at 10:00 AM");
    }

    @Test
    @DisplayName("isMarketOpenAt returns true at noon")
    void testMarketOpenAtNoon() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(12, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(time),
                   "Market should be open at noon");
    }

    @Test
    @DisplayName("isMarketOpenAt returns true at 3:59 PM (before market close)")
    void testMarketOpenAt359Pm() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(15, 59, 59),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(time),
                   "Market should be open at 3:59:59 PM");
    }

    // ==================== MARKET CLOSE HOUR TESTS ====================

    @Test
    @DisplayName("isMarketOpenAt returns false at market close time (4:00 PM)")
    void testMarketClosedAtCloseTime() {
        // Market closes AT 4:00 PM (exclusive)
        ZonedDateTime marketCloseTime = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(16, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(marketCloseTime),
                    "Market should be closed at exactly 4:00 PM");
    }

    @Test
    @DisplayName("isMarketOpenAt returns false at 4:01 PM")
    void testMarketClosedAt401Pm() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(16, 1, 0),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(time),
                    "Market should be closed at 4:01 PM");
    }

    @Test
    @DisplayName("isMarketOpenAt returns false at 5:00 PM")
    void testMarketClosedAt5Pm() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(17, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(time),
                    "Market should be closed at 5:00 PM");
    }

    @Test
    @DisplayName("isMarketOpenAt returns false at midnight")
    void testMarketClosedAtMidnight() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(0, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(time),
                    "Market should be closed at midnight");
    }

    @Test
    @DisplayName("isMarketOpenAt returns false before market open (9:29 AM)")
    void testMarketClosedBefore930Am() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(9, 29, 59),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(time),
                    "Market should be closed before 9:30 AM");
    }

    // ==================== WEEKEND TESTS ====================

    @Test
    @DisplayName("isMarketOpenAt returns false on Saturday even during market hours")
    void testMarketClosedOnSaturday() {
        // Saturday at 11:00 AM
        ZonedDateTime saturday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 6),  // Saturday
            LocalTime.of(11, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(saturday),
                    "Market should be closed on Saturday even during normal hours");
    }

    @Test
    @DisplayName("isMarketOpenAt returns false on Sunday even during market hours")
    void testMarketClosedOnSunday() {
        // Sunday at 11:00 AM
        ZonedDateTime sunday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 7),  // Sunday
            LocalTime.of(11, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(sunday),
                    "Market should be closed on Sunday even during normal hours");
    }

    @Test
    @DisplayName("isMarketOpenAt returns false on Saturday at midnight")
    void testMarketClosedSaturdayMidnight() {
        ZonedDateTime saturday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 6),  // Saturday
            LocalTime.of(0, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertFalse(marketStatusService.isMarketOpenAt(saturday),
                    "Market should be closed on Saturday at midnight");
    }

    // ==================== WEEKDAY TESTS ====================

    @Test
    @DisplayName("isMarketOpenAt returns true on Monday during market hours")
    void testMarketOpenOnMonday() {
        // Monday at 10:00 AM
        ZonedDateTime monday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 1),  // Monday
            LocalTime.of(10, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(monday),
                   "Market should be open on Monday during market hours");
    }

    @Test
    @DisplayName("isMarketOpenAt returns true on Tuesday during market hours")
    void testMarketOpenOnTuesday() {
        ZonedDateTime tuesday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 2),  // Tuesday
            LocalTime.of(14, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(tuesday),
                   "Market should be open on Tuesday during market hours");
    }

    @Test
    @DisplayName("isMarketOpenAt returns true on Wednesday during market hours")
    void testMarketOpenOnWednesday() {
        ZonedDateTime wednesday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(11, 30, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(wednesday),
                   "Market should be open on Wednesday during market hours");
    }

    @Test
    @DisplayName("isMarketOpenAt returns true on Thursday during market hours")
    void testMarketOpenOnThursday() {
        ZonedDateTime thursday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 4),  // Thursday
            LocalTime.of(13, 0, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(thursday),
                   "Market should be open on Thursday during market hours");
    }

    @Test
    @DisplayName("isMarketOpenAt returns true on Friday during market hours")
    void testMarketOpenOnFriday() {
        ZonedDateTime friday = ZonedDateTime.of(
            LocalDate.of(2024, 1, 5),  // Friday
            LocalTime.of(15, 30, 0),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(friday),
                   "Market should be open on Friday during market hours");
    }

    // ==================== BOUNDARY TESTS ====================

    @Test
    @DisplayName("isMarketOpenAt returns true one microsecond after market open")
    void testMarketOpenOneSecondAfterOpen() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(9, 30, 1),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(time),
                   "Market should be open one second after 9:30 AM");
    }

    @Test
    @DisplayName("isMarketOpenAt returns false one second before market close")
    void testMarketClosedOneSecondBeforeClose() {
        ZonedDateTime time = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(15, 59, 59),
            ZoneId.of("America/New_York")
        );
        
        assertTrue(marketStatusService.isMarketOpenAt(time),
                   "Market should be open one second before 4:00 PM");
    }

    // ==================== TIMEZONE CONSISTENCY TESTS ====================

    @Test
    @DisplayName("Market open/close times are consistent")
    void testMarketTimesConsistency() {
        LocalTime openTime = marketStatusService.getMarketOpenTime();
        LocalTime closeTime = marketStatusService.getMarketCloseTime();
        ZoneId timezone = marketStatusService.getMarketTimezone();
        
        assertTrue(openTime.isBefore(closeTime), "Market open time should be before close time");
        assertTrue(timezone.toString().contains("New_York"), "Timezone should be America/New_York");
    }

    @Test
    @DisplayName("isMarketOpenAt respects America/New_York timezone")
    void testMarketTimezoneRespected() {
        // 14:00 UTC = 9:00 AM EST (before market open)
        ZonedDateTime utcTime = ZonedDateTime.of(
            LocalDate.of(2024, 1, 3),  // Wednesday
            LocalTime.of(14, 0, 0),
            ZoneId.of("UTC")
        );
        
        ZonedDateTime estTime = utcTime.withZoneSameInstant(ZoneId.of("America/New_York"));
        
        // 9:00 AM EST should be before market open (9:30 AM EST)
        assertFalse(marketStatusService.isMarketOpenAt(estTime),
                    "9:00 AM EST should be before market open");
    }
}

