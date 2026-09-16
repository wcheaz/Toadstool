package com.neueda.leap;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Order {

    private UUID orderId;
    private UUID accountId;
    private UUID instrumentId;
    private OrderSide side;
    private BigDecimal quantity;
    private String idempotencyKey;
    private OffsetDateTime submittedAt;

    // Constructors
    public Order() {
    }

    public Order(UUID accountId, UUID instrumentId, OrderSide side, BigDecimal quantity, String idempotencyKey) {
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.side = side;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
    }

    public Order(UUID orderId, UUID accountId, UUID instrumentId, OrderSide side, BigDecimal quantity, String idempotencyKey, OffsetDateTime submittedAt) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.side = side;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
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

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    // Enums matching database constraints
    public enum OrderSide {
        BUY,
        SELL
    }
}
