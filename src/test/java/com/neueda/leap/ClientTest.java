package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.neueda.leap.enums.ClientStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Client Tests")
class ClientTest {

    private Client client;
    private UUID testClientId;
    private OffsetDateTime testDateTime;
    private String testEmail = "test@example.com";
    private String testDisplayName = "Test User";

    @BeforeEach
    void setUp() {
        testClientId = UUID.randomUUID();
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty Client")
    void testDefaultConstructor() {
        client = new Client();
        assertNull(client.getClientId());
        assertNull(client.getEmail());
        assertNull(client.getDisplayName());
        assertNull(client.getStatus());
        assertNull(client.getCreatedAt());
        assertNull(client.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with email, displayName, and status")
    void testConstructorWithBasicFields() {
        client = new Client(testEmail, testDisplayName, ClientStatus.ACTIVE);
        
        assertNull(client.getClientId());
        assertEquals(testEmail, client.getEmail());
        assertEquals(testDisplayName, client.getDisplayName());
        assertEquals(ClientStatus.ACTIVE, client.getStatus());
        assertNull(client.getCreatedAt());
        assertNull(client.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including ID")
    void testConstructorWithAllFields() {
        client = new Client(testClientId, testEmail, testDisplayName, ClientStatus.ACTIVE, testDateTime, testDateTime);
        
        assertEquals(testClientId, client.getClientId());
        assertEquals(testEmail, client.getEmail());
        assertEquals(testDisplayName, client.getDisplayName());
        assertEquals(ClientStatus.ACTIVE, client.getStatus());
        assertEquals(testDateTime, client.getCreatedAt());
        assertEquals(testDateTime, client.getUpdatedAt());
    }

    @Test
    @DisplayName("setClientId and getClientId")
    void testSetAndGetClientId() {
        client = new Client();
        client.setClientId(testClientId);
        assertEquals(testClientId, client.getClientId());
    }

    @Test
    @DisplayName("setEmail and getEmail")
    void testSetAndGetEmail() {
        client = new Client();
        client.setEmail("newemail@example.com");
        assertEquals("newemail@example.com", client.getEmail());
    }

    @Test
    @DisplayName("setDisplayName and getDisplayName")
    void testSetAndGetDisplayName() {
        client = new Client();
        client.setDisplayName("New Display Name");
        assertEquals("New Display Name", client.getDisplayName());
    }

    @Test
    @DisplayName("setStatus and getStatus")
    void testSetAndGetStatus() {
        client = new Client();
        client.setStatus(ClientStatus.SUSPENDED);
        assertEquals(ClientStatus.SUSPENDED, client.getStatus());
    }

    @Test
    @DisplayName("setCreatedAt and getCreatedAt")
    void testSetAndGetCreatedAt() {
        client = new Client();
        client.setCreatedAt(testDateTime);
        assertEquals(testDateTime, client.getCreatedAt());
    }

    @Test
    @DisplayName("setUpdatedAt and getUpdatedAt")
    void testSetAndGetUpdatedAt() {
        client = new Client();
        client.setUpdatedAt(testDateTime);
        assertEquals(testDateTime, client.getUpdatedAt());
    }
}
