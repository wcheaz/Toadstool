package com.neueda.leap.validator.impl;

import com.neueda.leap.validator.FillValidator;
import com.neueda.leap.validator.FillValidationRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Default implementation of FillValidator
 * Validates fill price, quantity, and status
 */
@Component
public class FillValidatorImpl implements FillValidator {

    private static final String[] VALID_STATUSES = {"Filled", "Failed", "Pending"};

    @Override
    public void validate(FillValidationRequest request) throws IllegalArgumentException {
        validatePrice(request.getPrice());
        validateQuantity(request.getQuantity());
        validateStatus(request.getStatus());
    }

    private void validatePrice(String price) {
        if (price == null || price.isEmpty()) {
            throw new IllegalArgumentException("Price cannot be null or empty");
        }

        try {
            BigDecimal p = new BigDecimal(price);
            if (p.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be >= 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Price must be a valid decimal number");
        }
    }

    private void validateQuantity(String quantity) {
        if (quantity == null || quantity.isEmpty()) {
            throw new IllegalArgumentException("Quantity cannot be null or empty");
        }

        try {
            new BigDecimal(quantity);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a valid decimal number");
        }
    }

    private void validateStatus(String status) {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        boolean isValid = false;
        for (String validStatus : VALID_STATUSES) {
            if (validStatus.equals(status)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException("Invalid fill status. Must be one of: Filled, Failed, Pending");
        }
    }
}
