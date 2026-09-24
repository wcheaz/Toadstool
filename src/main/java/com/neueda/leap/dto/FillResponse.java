package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for fill data (executed trade)
 */
public class FillResponse {
    private UUID fillId;
    private UUID orderId;
    private String price;
    private String quantity;
    private String status;
    private OffsetDateTime executedAt;

    public FillResponse() {
    }

    public FillResponse(UUID fillId, UUID orderId, String price, String quantity, 
                        String status, OffsetDateTime executedAt) {
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
