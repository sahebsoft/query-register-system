package com.balsam.oasis.common.query.processor;

/**
 * Validator for parameter and field values
 */
@FunctionalInterface
public interface Validator {
    /**
     * Validate a value
     * 
     * @param value the value to validate
     * @return true if valid, false otherwise
     */
    boolean validate(Object value);
}