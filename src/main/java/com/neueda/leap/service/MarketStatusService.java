package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.enums.AssetClass;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Service for checking if markets are currently open
 * 
 * In testing environments: Always returns true to allow testing past market hours
 * In production environments: Respects actual US market hours (9:30 AM - 4:00 PM EST)
 */
@Service
public class MarketStatusService {

    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);    // 9:30 AM EST
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);   // 4:00 PM EST
    private static final ZoneId MARKET_TIMEZONE = ZoneId.of("America/New_York");
    private static final ZoneId INDIA_TIMEZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime INDIA_OPEN = LocalTime.of(9, 15);
    private static final LocalTime INDIA_CLOSE = LocalTime.of(15, 30);
    private static final ZoneId LONDON_TIMEZONE = ZoneId.of("Europe/London");
    private static final LocalTime LONDON_OPEN = LocalTime.of(8, 0);
    private static final LocalTime LONDON_CLOSE = LocalTime.of(16, 30);
    private static final LocalTime FX_WEEK_BOUNDARY = LocalTime.of(17, 0);

    private final Environment environment;

    public MarketStatusService(Environment environment) {
        this.environment = environment;
    }

    /**
     * Checks if the market is currently open
     * 
     * Test Environment: Always returns true to allow unrestricted testing
     * Production Environment: Checks if current time is within market hours (9:30 AM - 4:00 PM EST)
     * and it's a weekday (Monday - Friday)
     * 
     * @return true if the market is open (or in test environment), false otherwise
     */
    public boolean isMarketOpen() {
        // In test environment, always return true
        if (isTestEnvironment()) {
            return true;
        }

        return isWithinMarketHours();
    }

    /**
     * Checks if the market an instrument trades on is currently open (always true in test environments).
     * Each market keeps its own session: crypto trades continuously, FX from Sunday 5pm to Friday 5pm
     * New York time, Indian equities 09:15-15:30 IST, UK equities 08:00-16:30 London time, and other
     * equities the US session.
     */
    public boolean isMarketOpen(Instrument instrument) {
        if (isTestEnvironment()) {
            return true;
        }
        return isMarketOpenAt(instrument, ZonedDateTime.now());
    }

    /** Market status for an instrument at a given time (for testing). */
    protected boolean isMarketOpenAt(Instrument instrument, ZonedDateTime time) {
        AssetClass assetClass = instrument != null ? instrument.getAssetClass() : null;
        if (assetClass == AssetClass.CRYPTO) {
            return true;
        }
        if (assetClass == AssetClass.FX) {
            return isFxMarketOpenAt(time.withZoneSameInstant(MARKET_TIMEZONE));
        }
        String symbol = instrument != null && instrument.getSymbol() != null ? instrument.getSymbol().toUpperCase() : "";
        if (symbol.endsWith(".NS") || symbol.endsWith(".BO")) {
            return isWeekdaySessionOpen(time.withZoneSameInstant(INDIA_TIMEZONE), INDIA_OPEN, INDIA_CLOSE);
        }
        if (symbol.endsWith(".L")) {
            return isWeekdaySessionOpen(time.withZoneSameInstant(LONDON_TIMEZONE), LONDON_OPEN, LONDON_CLOSE);
        }
        return isMarketOpenAt(time.withZoneSameInstant(MARKET_TIMEZONE));
    }

    private static boolean isWeekdaySessionOpen(ZonedDateTime local, LocalTime open, LocalTime close) {
        DayOfWeek day = local.getDayOfWeek();
        boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        LocalTime time = local.toLocalTime();
        return isWeekday && !time.isBefore(open) && time.isBefore(close);
    }

    /** FX trades around the clock from Sunday 17:00 to Friday 17:00 New York time. */
    private static boolean isFxMarketOpenAt(ZonedDateTime newYork) {
        DayOfWeek day = newYork.getDayOfWeek();
        LocalTime time = newYork.toLocalTime();
        if (day == DayOfWeek.SATURDAY) {
            return false;
        }
        if (day == DayOfWeek.SUNDAY) {
            return !time.isBefore(FX_WEEK_BOUNDARY);
        }
        if (day == DayOfWeek.FRIDAY) {
            return time.isBefore(FX_WEEK_BOUNDARY);
        }
        return true;
    }

    /**
     * Checks if the current time is within market operating hours
     * 
     * @return true if current time is between 9:30 AM and 4:00 PM EST on a weekday
     */
    private boolean isWithinMarketHours() {
        ZonedDateTime nowEst = ZonedDateTime.now(MARKET_TIMEZONE);
        LocalTime currentTime = nowEst.toLocalTime();
        
        // Check if it's a weekday (Monday = 1 to Friday = 5)
        int dayOfWeek = nowEst.getDayOfWeek().getValue();
        boolean isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;
        
        // Check if current time is within market hours
        boolean isWithinHours = !currentTime.isBefore(MARKET_OPEN) && currentTime.isBefore(MARKET_CLOSE);
        
        return isWeekday && isWithinHours;
    }

    /**
     * Determines if the application is running in a test environment
     * 
     * Checks Spring active profiles to detect test context
     * 
     * @return true if in test environment, false otherwise
     */
    private boolean isTestEnvironment() {
        // Check Spring active profiles
        String[] activeProfiles = environment.getActiveProfiles();
        
        // Check for test, dev, or development profiles
        for (String profile : activeProfiles) {
            String lowerProfile = profile.toLowerCase();
            if (lowerProfile.contains("test") || lowerProfile.contains("dev") || lowerProfile.contains("local")) {
                return true;
            }
        }

        // If no active profile is set, assume development/testing environment
        // This allows local testing to proceed outside market hours
        if (activeProfiles.length == 0) {
            return true;
        }

        return false;
    }

    /**
     * Gets the market open time (9:30 AM EST)
     * 
     * @return The market opening time
     */
    public LocalTime getMarketOpenTime() {
        return MARKET_OPEN;
    }

    /**
     * Gets the market close time (4:00 PM EST)
     * 
     * @return The market closing time
     */
    public LocalTime getMarketCloseTime() {
        return MARKET_CLOSE;
    }

    /**
     * Gets the market timezone (America/New_York)
     * 
     * @return The market timezone
     */
    public ZoneId getMarketTimezone() {
        return MARKET_TIMEZONE;
    }

    /**
     * Checks if the market is open at a specific time (for testing)
     * 
     * This method allows tests to inject specific timestamps to verify market hour logic
     * bypassing the normal test environment detection.
     * 
     * Production code should use isMarketOpen(), not this method.
     * 
     * @param testTime The time to check market status for
     * @return true if market would be open at the given time
     */
    protected boolean isMarketOpenAt(ZonedDateTime testTime) {
        LocalTime currentTime = testTime.toLocalTime();
        
        // Check if it's a weekday (Monday = 1 to Friday = 5)
        int dayOfWeek = testTime.getDayOfWeek().getValue();
        boolean isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;
        
        // Check if current time is within market hours
        boolean isWithinHours = !currentTime.isBefore(MARKET_OPEN) && currentTime.isBefore(MARKET_CLOSE);
        
        return isWeekday && isWithinHours;
    }
}
