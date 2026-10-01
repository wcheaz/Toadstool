package com.neueda.leap.pricing.impl;

import com.neueda.leap.pricing.FeeStrategy;
import com.neueda.leap.pricing.FeeStrategyFactory;
import com.neueda.leap.enums.AssetClass;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of FeeStrategyFactory
 * Maps asset classes to their corresponding fee strategies
 */
@Component
public class FeeStrategyFactoryImpl implements FeeStrategyFactory {

    private final Map<AssetClass, FeeStrategy> strategies = new HashMap<>();

    public FeeStrategyFactoryImpl() {
        strategies.put(AssetClass.EQUITY, new EquityFeeStrategy());
        strategies.put(AssetClass.CRYPTO, new CryptoFeeStrategy());
        strategies.put(AssetClass.FX, new FXFeeStrategy());
    }

    @Override
    public FeeStrategy getStrategy(AssetClass assetClass) {
        if (assetClass == null) {
            throw new IllegalArgumentException("Asset class cannot be null");
        }

        FeeStrategy strategy = strategies.get(assetClass);
        if (strategy == null) {
            throw new IllegalArgumentException("No fee strategy configured for asset class: " + assetClass);
        }

        return strategy;
    }
}
