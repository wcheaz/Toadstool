package com.neueda.leap.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee structure enumeration for different asset classes
 * Maps AssetClass to their respective trading fees
 * 
 * Fees are calculated as: MAX(orderValue × percentage, minimumFee)
 * All monetary values use 5 decimal places precision
 */
public enum AssetClassFeeStructure {
    EQUITY(new BigDecimal("0.0005"), new BigDecimal("1.00000")),      // 0.05% minimum $1.00
    CRYPTO(new BigDecimal("0.001"), new BigDecimal("2.00000")),       // 0.10% minimum $2.00
    FX(new BigDecimal("0.0002"), new BigDecimal("0.50000"));          // 0.02% minimum $0.50

    private final BigDecimal feePercentage;
    private final BigDecimal minimumFee;

    /**
     * Constructs a fee structure with percentage-based and minimum fees
     * 
     * @param feePercentage The percentage fee (e.g., 0.0005 for 0.05%)
     * @param minimumFee The minimum fee regardless of order size
     */
    AssetClassFeeStructure(BigDecimal feePercentage, BigDecimal minimumFee) {
        this.feePercentage = feePercentage;
        this.minimumFee = minimumFee;
    }

    /**
     * Gets the fee percentage for this asset class
     * 
     * @return The fee percentage as BigDecimal
     */
    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    /**
     * Gets the minimum fee for this asset class
     * 
     * @return The minimum fee as BigDecimal
     */
    public BigDecimal getMinimumFee() {
        return minimumFee;
    }

    /**
     * Calculates the fee for a given order value
     * 
     * Formula: MAX(orderValue × feePercentage, minimumFee)
     * Result is rounded to 5 decimal places using ROUND_HALF_UP
     * 
     * @param orderValue The total value of the order (price × quantity)
     * @return The calculated fee with 5 decimal places precision
     */
    public BigDecimal calculateFee(BigDecimal orderValue) {
        if (orderValue == null) {
            throw new IllegalArgumentException("Order value cannot be null");
        }

        // Calculate percentage-based fee
        BigDecimal percentageFee = orderValue.multiply(feePercentage);
        
        // Return the maximum of percentage fee and minimum fee
        BigDecimal actualFee = percentageFee.max(minimumFee);
        
        // Ensure 5 decimal places precision
        return actualFee.setScale(5, RoundingMode.HALF_UP);
    }

    /**
     * Gets the fee structure for a given asset class
     * 
     * @param assetClass The asset class to get the fee structure for
     * @return The corresponding AssetClassFeeStructure enum value
     * @throws IllegalArgumentException if the asset class is not supported
     */
    public static AssetClassFeeStructure fromAssetClass(AssetClass assetClass) {
        if (assetClass == null) {
            throw new IllegalArgumentException("Asset class cannot be null");
        }

        switch (assetClass) {
            case EQUITY:
                return EQUITY;
            case CRYPTO:
                return CRYPTO;
            case FX:
                return FX;
            default:
                throw new IllegalArgumentException("Unsupported asset class: " + assetClass);
        }
    }
}
