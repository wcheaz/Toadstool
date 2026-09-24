package com.neueda.leap;

import java.util.UUID;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;

public class Instrument {
    
    private UUID instrumentId;
    private String symbol;
    private String name;
    private AssetClass assetClass;
    private InstrumentStatus status;
    
    // Constructors
    public Instrument() {
        this.status = InstrumentStatus.TRADABLE;
    }
    
    public Instrument(String symbol, String name, AssetClass assetClass, InstrumentStatus status) {
        this.symbol = symbol;
        this.name = name;
        this.assetClass = assetClass;
        this.status = status;
    }
    
    public Instrument(UUID instrumentId, String symbol, String name, AssetClass assetClass, InstrumentStatus status) {
        this.instrumentId = instrumentId;
        this.symbol = symbol;
        this.name = name;
        this.assetClass = assetClass;
        this.status = status;
    }
    
    // Getters and Setters
    public UUID getInstrumentId() {
        return instrumentId;
    }
    
    public void setInstrumentId(UUID instrumentId) {
        this.instrumentId = instrumentId;
    }
    
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
    
    public InstrumentStatus getStatus() {
        return status;
    }
    
    public void setStatus(InstrumentStatus status) {
        this.status = status;
    }
}

