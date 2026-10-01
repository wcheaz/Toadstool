package com.neueda.leap.validator;

/**
 * Generic validator interface for domain objects
 */
public interface Validator<T> {
    /**
     * Validate the given object
     * @param value The value to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validate(T value) throws IllegalArgumentException;
}
