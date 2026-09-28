package com.neueda.leap.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssetClassFeeStructure Tests")
class AssetClassFeeStructureTest {

    @Test
    @DisplayName("EQUITY enum value exists")
    void testEquityEnumExists() {
        AssetClassFeeStructure equity = AssetClassFeeStructure.EQUITY;
        assertNotNull(equity);
        assertNotNull(equity.getFeePercentage());
        assertNotNull(equity.getMinimumFee());
    }

    @Test
    @DisplayName("CRYPTO enum value exists")
    void testCryptoEnumExists() {
        AssetClassFeeStructure crypto = AssetClassFeeStructure.CRYPTO;
        assertNotNull(crypto);
        assertNotNull(crypto.getFeePercentage());
        assertNotNull(crypto.getMinimumFee());
    }

    @Test
    @DisplayName("FX enum value exists")
    void testFxEnumExists() {
        AssetClassFeeStructure fx = AssetClassFeeStructure.FX;
        assertNotNull(fx);
        assertNotNull(fx.getFeePercentage());
        assertNotNull(fx.getMinimumFee());
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
    @DisplayName("calculateFee returns positive value for large order value")
    void testCalculateFeeReturnsPositiveForLargeOrder() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            BigDecimal largeOrderValue = new BigDecimal("10000.00");
            BigDecimal fee = feeStructure.calculateFee(largeOrderValue);
            
            assertTrue(fee.compareTo(BigDecimal.ZERO) > 0, 
                    feeStructure + " should return positive fee for large order");
        }
    }

    @Test
    @DisplayName("calculateFee returns at least the minimum fee for small order")
    void testCalculateFeeReturnsAtLeastMinimumForSmallOrder() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            BigDecimal smallOrderValue = new BigDecimal("10.00");
            BigDecimal fee = feeStructure.calculateFee(smallOrderValue);
            
            assertTrue(fee.compareTo(feeStructure.getMinimumFee()) >= 0,
                    feeStructure + " should return at least minimum fee");
            assertEquals(fee, feeStructure.getMinimumFee(),
                    feeStructure + " should apply minimum fee for small orders");
        }
    }

    @Test
    @DisplayName("calculateFee for large order exceeds minimum fee")
    void testCalculateFeeForLargeOrderExceedsMinimum() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            // Use a sufficiently large order value to exceed percentage-based fee
            BigDecimal largeOrderValue = new BigDecimal("100000.00");
            BigDecimal fee = feeStructure.calculateFee(largeOrderValue);
            
            assertTrue(fee.compareTo(feeStructure.getMinimumFee()) >= 0,
                    feeStructure + " calculated fee should meet or exceed minimum");
        }
    }

    @Test
    @DisplayName("calculateFee maintains 5 decimal precision")
    void testCalculateFeePrecision() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            BigDecimal orderValue = new BigDecimal("12345.67");
            BigDecimal fee = feeStructure.calculateFee(orderValue);
            
            assertEquals(5, fee.scale(), 
                    feeStructure + " should maintain 5 decimal places");
        }
    }

    @Test
    @DisplayName("calculateFee handles zero order value with minimum fee")
    void testCalculateFeeZeroOrderValue() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            BigDecimal orderValue = BigDecimal.ZERO;
            BigDecimal fee = feeStructure.calculateFee(orderValue);
            
            assertEquals(feeStructure.getMinimumFee(), fee,
                    feeStructure + " should return minimum fee for zero order value");
        }
    }

    @Test
    @DisplayName("calculateFee results increase with larger order values")
    void testCalculateFeeIncreases() {
        for (AssetClassFeeStructure feeStructure : AssetClassFeeStructure.values()) {
            BigDecimal smallOrder = new BigDecimal("1000.00");
            BigDecimal largeOrder = new BigDecimal("100000.00");
            
            BigDecimal feeSmall = feeStructure.calculateFee(smallOrder);
            BigDecimal feeLarge = feeStructure.calculateFee(largeOrder);
            
            assertTrue(feeLarge.compareTo(feeSmall) >= 0,
                    feeStructure + " fee should increase with order value");
        }
    }
}
