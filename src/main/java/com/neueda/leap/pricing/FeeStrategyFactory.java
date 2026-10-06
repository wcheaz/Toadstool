package com.neueda.leap.pricing;

import com.neueda.leap.enums.AssetClass;

/**
 * Factory for creating FeeStrategy implementations based on asset class
 * This abstraction allows new fee strategies to be added without modifying existing code
 */
public interface FeeStrategyFactory {
    /**
     * Get the fee strategy for a given asset class
     * @param assetClass The asset class
     * @return The corresponding fee strategy
     * @throws IllegalArgumentException if asset class is not supported
     */
    FeeStrategy getStrategy(AssetClass assetClass);
}
