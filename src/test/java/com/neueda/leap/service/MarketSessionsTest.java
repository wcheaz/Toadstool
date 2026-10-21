package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Per-market trading sessions")
class MarketSessionsTest {

    private final MarketStatusService service = new MarketStatusService(new MockEnvironment());

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    // Wednesday 21 October 2026
    private static ZonedDateTime wednesday(int hour, int minute, ZoneId zone) {
        return ZonedDateTime.of(2026, 10, 21, hour, minute, 0, 0, zone);
    }

    private static Instrument instrument(String symbol, AssetClass assetClass) {
        return new Instrument(symbol, symbol, assetClass, InstrumentStatus.TRADABLE);
    }

    @Test
    @DisplayName("Crypto trades around the clock, including weekends")
    void cryptoAlwaysOpen() {
        Instrument btc = instrument("BTC-USD", AssetClass.CRYPTO);
        assertTrue(service.isMarketOpenAt(btc, wednesday(3, 0, NEW_YORK)));
        assertTrue(service.isMarketOpenAt(btc, ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, NEW_YORK)));
    }

    @Test
    @DisplayName("US equities follow the 09:30-16:00 New York session")
    void usEquities() {
        Instrument aapl = instrument("AAPL", AssetClass.EQUITY);
        assertTrue(service.isMarketOpenAt(aapl, wednesday(10, 0, NEW_YORK)));
        assertFalse(service.isMarketOpenAt(aapl, wednesday(17, 0, NEW_YORK)));
    }

    @Test
    @DisplayName("Indian equities follow the 09:15-15:30 IST session")
    void indianEquities() {
        Instrument reliance = instrument("RELIANCE.NS", AssetClass.EQUITY);
        assertTrue(service.isMarketOpenAt(reliance, wednesday(10, 0, KOLKATA)));
        assertFalse(service.isMarketOpenAt(reliance, wednesday(9, 0, KOLKATA)));
        assertFalse(service.isMarketOpenAt(reliance, wednesday(15, 30, KOLKATA)));
        // Closed during the US session (11:00 New York = 20:30 IST)
        assertFalse(service.isMarketOpenAt(reliance, wednesday(11, 0, NEW_YORK)));
        assertTrue(service.isMarketOpenAt(instrument("TCS.BO", AssetClass.EQUITY), wednesday(14, 0, KOLKATA)));
    }

    @Test
    @DisplayName("UK equities follow the 08:00-16:30 London session")
    void ukEquities() {
        Instrument vod = instrument("VOD.L", AssetClass.EQUITY);
        assertTrue(service.isMarketOpenAt(vod, wednesday(8, 0, LONDON)));
        assertFalse(service.isMarketOpenAt(vod, wednesday(16, 30, LONDON)));
    }

    @Test
    @DisplayName("FX trades from Sunday 17:00 to Friday 17:00 New York time")
    void fxWeek() {
        Instrument eurusd = instrument("FX:EURUSD", AssetClass.FX);
        assertTrue(service.isMarketOpenAt(eurusd, wednesday(2, 0, NEW_YORK)));
        assertTrue(service.isMarketOpenAt(eurusd, ZonedDateTime.of(2026, 10, 23, 16, 59, 0, 0, NEW_YORK)));
        assertFalse(service.isMarketOpenAt(eurusd, ZonedDateTime.of(2026, 10, 23, 17, 0, 0, 0, NEW_YORK)));
        assertFalse(service.isMarketOpenAt(eurusd, ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, NEW_YORK)));
        assertFalse(service.isMarketOpenAt(eurusd, ZonedDateTime.of(2026, 10, 25, 16, 59, 0, 0, NEW_YORK)));
        assertTrue(service.isMarketOpenAt(eurusd, ZonedDateTime.of(2026, 10, 25, 17, 0, 0, 0, NEW_YORK)));
    }
}
