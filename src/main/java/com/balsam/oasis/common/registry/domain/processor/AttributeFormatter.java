package com.balsam.oasis.common.registry.domain.processor;

/**
 * Functional interface for formatting attribute values to strings
 * Accepts typed value and returns formatted string representation
 * 
 * @param <T> the type of value to format
 */
@FunctionalInterface
public interface AttributeFormatter<T> {
    /**
     * Format a value to string representation
     * 
     * @param value the value to format (may be null)
     * @return the formatted string representation
     */
    String format(T value);

}