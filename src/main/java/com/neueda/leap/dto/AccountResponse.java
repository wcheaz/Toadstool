package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Response DTO for account data
 */
public class AccountResponse {
    private UUID accountId;
    private UUID clientId;
    private String status;
    private OffsetDateTime openedAt;

    public AccountResponse() {
    }

    public AccountResponse(UUID accountId, UUID clientId, String status, OffsetDateTime openedAt) {
        this.accountId = accountId;
        this.clientId = clientId;
        this.status = status;
        this.openedAt = openedAt;
    }

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(OffsetDateTime openedAt) {
        this.openedAt = openedAt;
    }
}

/**
 * Request DTO for creating an account
 */
class CreateAccountRequest {
    private UUID clientId;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }
}
