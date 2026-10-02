package com.neueda.leap.repository;

import com.neueda.leap.Instrument;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Instrument persistence operations
 */
public interface InstrumentRepository {
    Instrument findById(UUID instrumentId);
    Instrument findBySymbol(String symbol);
    List<Instrument> findTradable(int limit, int offset);
    int countTradable();
    List<Instrument> findAll(int limit, int offset);
    int countAll();
    List<Instrument> findByStatus(String status, int limit, int offset);
    List<Instrument> findByAssetClass(String assetClass, int limit, int offset);
    void save(String symbol, String name, String assetClass);
    void updateStatus(UUID instrumentId, String status);
}
