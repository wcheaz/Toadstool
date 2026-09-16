package com.neueda.leap;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Fill {

    private UUID fillId;
    private UUID orderId;
    private BigDecimal price;
    private BigDecimal quantity;
    private FillStatus status;
    private OffsetDateTime executedAt;

    // Constructors
    public Fill() {
    }

    public Fill(UUID orderId, BigDecimal price, BigDecimal quantity, FillStatus status) {
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
    }

    public Fill(UUID fillId, UUID orderId, BigDecimal price, BigDecimal quantity, FillStatus status, OffsetDateTime executedAt) {
        this.fillId = fillId;
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
        this.executedAt = executedAt;
    }

    // Getters and Setters
    public UUID getFillId() {
        return fillId;
    }

    public void setFillId(UUID fillId) {
        this.fillId = fillId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public FillStatus getStatus() {
        return status;
    }

    public void setStatus(FillStatus status) {
        this.status = status;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }

    // Enums matching database constraints
    public enum FillStatus {
        Filled,
        Failed,
        Pending
    }
}
