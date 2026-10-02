package com.neueda.leap.pricing;

import java.math.BigDecimal;

/**
 * Strategy interface for calculating trading fees
 * Allows for different fee calculation implementations
 */
public interface FeeStrategy {
    /**
     * Calculate fee for a given order value
     * @param orderValue The total order value (price × quantity)
     * @return The calculated fee
     */
    BigDecimal calculateFee(BigDecimal orderValue);

    /**
     * Get the percentage fee for this strategy
     * @return Fee percentage
     */
    BigDecimal getFeePercentage();

    /**
     * Get the minimum fee for this strategy
     * @return Minimum fee
     */
    BigDecimal getMinimumFee();
}
