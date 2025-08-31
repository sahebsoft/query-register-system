package com.balasam.oasis.common.query.processor;

/**
 * Formatter for value presentation
 */
@FunctionalInterface
public interface Formatter {
    /**
     * Format a value for presentation
     * @param value the value to format
     * @return the formatted string representation
     */
    String format(Object value);
}