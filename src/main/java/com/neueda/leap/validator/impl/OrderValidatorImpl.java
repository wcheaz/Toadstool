package com.neueda.leap.validator.impl;

import com.neueda.leap.validator.OrderValidator;
import com.neueda.leap.validator.OrderValidationRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Default implementation of OrderValidator
 * Validates order side and quantity
 */
@Component
public class OrderValidatorImpl implements OrderValidator {

    private static final String[] VALID_SIDES = {"BUY", "SELL"};

    @Override
    public void validate(OrderValidationRequest request) throws IllegalArgumentException {
        validateSide(request.getSide());
        validateQuantity(request.getQuantity());
    }

    private void validateSide(String side) {
        if (side == null || side.isEmpty()) {
            throw new IllegalArgumentException("Order side cannot be null or empty");
        }

        boolean isValid = false;
        for (String validSide : VALID_SIDES) {
            if (validSide.equals(side)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException("Invalid order side. Must be BUY or SELL.");
        }
    }

    private void validateQuantity(String quantity) {
        if (quantity == null || quantity.isEmpty()) {
            throw new IllegalArgumentException("Quantity cannot be null or empty");
        }

        try {
            BigDecimal qty = new BigDecimal(quantity);
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a valid decimal number");
        }
    }
}
