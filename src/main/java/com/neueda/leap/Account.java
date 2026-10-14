package com.neueda.leap;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;
import com.neueda.leap.enums.AccountStatus;

public class Account {

    private UUID accountId;
    private UUID clientId;
    private String accountName;
    private AccountStatus status;
    private OffsetDateTime openedAt;
    private BigDecimal balance;

    // Constructors
    public Account() {
    }

    public Account(UUID clientId, AccountStatus status) {
        this.clientId = clientId;
        this.status = status;
        this.balance = BigDecimal.ZERO;
    }

    public Account(UUID accountId, UUID clientId, AccountStatus status, OffsetDateTime openedAt) {
        this(accountId, clientId, status, openedAt, BigDecimal.ZERO);
    }

    public Account(UUID accountId, UUID clientId, AccountStatus status, OffsetDateTime openedAt, BigDecimal balance) {
        this.accountId = accountId;
        this.clientId = clientId;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
