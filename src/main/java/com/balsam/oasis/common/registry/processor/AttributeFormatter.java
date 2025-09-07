package com.balsam.oasis.common.registry.processor;

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

    /**
     * Convenience method for null-safe formatting
     */
    static <T> AttributeFormatter<T> nullSafe(AttributeFormatter<T> formatter, String nullValue) {
        return value -> value == null ? nullValue : formatter.format(value);
    }

    /**
     * Common formatter for currency values
     */
    static AttributeFormatter<Number> currency(String symbol) {
        return value -> value == null ? null : String.format("%s%,.2f", symbol, value.doubleValue());
    }

    /**
     * Common formatter for percentage values
     */
    static AttributeFormatter<Number> percentage() {
        return value -> value == null ? null : String.format("%.1f%%", value.doubleValue());
    }

    /**
     * Common formatter for masking sensitive data
     */
    static <T> AttributeFormatter<T> mask(String maskValue) {
        return value -> maskValue;
    }

    /**
     * Common formatter for truncating strings
     */
    static AttributeFormatter<String> truncate(int maxLength) {
        return value -> {
            if (value == null)
                return null;
            if (value.isEmpty())
                return value;
            if (maxLength <= 0)
                return "...";
            return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
        };
    }
}