package com.neueda.leap.repository.impl;

import com.neueda.leap.Instrument;
import com.neueda.leap.repository.InstrumentRepository;
import com.neueda.leap.mapper.InstrumentMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis-based implementation of InstrumentRepository
 */
@Repository
public class InstrumentRepositoryImpl implements InstrumentRepository {

    private final InstrumentMapper instrumentMapper;

    public InstrumentRepositoryImpl(InstrumentMapper instrumentMapper) {
        this.instrumentMapper = instrumentMapper;
    }

    @Override
    public Instrument findById(UUID instrumentId) {
        return instrumentMapper.selectInstrumentById(instrumentId);
    }

    @Override
    public Instrument findBySymbol(String symbol) {
        return instrumentMapper.selectInstrumentBySymbol(symbol);
    }

    @Override
    public List<Instrument> findTradable(int limit, int offset) {
        return instrumentMapper.selectTradableInstruments(limit, offset);
    }

    @Override
    public int countTradable() {
        return instrumentMapper.countTradableInstruments();
    }

    @Override
    public List<Instrument> findAll(int limit, int offset) {
        return instrumentMapper.selectAllInstruments(limit, offset);
    }

    @Override
    public int countAll() {
        return instrumentMapper.countAllInstruments();
    }

    @Override
    public List<Instrument> findByStatus(String status, int limit, int offset) {
        return instrumentMapper.selectInstrumentsByStatus(status, limit, offset);
    }

    @Override
    public List<Instrument> findByAssetClass(String assetClass, int limit, int offset) {
        return instrumentMapper.selectInstrumentsByAssetClass(assetClass, limit, offset);
    }

    @Override
    public void save(String symbol, String name, String assetClass) {
        instrumentMapper.insertInstrument(symbol, name, assetClass);
    }

    @Override
    public void updateStatus(UUID instrumentId, String status) {
        instrumentMapper.updateInstrumentStatus(instrumentId, status);
    }
}
