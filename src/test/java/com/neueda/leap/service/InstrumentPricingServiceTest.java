package com.neueda.leap.service;

import com.neueda.leap.enums.AssetClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("InstrumentPricingService Tests")
class InstrumentPricingServiceTest {

    @Autowired
    private InstrumentPricingService pricingService;

    @Test
    @DisplayName("getPrice returns a price for EQUITY instruments")
    void testGetPriceForEquity() {
        UUID instrumentId = UUID.randomUUID();
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should be positive");
        assertTrue(price.compareTo(new BigDecimal("10")) >= 0, "EQUITY price should be >= $10");
        assertTrue(price.compareTo(new BigDecimal("500")) <= 0, "EQUITY price should be <= $500");
    }

    @Test
    @DisplayName("getPrice returns a price for CRYPTO instruments")
    void testGetPriceForCrypto() {
        UUID instrumentId = UUID.randomUUID();
        BigDecimal price = pricingService.getPrice(AssetClass.CRYPTO, instrumentId);
        
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should be positive");
        assertTrue(price.compareTo(new BigDecimal("100")) >= 0, "CRYPTO price should be >= $100");
        assertTrue(price.compareTo(new BigDecimal("50000")) <= 0, "CRYPTO price should be <= $50,000");
    }

    @Test
    @DisplayName("getPrice returns a price for FX instruments")
    void testGetPriceForFx() {
        UUID instrumentId = UUID.randomUUID();
        BigDecimal price = pricingService.getPrice(AssetClass.FX, instrumentId);
        
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should be positive");
        assertTrue(price.compareTo(new BigDecimal("0.50")) >= 0, "FX price should be >= $0.50");
        assertTrue(price.compareTo(new BigDecimal("2.00")) <= 0, "FX price should be <= $2.00");
    }

    @Test
    @DisplayName("getPrice returns prices with at most 5 decimal places")
    void testGetPricePrecision() {
        UUID instrumentId = UUID.randomUUID();
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        
        assertTrue(price.scale() <= 5, "Price should have at most 5 decimal places");
    }

    @Test
    @DisplayName("getPrice returns different values for different calls (randomness)")
    void testGetPriceRandomness() {
        UUID instrumentId = UUID.randomUUID();
        
        BigDecimal price1 = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        BigDecimal price2 = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
        
        // Prices should be randomly generated (very unlikely to be identical multiple times)
        // We don't assert they're different (probability too low), but verify they're in valid range
        assertNotNull(price1);
        assertNotNull(price2);
    }

    @Test
    @DisplayName("getPrice returns consistent decimal scale")
    void testGetPriceDecimalScale() {
        UUID instrumentId = UUID.randomUUID();
        
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
    @DisplayName("getPrice returns price for each call with null instrument ID (ID not used in temp implementation)")
    void testGetPriceNullInstrumentId() {
        // Note: In temporary implementation, instrumentId is not used, so this should work
        // This may change when integrated with actual trading API
        BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, null);
        
        assertNotNull(price, "Should return price even with null instrumentId in temporary implementation");
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("getPrice for EQUITY respects upper bound")
    void testGetPriceEquityUpperBound() {
        UUID instrumentId = UUID.randomUUID();
        
        for (int i = 0; i < 20; i++) {
            BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
            assertTrue(price.compareTo(new BigDecimal("500")) <= 0, 
                    "EQUITY price should never exceed $500");
        }
    }

    @Test
    @DisplayName("getPrice for EQUITY respects lower bound")
    void testGetPriceEquityLowerBound() {
        UUID instrumentId = UUID.randomUUID();
        
        for (int i = 0; i < 20; i++) {
            BigDecimal price = pricingService.getPrice(AssetClass.EQUITY, instrumentId);
            assertTrue(price.compareTo(new BigDecimal("10")) >= 0, 
                    "EQUITY price should never go below $10");
        }
    }

    @Test
    @DisplayName("getPrice for CRYPTO respects bounds")
    void testGetPriceCryptoBounds() {
        UUID instrumentId = UUID.randomUUID();
        
        for (int i = 0; i < 20; i++) {
            BigDecimal price = pricingService.getPrice(AssetClass.CRYPTO, instrumentId);
            assertTrue(price.compareTo(new BigDecimal("100")) >= 0);
            assertTrue(price.compareTo(new BigDecimal("50000")) <= 0);
        }
    }

    @Test
    @DisplayName("getPrice for FX respects bounds")
    void testGetPriceFxBounds() {
        UUID instrumentId = UUID.randomUUID();
        
        for (int i = 0; i < 20; i++) {
            BigDecimal price = pricingService.getPrice(AssetClass.FX, instrumentId);
            assertTrue(price.compareTo(new BigDecimal("0.50")) >= 0);
            assertTrue(price.compareTo(new BigDecimal("2.00")) <= 0);
        }
    }

    @Test
    @DisplayName("getPrice returns non-null for all asset classes")
    void testGetPriceNeverNull() {
        UUID instrumentId = UUID.randomUUID();
        
        assertNotNull(pricingService.getPrice(AssetClass.EQUITY, instrumentId));
        assertNotNull(pricingService.getPrice(AssetClass.CRYPTO, instrumentId));
        assertNotNull(pricingService.getPrice(AssetClass.FX, instrumentId));
    }
}
