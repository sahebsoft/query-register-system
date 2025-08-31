package com.balasam.oasis.common.query.processor;

/**
 * Converter for type conversions
 */
@FunctionalInterface
public interface Converter {
    /**
     * Convert a value to a target type
     * @param value the value to convert
     * @param targetType the target type
     * @return the converted value
     */
    Object convert(Object value, Class<?> targetType);
}