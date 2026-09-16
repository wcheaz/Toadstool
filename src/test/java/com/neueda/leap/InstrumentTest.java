package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Instrument Tests")
class InstrumentTest {

    private Instrument instrument;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        testUUID = UUID.randomUUID();
    }

    @Test
    @DisplayName("Default constructor creates empty Instrument")
    void testDefaultConstructor() {
        instrument = new Instrument();
        assertNull(instrument.getInstrumentId());
        assertNull(instrument.getSymbol());
        assertNull(instrument.getName());
        assertNull(instrument.getAssetClass());
        assertNull(instrument.getStatus());
    }

    @Test
    @DisplayName("Constructor with symbol, name, assetClass, status")
    void testConstructorWithoutId() {
        instrument = new Instrument("AAPL", "Apple Inc.", 
            Instrument.AssetClass.EQUITY, 
            Instrument.InstrumentStatus.TRADABLE);
        
        assertNull(instrument.getInstrumentId());
        assertEquals("AAPL", instrument.getSymbol());
        assertEquals("Apple Inc.", instrument.getName());
        assertEquals(Instrument.AssetClass.EQUITY, instrument.getAssetClass());
        assertEquals(Instrument.InstrumentStatus.TRADABLE, instrument.getStatus());
    }

    @Test
    @DisplayName("Constructor with all fields including ID")
    void testConstructorWithId() {
        instrument = new Instrument(testUUID, "GOOGL", "Alphabet Inc.",
            Instrument.AssetClass.EQUITY,
            Instrument.InstrumentStatus.TRADABLE);
        
        assertEquals(testUUID, instrument.getInstrumentId());
        assertEquals("GOOGL", instrument.getSymbol());
        assertEquals("Alphabet Inc.", instrument.getName());
        assertEquals(Instrument.AssetClass.EQUITY, instrument.getAssetClass());
        assertEquals(Instrument.InstrumentStatus.TRADABLE, instrument.getStatus());
    }

    @Test
    @DisplayName("setInstrumentId and getInstrumentId")
    void testSetAndGetInstrumentId() {
        instrument = new Instrument();
        instrument.setInstrumentId(testUUID);
        assertEquals(testUUID, instrument.getInstrumentId());
    }

    @Test
    @DisplayName("setSymbol and getSymbol")
    void testSetAndGetSymbol() {
        instrument = new Instrument();
        instrument.setSymbol("MSFT");
        assertEquals("MSFT", instrument.getSymbol());
    }

    @Test
    @DisplayName("setName and getName")
    void testSetAndGetName() {
        instrument = new Instrument();
        instrument.setName("Microsoft Corporation");
        assertEquals("Microsoft Corporation", instrument.getName());
    }

    @Test
    @DisplayName("setAssetClass and getAssetClass")
    void testSetAndGetAssetClass() {
        instrument = new Instrument();
        instrument.setAssetClass(Instrument.AssetClass.EQUITY);
        assertEquals(Instrument.AssetClass.EQUITY, instrument.getAssetClass());
    }

    @Test
    @DisplayName("setStatus and getStatus")
    void testSetAndGetStatus() {
        instrument = new Instrument();
        instrument.setStatus(Instrument.InstrumentStatus.HALTED);
        assertEquals(Instrument.InstrumentStatus.HALTED, instrument.getStatus());
    }

    @Test
    @DisplayName("Multiple asset classes")
    void testMultipleAssetClasses() {
        instrument = new Instrument();
        
        instrument.setAssetClass(Instrument.AssetClass.EQUITY);
        assertEquals(Instrument.AssetClass.EQUITY, instrument.getAssetClass());
        
        instrument.setAssetClass(Instrument.AssetClass.FX);
        assertEquals(Instrument.AssetClass.FX, instrument.getAssetClass());
        
        instrument.setAssetClass(Instrument.AssetClass.CRYPTO);
        assertEquals(Instrument.AssetClass.CRYPTO, instrument.getAssetClass());
    }

    @Test
    @DisplayName("Multiple statuses")
    void testMultipleStatuses() {
        instrument = new Instrument();
        
        instrument.setStatus(Instrument.InstrumentStatus.TRADABLE);
        assertEquals(Instrument.InstrumentStatus.TRADABLE, instrument.getStatus());
        
        instrument.setStatus(Instrument.InstrumentStatus.HALTED);
        assertEquals(Instrument.InstrumentStatus.HALTED, instrument.getStatus());
        
        instrument.setStatus(Instrument.InstrumentStatus.INACTIVE);
        assertEquals(Instrument.InstrumentStatus.INACTIVE, instrument.getStatus());
    }

    @Test
    @DisplayName("Update all fields")
    void testUpdateAllFields() {
        instrument = new Instrument();
        UUID newId = UUID.randomUUID();
        
        instrument.setInstrumentId(newId);
        instrument.setSymbol("TSLA");
        instrument.setName("Tesla Inc.");
        instrument.setAssetClass(Instrument.AssetClass.EQUITY);
        instrument.setStatus(Instrument.InstrumentStatus.TRADABLE);
        
        assertEquals(newId, instrument.getInstrumentId());
        assertEquals("TSLA", instrument.getSymbol());
        assertEquals("Tesla Inc.", instrument.getName());
        assertEquals(Instrument.AssetClass.EQUITY, instrument.getAssetClass());
        assertEquals(Instrument.InstrumentStatus.TRADABLE, instrument.getStatus());
    }

    @Test
    @DisplayName("FX Instrument")
    void testFxInstrument() {
        instrument = new Instrument(testUUID, "EURUSD", "Euro vs US Dollar",
            Instrument.AssetClass.FX,
            Instrument.InstrumentStatus.TRADABLE);
        
        assertEquals("EURUSD", instrument.getSymbol());
        assertEquals(Instrument.AssetClass.FX, instrument.getAssetClass());
    }

    @Test
    @DisplayName("Crypto Instrument")
    void testCryptoInstrument() {
        instrument = new Instrument(testUUID, "BTC", "Bitcoin",
            Instrument.AssetClass.CRYPTO,
            Instrument.InstrumentStatus.TRADABLE);
        
        assertEquals("BTC", instrument.getSymbol());
        assertEquals(Instrument.AssetClass.CRYPTO, instrument.getAssetClass());
    }

    @Test
    @DisplayName("Instrument with halted status")
    void testHaltedInstrument() {
        instrument = new Instrument("HALT", "Halted Stock",
            Instrument.AssetClass.EQUITY,
            Instrument.InstrumentStatus.HALTED);
        
        assertEquals(Instrument.InstrumentStatus.HALTED, instrument.getStatus());
    }

    @Test
    @DisplayName("Instrument with inactive status")
    void testInactiveInstrument() {
        instrument = new Instrument("INAC", "Inactive Stock",
            Instrument.AssetClass.EQUITY,
            Instrument.InstrumentStatus.INACTIVE);
        
        assertEquals(Instrument.InstrumentStatus.INACTIVE, instrument.getStatus());
    }
}
