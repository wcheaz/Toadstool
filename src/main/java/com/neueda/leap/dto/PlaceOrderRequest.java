package com.neueda.leap.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request DTO for placing an order
 */
public class PlaceOrderRequest {
    private UUID instrumentId;
    private String side;
    private String quantity;

    // Constructors
    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(UUID instrumentId, String side, String quantity) {
        this.instrumentId = instrumentId;
        this.side = side;
        this.quantity = quantity;
    }

    // Getters and Setters
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
}
