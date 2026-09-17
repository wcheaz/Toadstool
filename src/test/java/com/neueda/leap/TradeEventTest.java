package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.neueda.leap.enums.TradeEventEntityType;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TradeEvent Tests")
class TradeEventTest {

    private TradeEvent tradeEvent;
    private UUID testTradeEventId;
    private UUID testClientId;
    private UUID testEntityId;
    private OffsetDateTime testDateTime;
    private String testAction = "CREATE";
    private String testDetails = "Order created successfully";

    @BeforeEach
    void setUp() {
        testTradeEventId = UUID.randomUUID();
        testClientId = UUID.randomUUID();
        testEntityId = UUID.randomUUID();
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty TradeEvent")
    void testDefaultConstructor() {
        tradeEvent = new TradeEvent();
        assertNull(tradeEvent.getTradeEventId());
        assertNull(tradeEvent.getClientId());
        assertNull(tradeEvent.getEntityType());
        assertNull(tradeEvent.getEntityId());
        assertNull(tradeEvent.getAction());
        assertNull(tradeEvent.getOccurredAt());
        assertNull(tradeEvent.getDetails());
    }

    @Test
    @DisplayName("Constructor with entityType, entityId, and action")
    void testConstructorWithBasicFields() {
        tradeEvent = new TradeEvent(TradeEventEntityType.ORDER, testEntityId, testAction);
        
        assertNull(tradeEvent.getTradeEventId());
        assertNull(tradeEvent.getClientId());
        assertEquals(TradeEventEntityType.ORDER, tradeEvent.getEntityType());
        assertEquals(testEntityId, tradeEvent.getEntityId());
        assertEquals(testAction, tradeEvent.getAction());
        assertNull(tradeEvent.getOccurredAt());
        assertNull(tradeEvent.getDetails());
    }

    @Test
    @DisplayName("Constructor with clientId, entityType, entityId, action, and details")
    void testConstructorWithClientIdAndDetails() {
        tradeEvent = new TradeEvent(testClientId, TradeEventEntityType.FILL, testEntityId, "EXECUTE", testDetails);
        
        assertNull(tradeEvent.getTradeEventId());
        assertEquals(testClientId, tradeEvent.getClientId());
        assertEquals(TradeEventEntityType.FILL, tradeEvent.getEntityType());
        assertEquals(testEntityId, tradeEvent.getEntityId());
        assertEquals("EXECUTE", tradeEvent.getAction());
        assertNull(tradeEvent.getOccurredAt());
        assertEquals(testDetails, tradeEvent.getDetails());
    }

    @Test
    @DisplayName("Constructor with all fields including ID and occurredAt")
    void testConstructorWithAllFields() {
        tradeEvent = new TradeEvent(testTradeEventId, testClientId, TradeEventEntityType.ORDER, testEntityId, testAction, testDateTime, testDetails);
        
        assertEquals(testTradeEventId, tradeEvent.getTradeEventId());
        assertEquals(testClientId, tradeEvent.getClientId());
        assertEquals(TradeEventEntityType.ORDER, tradeEvent.getEntityType());
        assertEquals(testEntityId, tradeEvent.getEntityId());
        assertEquals(testAction, tradeEvent.getAction());
        assertEquals(testDateTime, tradeEvent.getOccurredAt());
        assertEquals(testDetails, tradeEvent.getDetails());
    }

    @Test
    @DisplayName("setTradeEventId and getTradeEventId")
    void testSetAndGetTradeEventId() {
        tradeEvent = new TradeEvent();
        tradeEvent.setTradeEventId(testTradeEventId);
        assertEquals(testTradeEventId, tradeEvent.getTradeEventId());
    }

    @Test
    @DisplayName("setClientId and getClientId")
    void testSetAndGetClientId() {
        tradeEvent = new TradeEvent();
        tradeEvent.setClientId(testClientId);
        assertEquals(testClientId, tradeEvent.getClientId());
    }

    @Test
    @DisplayName("setEntityType and getEntityType")
    void testSetAndGetEntityType() {
        tradeEvent = new TradeEvent();
        tradeEvent.setEntityType(TradeEventEntityType.FILL);
        assertEquals(TradeEventEntityType.FILL, tradeEvent.getEntityType());
    }

    @Test
    @DisplayName("setEntityId and getEntityId")
    void testSetAndGetEntityId() {
        tradeEvent = new TradeEvent();
        tradeEvent.setEntityId(testEntityId);
        assertEquals(testEntityId, tradeEvent.getEntityId());
    }

    @Test
    @DisplayName("setAction and getAction")
    void testSetAndGetAction() {
        tradeEvent = new TradeEvent();
        tradeEvent.setAction("UPDATE");
        assertEquals("UPDATE", tradeEvent.getAction());
    }

    @Test
    @DisplayName("setOccurredAt and getOccurredAt")
    void testSetAndGetOccurredAt() {
        tradeEvent = new TradeEvent();
        tradeEvent.setOccurredAt(testDateTime);
        assertEquals(testDateTime, tradeEvent.getOccurredAt());
    }

    @Test
    @DisplayName("setDetails and getDetails")
    void testSetAndGetDetails() {
        tradeEvent = new TradeEvent();
        tradeEvent.setDetails("Test event details");
        assertEquals("Test event details", tradeEvent.getDetails());
    }
}
