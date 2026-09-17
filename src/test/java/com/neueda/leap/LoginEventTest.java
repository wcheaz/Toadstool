package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.neueda.leap.enums.LoginOutcome;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginEvent Tests")
class LoginEventTest {

    private LoginEvent loginEvent;
    private UUID testLoginEventId;
    private UUID testClientId;
    private OffsetDateTime testDateTime;
    private String testEmail = "test@example.com";
    private String testDetails = "Successful login from IP 192.168.1.1";

    @BeforeEach
    void setUp() {
        testLoginEventId = UUID.randomUUID();
        testClientId = UUID.randomUUID();
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty LoginEvent")
    void testDefaultConstructor() {
        loginEvent = new LoginEvent();
        assertNull(loginEvent.getLoginEventId());
        assertNull(loginEvent.getClientId());
        assertNull(loginEvent.getEmailAttempted());
        assertNull(loginEvent.getOutcome());
        assertNull(loginEvent.getOccurredAt());
        assertNull(loginEvent.getDetails());
    }

    @Test
    @DisplayName("Constructor with emailAttempted and outcome")
    void testConstructorWithEmailAndOutcome() {
        loginEvent = new LoginEvent(testEmail, LoginOutcome.SUCCESS);
        
        assertNull(loginEvent.getLoginEventId());
        assertNull(loginEvent.getClientId());
        assertEquals(testEmail, loginEvent.getEmailAttempted());
        assertEquals(LoginOutcome.SUCCESS, loginEvent.getOutcome());
        assertNull(loginEvent.getOccurredAt());
        assertNull(loginEvent.getDetails());
    }

    @Test
    @DisplayName("Constructor with clientId, emailAttempted, outcome, and details")
    void testConstructorWithClientIdAndDetails() {
        loginEvent = new LoginEvent(testClientId, testEmail, LoginOutcome.SUCCESS, testDetails);
        
        assertNull(loginEvent.getLoginEventId());
        assertEquals(testClientId, loginEvent.getClientId());
        assertEquals(testEmail, loginEvent.getEmailAttempted());
        assertEquals(LoginOutcome.SUCCESS, loginEvent.getOutcome());
        assertNull(loginEvent.getOccurredAt());
        assertEquals(testDetails, loginEvent.getDetails());
    }

    @Test
    @DisplayName("Constructor with all fields including ID and occurredAt")
    void testConstructorWithAllFields() {
        loginEvent = new LoginEvent(testLoginEventId, testClientId, testEmail, LoginOutcome.FAILURE, testDateTime, testDetails);
        
        assertEquals(testLoginEventId, loginEvent.getLoginEventId());
        assertEquals(testClientId, loginEvent.getClientId());
        assertEquals(testEmail, loginEvent.getEmailAttempted());
        assertEquals(LoginOutcome.FAILURE, loginEvent.getOutcome());
        assertEquals(testDateTime, loginEvent.getOccurredAt());
        assertEquals(testDetails, loginEvent.getDetails());
    }

    @Test
    @DisplayName("setLoginEventId and getLoginEventId")
    void testSetAndGetLoginEventId() {
        loginEvent = new LoginEvent();
        loginEvent.setLoginEventId(testLoginEventId);
        assertEquals(testLoginEventId, loginEvent.getLoginEventId());
    }

    @Test
    @DisplayName("setClientId and getClientId")
    void testSetAndGetClientId() {
        loginEvent = new LoginEvent();
        loginEvent.setClientId(testClientId);
        assertEquals(testClientId, loginEvent.getClientId());
    }

    @Test
    @DisplayName("setEmailAttempted and getEmailAttempted")
    void testSetAndGetEmailAttempted() {
        loginEvent = new LoginEvent();
        loginEvent.setEmailAttempted("newemail@example.com");
        assertEquals("newemail@example.com", loginEvent.getEmailAttempted());
    }

    @Test
    @DisplayName("setOutcome and getOutcome")
    void testSetAndGetOutcome() {
        loginEvent = new LoginEvent();
        loginEvent.setOutcome(LoginOutcome.SUCCESS);
        assertEquals(LoginOutcome.SUCCESS, loginEvent.getOutcome());
    }

    @Test
    @DisplayName("setOccurredAt and getOccurredAt")
    void testSetAndGetOccurredAt() {
        loginEvent = new LoginEvent();
        loginEvent.setOccurredAt(testDateTime);
        assertEquals(testDateTime, loginEvent.getOccurredAt());
    }

    @Test
    @DisplayName("setDetails and getDetails")
    void testSetAndGetDetails() {
        loginEvent = new LoginEvent();
        loginEvent.setDetails("Login from mobile app");
        assertEquals("Login from mobile app", loginEvent.getDetails());
    }
}
