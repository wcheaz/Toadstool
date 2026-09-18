package com.neueda.leap.mapper;

import com.neueda.leap.Instrument;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class InstrumentMapperTest {

    @Autowired
    private InstrumentMapper instrumentMapper;

    @Test
    public void testSelectTradableInstruments() {
        List<Instrument> instruments = instrumentMapper.selectTradableInstruments(10, 0);
        assertNotNull(instruments);
        assertFalse(instruments.isEmpty());
        assertTrue(instruments.size() <= 10);
        instruments.forEach(i -> assertEquals(InstrumentStatus.TRADABLE, i.getStatus()));
    }

    @Test
    public void testCountTradableInstruments() {
        int count = instrumentMapper.countTradableInstruments();
        assertTrue(count > 0);
        assertEquals(16, count); // Test data inserts 16 instruments
    }

    @Test
    public void testSelectInstrumentBySymbol() {
        Instrument instrument = instrumentMapper.selectInstrumentBySymbol("AAPL");
        assertNotNull(instrument);
        assertEquals("AAPL", instrument.getSymbol());
        assertEquals("Apple Inc.", instrument.getName());
        assertEquals(AssetClass.EQUITY, instrument.getAssetClass());
        assertEquals(InstrumentStatus.TRADABLE, instrument.getStatus());
    }

    @Test
    public void testSelectInstrumentBySymbolNotFound() {
        Instrument instrument = instrumentMapper.selectInstrumentBySymbol("INVALID");
        assertNull(instrument);
    }

    @Test
    public void testSelectInstrumentById() {
        // First get an instrument by symbol to get its ID
        Instrument instrumentBySymbol = instrumentMapper.selectInstrumentBySymbol("GOOGL");
        assertNotNull(instrumentBySymbol);

        // Then fetch by ID
        Instrument instrumentById = instrumentMapper.selectInstrumentById(instrumentBySymbol.getInstrumentId());
        assertNotNull(instrumentById);
        assertEquals("GOOGL", instrumentById.getSymbol());
        assertEquals("Alphabet Inc.", instrumentById.getName());
    }

    @Test
    public void testCryptoAssetClass() {
        Instrument btc = instrumentMapper.selectInstrumentBySymbol("BTC/USD");
        assertNotNull(btc);
        assertEquals(AssetClass.CRYPTO, btc.getAssetClass());
    }

    @Test
    public void testForexAssetClass() {
        Instrument eurusd = instrumentMapper.selectInstrumentBySymbol("EUR/USD");
        assertNotNull(eurusd);
        assertEquals(AssetClass.FX, eurusd.getAssetClass());
    }

    @Test
    public void testPagination() {
        List<Instrument> page1 = instrumentMapper.selectTradableInstruments(5, 0);
        List<Instrument> page2 = instrumentMapper.selectTradableInstruments(5, 5);
        
        assertNotNull(page1);
        assertNotNull(page2);
        assertEquals(5, page1.size());
        assertEquals(5, page2.size());
        
        // Pages should be different
        assertNotEquals(page1.get(0).getInstrumentId(), page2.get(0).getInstrumentId());
    }
}
