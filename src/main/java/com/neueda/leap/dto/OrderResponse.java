package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for order data
 */
public class OrderResponse {
    private UUID orderId;
    private UUID accountId;
    private UUID instrumentId;
    private String side;
    private String quantity;
    private String idempotencyKey;
    private String status;
    private OffsetDateTime submittedAt;

    public OrderResponse() {
    }

    public OrderResponse(UUID orderId, UUID accountId, UUID instrumentId, String side, 
                         String quantity, String idempotencyKey, String status, OffsetDateTime submittedAt) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.side = side;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
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

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
