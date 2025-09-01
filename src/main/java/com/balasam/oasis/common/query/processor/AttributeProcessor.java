package com.balasam.oasis.common.query.processor;

import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.core.execution.QueryContext;

/**
 * Generic processor for value transformation and calculation
 * Supports simple value processing or complex calculations with row/context
 * access
 */
@FunctionalInterface
public interface AttributeProcessor<T> {
    /**
     * Process a value with optional row and context access
     * 
     * @param value   the current value (may be null for virtual attributes)
     * @param row     the current row (may be null for simple processing)
     * @param context the query context (may be null for simple processing)
     * @return the processed/calculated value
     */
    Object process(T value, Row row, QueryContext context);

    /**
     * Convenience static method for simple value-only processing
     */
    static <T> AttributeProcessor<T> simple(java.util.function.Function<T, Object> func) {
        return (value, row, context) -> func.apply(value);
    }

    /**
     * Convenience static method for masking values
     */
    static AttributeProcessor<String> mask(String maskValue) {
        return (value, row, context) -> maskValue;
    }

    /**
     * Convenience static method for formatting
     */
    static AttributeProcessor<String> formatter(java.util.function.Function<Object, String> formatter) {
        return (value, row, context) -> formatter.apply(value);
    }
}