package com.neueda.leap;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Holdings represents the current quantity of an instrument held by an account.
 * This is a denormalized, mutable cache of position data derived from filled trades.
 * While the data can be reconstructed from trade_events, caching it here avoids
 * expensive computation for every portfolio read.
 */
public class Holdings {

    private UUID holdingId;
    private UUID accountId;
    private UUID instrumentId;
    private BigDecimal quantity;
    private OffsetDateTime updatedAt;

    // Constructors
    public Holdings() {
    }

    public Holdings(UUID accountId, UUID instrumentId, BigDecimal quantity) {
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
    }

    public Holdings(UUID accountId, UUID instrumentId, BigDecimal quantity, OffsetDateTime updatedAt) {
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    public Holdings(UUID holdingId, UUID accountId, UUID instrumentId, BigDecimal quantity, OffsetDateTime updatedAt) {
        this.holdingId = holdingId;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(UUID holdingId) {
        this.holdingId = holdingId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(UUID instrumentId) {
        this.instrumentId = instrumentId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
