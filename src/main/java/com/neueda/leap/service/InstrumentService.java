package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.mapper.InstrumentMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Instrument operations
 */
@Service
public class InstrumentService {

    private final InstrumentMapper instrumentMapper;

    public InstrumentService(InstrumentMapper instrumentMapper) {
        this.instrumentMapper = instrumentMapper;
    }

    /**
     * Get instrument by ID
     */
    public Instrument getInstrumentById(UUID instrumentId) {
        return instrumentMapper.selectInstrumentById(instrumentId);
    }

    /**
     * Get instrument by symbol
     */
    public Instrument getInstrumentBySymbol(String symbol) {
        return instrumentMapper.selectInstrumentBySymbol(symbol);
    }

    /**
     * List tradable instruments
     */
    public List<Instrument> listTradableInstruments(int limit, int offset) {
        return instrumentMapper.selectTradableInstruments(limit, offset);
    }

    /**
     * Count tradable instruments
     */
    public int countTradableInstruments() {
        return instrumentMapper.countTradableInstruments();
    }

    /**
     * List all instruments (including halted/inactive)
     */
    public List<Instrument> listAllInstruments(int limit, int offset) {
        return instrumentMapper.selectAllInstruments(limit, offset);
    }

    /**
     * List all instruments without pagination
     */
    public List<Instrument> listAllInstruments() {
        return instrumentMapper.selectAllInstruments(Integer.MAX_VALUE, 0);
    }

    /**
     * Count all instruments
     */
    public int countAllInstruments() {
        return instrumentMapper.countAllInstruments();
    }

    /**
     * List instruments by status
     */
    public List<Instrument> listInstrumentsByStatus(String status, int limit, int offset) {
        return instrumentMapper.selectInstrumentsByStatus(status, limit, offset);
    }

    /**
     * List instruments by status without pagination
     */
    public List<Instrument> listInstrumentsByStatus(String status) {
        return instrumentMapper.selectInstrumentsByStatus(status, Integer.MAX_VALUE, 0);
    }

    /**
     * List instruments by asset class
     */
    public List<Instrument> listInstrumentsByAssetClass(String assetClass, int limit, int offset) {
        return instrumentMapper.selectInstrumentsByAssetClass(assetClass, limit, offset);
    }

    /**
     * Create a new instrument (admin-only)
     */
    public Instrument createInstrument(String symbol, String name, String assetClass) {
        validateInstrumentInput(symbol, name, assetClass);
        
        // Check if symbol already exists
        if (getInstrumentBySymbol(symbol) != null) {
            throw new IllegalArgumentException("Instrument symbol already exists: " + symbol);
        }

        instrumentMapper.insertInstrument(symbol.toUpperCase(), name, assetClass);
        
        // Return the newly created instrument
        return getInstrumentBySymbol(symbol.toUpperCase());
    }

    /**
     * Update instrument status (admin-only)
     */
    public Instrument updateInstrumentStatus(UUID instrumentId, String status) {
        if (status == null || (!status.equals("TRADABLE") && !status.equals("HALTED") && !status.equals("INACTIVE"))) {
            throw new IllegalArgumentException("Invalid instrument status");
        }
        instrumentMapper.updateInstrumentStatus(instrumentId, status);
        
        // Return the updated instrument
        return getInstrumentById(instrumentId);
    }

    /**
     * Validate instrument input
     */
    private void validateInstrumentInput(String symbol, String name, String assetClass) {
        if (symbol == null || symbol.trim().isEmpty() || symbol.length() > 40) {
            throw new IllegalArgumentException("Symbol must be 1-40 characters");
        }
        if (name == null || name.trim().isEmpty() || name.length() > 160) {
            throw new IllegalArgumentException("Name must be 1-160 characters");
        }
        if (assetClass == null || (!assetClass.equals("EQUITY") && !assetClass.equals("FX") && !assetClass.equals("CRYPTO"))) {
            throw new IllegalArgumentException("Asset class must be EQUITY, FX, or CRYPTO");
        }
    }
}
