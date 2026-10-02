package com.neueda.leap.pricing.impl;

import com.neueda.leap.pricing.FeeStrategy;
import com.neueda.leap.enums.AssetClassFeeStructure;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee strategy for FX asset class
 * Implements fee calculation based on AssetClassFeeStructure configuration
 */
public class FXFeeStrategy implements FeeStrategy {

    private static final AssetClassFeeStructure config = AssetClassFeeStructure.FX;

    @Override
    public BigDecimal calculateFee(BigDecimal orderValue) {
        if (orderValue == null) {
            throw new IllegalArgumentException("Order value cannot be null");
        }
        
        BigDecimal percentageFee = orderValue.multiply(config.getFeePercentage());
        BigDecimal actualFee = percentageFee.max(config.getMinimumFee());
        return actualFee.setScale(5, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getFeePercentage() {
        return config.getFeePercentage();
    }

    @Override
    public BigDecimal getMinimumFee() {
        return config.getMinimumFee();
    }
}
