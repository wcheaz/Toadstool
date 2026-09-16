package com.neueda.leap;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Account {

    private UUID accountId;
    private UUID clientId;
    private AccountStatus status;
    private OffsetDateTime openedAt;

    // Constructors
    public Account() {
    }

    public Account(UUID clientId, AccountStatus status) {
        this.clientId = clientId;
        this.status = status;
    }

    public Account(UUID accountId, UUID clientId, AccountStatus status, OffsetDateTime openedAt) {
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

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(OffsetDateTime openedAt) {
        this.openedAt = openedAt;
    }

    // Enums matching database constraints
    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        CLOSED
    }
}
