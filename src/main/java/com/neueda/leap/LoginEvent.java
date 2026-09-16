package com.neueda.leap;

import java.time.OffsetDateTime;
import java.util.UUID;

public class LoginEvent {

    private UUID loginEventId;
    private UUID clientId;
    private String emailAttempted;
    private LoginOutcome outcome;
    private OffsetDateTime occurredAt;
    private String details;

    // Constructors
    public LoginEvent() {
    }

    public LoginEvent(String emailAttempted, LoginOutcome outcome) {
        this.emailAttempted = emailAttempted;
        this.outcome = outcome;
    }

    public LoginEvent(UUID clientId, String emailAttempted, LoginOutcome outcome, String details) {
        this.clientId = clientId;
        this.emailAttempted = emailAttempted;
        this.outcome = outcome;
        this.details = details;
    }

    public LoginEvent(UUID loginEventId, UUID clientId, String emailAttempted, LoginOutcome outcome, OffsetDateTime occurredAt, String details) {
        this.loginEventId = loginEventId;
        this.clientId = clientId;
        this.emailAttempted = emailAttempted;
        this.outcome = outcome;
        this.occurredAt = occurredAt;
        this.details = details;
    }

    // Getters and Setters
    public UUID getLoginEventId() {
        return loginEventId;
    }

    public void setLoginEventId(UUID loginEventId) {
        this.loginEventId = loginEventId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getEmailAttempted() {
        return emailAttempted;
    }

    public void setEmailAttempted(String emailAttempted) {
        this.emailAttempted = emailAttempted;
    }

    public LoginOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(LoginOutcome outcome) {
        this.outcome = outcome;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    // Enums matching database constraints
    public enum LoginOutcome {
        SUCCESS,
        FAILURE
    }
}
