package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.neueda.leap.enums.AccountStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Account Tests")
class AccountTest {

    private Account account;
    private UUID testAccountId;
    private UUID testClientId;
    private OffsetDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testClientId = UUID.randomUUID();
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty Account")
    void testDefaultConstructor() {
        account = new Account();
        assertNull(account.getAccountId());
        assertNull(account.getClientId());
        assertNull(account.getStatus());
        assertNull(account.getOpenedAt());
    }

    @Test
    @DisplayName("Constructor with clientId and status")
    void testConstructorWithClientIdAndStatus() {
        account = new Account(testClientId, AccountStatus.ACTIVE);
        
        assertNull(account.getAccountId());
        assertEquals(testClientId, account.getClientId());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertNull(account.getOpenedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including ID")
    void testConstructorWithAllFields() {
        account = new Account(testAccountId, testClientId, AccountStatus.ACTIVE, testDateTime);
        
        assertEquals(testAccountId, account.getAccountId());
        assertEquals(testClientId, account.getClientId());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(testDateTime, account.getOpenedAt());
    }

    @Test
    @DisplayName("setAccountId and getAccountId")
    void testSetAndGetAccountId() {
        account = new Account();
        account.setAccountId(testAccountId);
        assertEquals(testAccountId, account.getAccountId());
    }

    @Test
    @DisplayName("setClientId and getClientId")
    void testSetAndGetClientId() {
        account = new Account();
        account.setClientId(testClientId);
        assertEquals(testClientId, account.getClientId());
    }

    @Test
    @DisplayName("setStatus and getStatus")
    void testSetAndGetStatus() {
        account = new Account();
        account.setStatus(AccountStatus.SUSPENDED);
        assertEquals(AccountStatus.SUSPENDED, account.getStatus());
    }

    @Test
    @DisplayName("setOpenedAt and getOpenedAt")
    void testSetAndGetOpenedAt() {
        account = new Account();
        account.setOpenedAt(testDateTime);
        assertEquals(testDateTime, account.getOpenedAt());
    }
}
