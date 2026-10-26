package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;

/**
 * Response DTO for fill data (executed trade)
 */
public class FillResponse {
    private UUID fillId;
    private UUID orderId;
    private String price;
    private String quantity;
    private String status;
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    private OffsetDateTime executedAt;
    private String fee = "0";

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

    public FillResponse(UUID fillId, UUID orderId, String price, String quantity,
                        String status, OffsetDateTime executedAt, String fee) {
        this(fillId, orderId, price, quantity, status, executedAt);
        this.fee = fee;
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

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }
}
