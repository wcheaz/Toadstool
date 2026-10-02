package com.neueda.leap.validator;

/**
 * Data class for fill validation inputs
 */
public class FillValidationRequest {
    private final String price;
    private final String quantity;
    private final String status;

    public FillValidationRequest(String price, String quantity, String status) {
        this.price = price;
        this.quantity = quantity;
        this.status = status;
    }

    public String getPrice() {
        return price;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }
}
