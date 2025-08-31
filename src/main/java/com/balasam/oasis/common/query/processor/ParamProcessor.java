package com.balasam.oasis.common.query.processor;

import com.balasam.oasis.common.query.core.execution.QueryContext;

/**
 * Processor specifically for parameter validation and transformation
 * Applied before query execution when no Row is available
 */
@FunctionalInterface
public interface ParamProcessor {
    /**
     * Process a parameter value with query context
     * @param value the parameter value
     * @param context the query context
     * @return the processed/validated value
     * @throws IllegalArgumentException if validation fails
     */
    Object process(Object value, QueryContext context);
    
    /**
     * Convenience static method for simple value-only processing
     */
    static ParamProcessor simple(java.util.function.Function<Object, Object> func) {
        return (value, context) -> func.apply(value);
    }
    
    /**
     * Convenience static method for validation
     */
    static ParamProcessor validator(java.util.function.Predicate<Object> validator, String errorMessage) {
        return (value, context) -> {
            if (!validator.test(value)) {
                throw new IllegalArgumentException(errorMessage);
            }
            return value;
        };
    }
    
    /**
     * Convenience static method for range validation
     */
    static ParamProcessor range(int min, int max) {
        return (value, context) -> {
            if (value instanceof Number) {
                int num = ((Number) value).intValue();
                if (num < min || num > max) {
                    throw new IllegalArgumentException(
                        String.format("Value must be between %d and %d", min, max));
                }
            }
            return value;
        };
    }
    
    /**
     * Convenience static method for string length validation
     */
    static ParamProcessor lengthBetween(int min, int max) {
        return (value, context) -> {
            if (value != null) {
                String str = value.toString();
                if (str.length() < min || str.length() > max) {
                    throw new IllegalArgumentException(
                        String.format("Length must be between %d and %d", min, max));
                }
            }
            return value;
        };
    }
    
    /**
     * Convenience static method for pattern validation
     */
    static ParamProcessor pattern(String pattern) {
        return (value, context) -> {
            if (value != null && !value.toString().matches(pattern)) {
                throw new IllegalArgumentException(
                    String.format("Value does not match pattern: %s", pattern));
            }
            return value;
        };
    }
}