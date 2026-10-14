package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;

/**
 * Response DTO for account data
 */
public class AccountResponse {
    private UUID accountId;
    private UUID clientId;
    private String accountName;
    private String status;
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    private OffsetDateTime openedAt;
    private BigDecimal balance;

    public AccountResponse() {
    }

    public AccountResponse(UUID accountId, UUID clientId, String accountName, String status, OffsetDateTime openedAt, BigDecimal balance) {
        this.accountId = accountId;
        this.clientId = clientId;
        this.accountName = accountName;
        this.status = status;
        this.openedAt = openedAt;
        this.balance = balance;
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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
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
