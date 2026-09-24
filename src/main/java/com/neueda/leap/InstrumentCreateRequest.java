package com.neueda.leap;

import com.neueda.leap.enums.AssetClass;

/**
 * Request DTO for creating an instrument
 */
public class InstrumentCreateRequest {
    private String symbol;
    private String name;
    private AssetClass assetClass;

    // Constructors
    public InstrumentCreateRequest() {
    }

    public InstrumentCreateRequest(String symbol, String name, AssetClass assetClass) {
        this.symbol = symbol;
        this.name = name;
        this.assetClass = assetClass;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(AssetClass assetClass) {
        this.assetClass = assetClass;
    }
}
