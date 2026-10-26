package com.neueda.leap.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssetClassFeeStructure Tests")
class AssetClassFeeStructureTest {

    @Test
    @DisplayName("EQUITY enum value has correct configuration")
    void testEquityConfiguration() {
        AssetClassFeeStructure equity = AssetClassFeeStructure.EQUITY;
        assertNotNull(equity);
        assertEquals(new BigDecimal("0.0005"), equity.getFeePercentage());
        assertEquals(new BigDecimal("1.00000"), equity.getMinimumFee());
    }

    @Test
    @DisplayName("CRYPTO enum value has correct configuration")
    void testCryptoConfiguration() {
        AssetClassFeeStructure crypto = AssetClassFeeStructure.CRYPTO;
        assertNotNull(crypto);
        assertEquals(new BigDecimal("0.001"), crypto.getFeePercentage());
        assertEquals(new BigDecimal("2.00000"), crypto.getMinimumFee());
    }

    @Test
    @DisplayName("FX enum value has correct configuration")
    void testFxConfiguration() {
        AssetClassFeeStructure fx = AssetClassFeeStructure.FX;
        assertNotNull(fx);
        assertEquals(new BigDecimal("0.0002"), fx.getFeePercentage());
        assertEquals(new BigDecimal("0.50000"), fx.getMinimumFee());
    }

    @Test
    @DisplayName("All AssetClassFeeStructure values have positive fee percentages")
    void testAllFeePercentagesArePositive() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            assertTrue(feeStructure.getFeePercentage().compareTo(BigDecimal.ZERO) > 0, 
                    feeStructure + " should have positive fee percentage");
        }
    }

    @Test
    @DisplayName("All AssetClassFeeStructure values have positive minimum fees")
    void testAllMinimumFeesArePositive() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            assertTrue(feeStructure.getMinimumFee().compareTo(BigDecimal.ZERO) > 0,
                    feeStructure + " should have positive minimum fee");
        }
    }

    @Test
    @DisplayName("All enum values are retrievable")
    void testAllEnumValuesExist() {
        assertEquals(3, AssetClassFeeStructure.values().length);
        assertNotNull(AssetClassFeeStructure.valueOf("EQUITY"));
        assertNotNull(AssetClassFeeStructure.valueOf("CRYPTO"));
        assertNotNull(AssetClassFeeStructure.valueOf("FX"));
    }

    @Test
    @DisplayName("Fee percentage is immutable")
    void testFeePercentageImmutable() {
        AssetClassFeeStructure equity = AssetClassFeeStructure.EQUITY;
        BigDecimal original = equity.getFeePercentage();
        
        // Verify same value returned consistently
        assertEquals(original, equity.getFeePercentage());
        assertEquals(original, equity.getFeePercentage());
    }

    @Test
    @DisplayName("Minimum fee is immutable")
    void testMinimumFeeImmutable() {
        AssetClassFeeStructure crypto = AssetClassFeeStructure.CRYPTO;
        BigDecimal original = crypto.getMinimumFee();
        
        // Verify same value returned consistently
        assertEquals(original, crypto.getMinimumFee());
        assertEquals(original, crypto.getMinimumFee());
    }

    void testEquityFeeLowerThanCrypto() {
        assertTrue(AssetClassFeeStructure.EQUITY.getFeePercentage()
                .compareTo(AssetClassFeeStructure.CRYPTO.getFeePercentage()) < 0,
                "EQUITY fee (0.05%) should be lower than CRYPTO (0.10%)");
    }

    @Test
    @DisplayName("FX has lowest fee percentage")
    void testFxHasLowestFeePercentage() {
        BigDecimal fxFee = AssetClassFeeStructure.FX.getFeePercentage();
        assertTrue(fxFee.compareTo(AssetClassFeeStructure.EQUITY.getFeePercentage()) < 0);
        assertTrue(fxFee.compareTo(AssetClassFeeStructure.CRYPTO.getFeePercentage()) < 0);
    }

    @Test
    @DisplayName("CRYPTO has highest minimum fee")
    void testCryptoHasHighestMinimumFee() {
        BigDecimal cryptoMin = AssetClassFeeStructure.CRYPTO.getMinimumFee();
        assertTrue(cryptoMin.compareTo(AssetClassFeeStructure.EQUITY.getMinimumFee()) > 0);
        assertTrue(cryptoMin.compareTo(AssetClassFeeStructure.FX.getMinimumFee()) > 0);
    }
}
