package com.neueda.leap.enums;

import java.math.BigDecimal;

/**
 * Fee structure enumeration for different asset classes
 * Holds fee configuration (percentages and minimums) for each asset class
 * 
 * Fee calculation logic is delegated to FeeStrategy implementations
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
}
