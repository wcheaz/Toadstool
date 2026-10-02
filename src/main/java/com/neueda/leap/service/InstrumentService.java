package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.repository.InstrumentRepository;
import com.neueda.leap.validator.InstrumentValidator;
import com.neueda.leap.validator.InstrumentValidationRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Instrument operations
 */
@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentValidator instrumentValidator;

    public InstrumentService(InstrumentRepository instrumentRepository, 
                            InstrumentValidator instrumentValidator) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentValidator = instrumentValidator;
    }

    /**
     * Get instrument by ID
     */
    public Instrument getInstrumentById(UUID instrumentId) {
        return instrumentRepository.findById(instrumentId);
    }

    /**
     * Get instrument by symbol
     */
    public Instrument getInstrumentBySymbol(String symbol) {
        return instrumentRepository.findBySymbol(symbol);
    }

    /**
     * List tradable instruments
     */
    public List<Instrument> listTradableInstruments(int limit, int offset) {
        return instrumentRepository.findTradable(limit, offset);
    }

    /**
     * Count tradable instruments
     */
    public int countTradableInstruments() {
        return instrumentRepository.countTradable();
    }

    /**
     * List all instruments (including halted/inactive)
     */
    public List<Instrument> listAllInstruments(int limit, int offset) {
        return instrumentRepository.findAll(limit, offset);
    }

    /**
     * List all instruments without pagination
     */
    public List<Instrument> listAllInstruments() {
        return instrumentRepository.findAll(Integer.MAX_VALUE, 0);
    }

    /**
     * Count all instruments
     */
    public int countAllInstruments() {
        return instrumentRepository.countAll();
    }

    /**
     * List instruments by status
     */
    public List<Instrument> listInstrumentsByStatus(String status, int limit, int offset) {
        return instrumentRepository.findByStatus(status, limit, offset);
    }

    /**
     * List instruments by status without pagination
     */
    public List<Instrument> listInstrumentsByStatus(String status) {
        return instrumentRepository.findByStatus(status, Integer.MAX_VALUE, 0);
    }

    /**
     * List instruments by asset class
     */
    public List<Instrument> listInstrumentsByAssetClass(String assetClass, int limit, int offset) {
        return instrumentRepository.findByAssetClass(assetClass, limit, offset);
    }

    /**
     * Create a new instrument (admin-only)
     */
    public Instrument createInstrument(String symbol, String name, String assetClass) {
        // Use injected validator
        instrumentValidator.validate(new InstrumentValidationRequest(symbol, name, assetClass));
        
        // Check if symbol already exists
        if (getInstrumentBySymbol(symbol) != null) {
            throw new IllegalArgumentException("Instrument symbol already exists: " + symbol);
        }

        instrumentRepository.save(symbol.toUpperCase(), name, assetClass);
        
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
        instrumentRepository.updateStatus(instrumentId, status);
        
        // Return the updated instrument
        return getInstrumentById(instrumentId);
    }
}
