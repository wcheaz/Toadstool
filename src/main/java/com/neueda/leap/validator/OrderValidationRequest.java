package com.neueda.leap.validator;

/**
 * Data class for order validation inputs
 */
public class OrderValidationRequest {
    private final String side;
    private final String quantity;

    public OrderValidationRequest(String side, String quantity) {
        this.side = side;
        this.quantity = quantity;
    }

    public String getSide() {
        return side;
    }

    public String getQuantity() {
        return quantity;
    }
}
