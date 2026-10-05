package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.enums.AssetClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("InstrumentPricingService Tests")
class InstrumentPricingServiceTest {

    @Autowired
    private InstrumentPricingService pricingService;

    @MockBean
    private InstrumentService instrumentService;

    @MockBean
    private FauxnanceService fauxnanceService;

    @BeforeEach
    void setUp() {
        // Default mock setup - any instrument returns a mock with symbol
        Instrument mockInstrument = new Instrument();
        mockInstrument.setInstrumentId(UUID.randomUUID());
        mockInstrument.setSymbol("TEST");
        mockInstrument.setAssetClass(AssetClass.EQUITY);
        
        when(instrumentService.getInstrumentById(any(UUID.class))).thenReturn(mockInstrument);
        when(fauxnanceService.getQuote("TEST")).thenReturn(new FauxnanceService.QuoteResponse("TEST", 25.50, 25.00, 26.00, 0.50, 2.0, "2026-10-05"));
    }

    @Test
    @DisplayName("getPrice returns a price for EQUITY instruments")
    void testGetPriceForEquity() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("AAPL");
        instrument.setAssetClass(AssetClass.EQUITY);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("AAPL")).thenReturn(new FauxnanceService.QuoteResponse("AAPL", 150.75, 150.00, 151.50, 0.75, 0.5, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        
        assertNotNull(price);
        assertEquals(new BigDecimal("150.75000"), price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should be positive");
    }

    @Test
    @DisplayName("getPrice returns a price for CRYPTO instruments")
    void testGetPriceForCrypto() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("BTC");
        instrument.setAssetClass(AssetClass.CRYPTO);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("BTC")).thenReturn(new FauxnanceService.QuoteResponse("BTC", 45000.50, 44900.00, 45100.00, 500.50, 1.1, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.CRYPTO, instrumentId);
        
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should be positive");
    }

    @Test
    @DisplayName("getPrice returns a price for FX instruments")
    void testGetPriceForFx() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("EUR/USD");
        instrument.setAssetClass(AssetClass.FX);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("EUR/USD")).thenReturn(new FauxnanceService.QuoteResponse("EUR/USD", 1.08, 1.079, 1.081, 0.01, 0.9, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.FX, instrumentId);
        
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should be positive");
    }

    @Test
    @DisplayName("getPrice returns prices with at most 5 decimal places")
    void testGetPricePrecision() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("TEST");
        instrument.setAssetClass(AssetClass.EQUITY);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("TEST")).thenReturn(new FauxnanceService.QuoteResponse("TEST", 25.123456, 25.00, 26.00, 0.1, 0.5, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        
        assertTrue(price.scale() <= 5, "Price should have at most 5 decimal places");
    }

    @Test
    @DisplayName("getPrice returns different values for different instruments")
    void testGetPriceRandomness() {
        UUID instrumentId1 = UUID.randomUUID();
        UUID instrumentId2 = UUID.randomUUID();
        
        Instrument instrument1 = new Instrument();
        instrument1.setInstrumentId(instrumentId1);
        instrument1.setSymbol("AAPL");
        instrument1.setAssetClass(AssetClass.EQUITY);
        
        Instrument instrument2 = new Instrument();
        instrument2.setInstrumentId(instrumentId2);
        instrument2.setSymbol("MSFT");
        instrument2.setAssetClass(AssetClass.EQUITY);
        
        when(instrumentService.getInstrumentById(instrumentId1)).thenReturn(instrument1);
        when(instrumentService.getInstrumentById(instrumentId2)).thenReturn(instrument2);
        when(fauxnanceService.getQuote("AAPL")).thenReturn(new FauxnanceService.QuoteResponse("AAPL", 150.00, 149.00, 151.00, 0.5, 0.3, "2026-10-05"));
        when(fauxnanceService.getQuote("MSFT")).thenReturn(new FauxnanceService.QuoteResponse("MSFT", 300.00, 299.00, 301.00, 1.0, 0.3, "2026-10-05"));
        
        BigDecimal price1 = pricingService.getPrice(AssetClass.EQUITY, instrumentId1);
        BigDecimal price2 = pricingService.getPrice(AssetClass.EQUITY, instrumentId2);
        
        assertNotNull(price1);
        assertNotNull(price2);
        assertNotEquals(price1, price2, "Different instruments should return different prices");
    }

    @Test
    @DisplayName("getPrice returns consistent decimal scale")
    void testGetPriceDecimalScale() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("TEST");
        instrument.setAssetClass(AssetClass.EQUITY);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("TEST")).thenReturn(new FauxnanceService.QuoteResponse("TEST", 100.00, 99.00, 101.00, 0.5, 0.5, "2026-10-05"));
        
        for (int i = 0; i < 5; i++) {
            BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
            assertTrue(price.scale() <= 5, "All prices should maintain consistent scale");
        }
    }

    // ==================== EDGE CASE & ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("getPrice throws IllegalArgumentException for null asset class")
    void testGetPriceNullAssetClass() {
        UUID instrumentId = UUID.randomUUID();
        
        assertThrows(IllegalArgumentException.class,
                () -> pricingService.getPrice(null, instrumentId),
                "Should throw for null asset class");
    }

    @Test
    @DisplayName("getPrice returns fallback price when instrument not found")
    void testGetPriceNullInstrumentId() {
        UUID instrumentId = UUID.randomUUID();
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(null);
        
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        
        assertNotNull(price);
        assertEquals(new BigDecimal("100.00000"), price, "Should return fallback price when instrument not found");
    }

    @Test
    @DisplayName("getPrice returns positive prices")
    void testGetPricePositive() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("TEST");
        instrument.setAssetClass(AssetClass.EQUITY);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("TEST")).thenReturn(new FauxnanceService.QuoteResponse("TEST", 50.00, 49.00, 51.00, 0.5, 1.0, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should always be positive");
    }

    @Test
    @DisplayName("getPrice correctly converts API double to BigDecimal")
    void testGetPriceConversion() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("TEST");
        instrument.setAssetClass(AssetClass.EQUITY);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("TEST")).thenReturn(new FauxnanceService.QuoteResponse("TEST", 123.456, 123.0, 124.0, 0.456, 0.4, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        
        assertEquals(new BigDecimal("123.45600"), price);
    }

    @Test
    @DisplayName("getPrice for CRYPTO with high price")
    void testGetPriceCryptoBounds() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("BTC");
        instrument.setAssetClass(AssetClass.CRYPTO);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("BTC")).thenReturn(new FauxnanceService.QuoteResponse("BTC", 65000.00, 64900.00, 65100.00, 1000.00, 1.6, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.CRYPTO, instrumentId);
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("getPrice for FX with realistic exchange rate")
    void testGetPriceFxBounds() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("GBP/USD");
        instrument.setAssetClass(AssetClass.FX);
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("GBP/USD")).thenReturn(new FauxnanceService.QuoteResponse("GBP/USD", 1.27, 1.269, 1.271, 0.01, 0.8, "2026-10-05"));
        
        BigDecimal price = pricingService.getPrice(AssetClass.FX, instrumentId);
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("getPrice returns non-null for all asset classes")
    void testGetPriceNeverNull() {
        UUID instrumentId = UUID.randomUUID();
        
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(instrumentId);
        instrument.setSymbol("TEST");
        
        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);
        when(fauxnanceService.getQuote("TEST")).thenReturn(new FauxnanceService.QuoteResponse("TEST", 50.00, 49.00, 51.00, 0.5, 1.0, "2026-10-05"));
        
        assertNotNull(pricingService.getPrice(AssetClass.EQUITY, instrumentId));
        assertNotNull(pricingService.getPrice(AssetClass.CRYPTO, instrumentId));
        assertNotNull(pricingService.getPrice(AssetClass.FX, instrumentId));
    }
}
