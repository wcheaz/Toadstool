package com.neueda.leap.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for placing an order
 */
public class PlaceOrderRequest {
    private UUID instrumentId;
    private String side;
    private String quantity;
    /** MARKET (default) or LIMIT. */
    private String orderType;
    /** Required for LIMIT orders: worst acceptable execution price. */
    private BigDecimal limitPrice;

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

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }
}
