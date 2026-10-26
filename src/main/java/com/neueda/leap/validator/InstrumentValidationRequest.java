package com.neueda.leap.validator;

/**
 * Data class for instrument validation inputs
 */
public class InstrumentValidationRequest {
    private final String symbol;
    private final String name;
    private final String assetClass;

    public InstrumentValidationRequest(String symbol, String name, String assetClass) {
        this.symbol = symbol;
        this.name = name;
        this.assetClass = assetClass;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getAssetClass() {
        return assetClass;
    }
}
