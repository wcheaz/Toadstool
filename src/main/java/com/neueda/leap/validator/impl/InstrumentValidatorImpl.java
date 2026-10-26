package com.neueda.leap.validator.impl;

import com.neueda.leap.validator.InstrumentValidator;
import com.neueda.leap.validator.InstrumentValidationRequest;
import org.springframework.stereotype.Component;

/**
 * Default implementation of InstrumentValidator
 * Validates instrument symbol, name, and asset class
 */
@Component
public class InstrumentValidatorImpl implements InstrumentValidator {

    private static final String[] VALID_ASSET_CLASSES = {"EQUITY", "FX", "CRYPTO"};
    private static final int SYMBOL_MAX_LENGTH = 40;
    private static final int NAME_MAX_LENGTH = 160;

    @Override
    public void validate(InstrumentValidationRequest request) throws IllegalArgumentException {
        validateSymbol(request.getSymbol());
        validateName(request.getName());
        validateAssetClass(request.getAssetClass());
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (symbol.length() > SYMBOL_MAX_LENGTH) {
            throw new IllegalArgumentException("Symbol must be 1-" + SYMBOL_MAX_LENGTH + " characters");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Name must be 1-" + NAME_MAX_LENGTH + " characters");
        }
    }

    private void validateAssetClass(String assetClass) {
        if (assetClass == null || assetClass.isEmpty()) {
            throw new IllegalArgumentException("Asset class cannot be null or empty");
        }

        boolean isValid = false;
        for (String validClass : VALID_ASSET_CLASSES) {
            if (validClass.equals(assetClass)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException("Asset class must be one of: EQUITY, FX, CRYPTO");
        }
    }
}
