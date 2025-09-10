package com.balsam.oasis.common.registry.engine.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

/**
 * Interface representing a single row of query results
 */
public interface QueryRow {

    // Basic accessors
    Object get(String attributeName);

    <T> T get(String attributeName, Class<T> type);

    <T> Optional<T> getOptional(String attributeName, Class<T> type);

    // Type-specific accessors with default implementations
    default String getString(String attributeName) {
        return get(attributeName, String.class);
    }

    default Integer getInteger(String attributeName) {
        return get(attributeName, Integer.class);
    }

    default Long getLong(String attributeName) {
        return get(attributeName, Long.class);
    }

    default Double getDouble(String attributeName) {
        return get(attributeName, Double.class);
    }

    default Float getFloat(String attributeName) {
        return get(attributeName, Float.class);
    }

    default BigDecimal getBigDecimal(String attributeName) {
        return get(attributeName, BigDecimal.class);
    }

    default LocalDate getLocalDate(String attributeName) {
        return get(attributeName, LocalDate.class);
    }

    default LocalDateTime getLocalDateTime(String attributeName) {
        return get(attributeName, LocalDateTime.class);
    }

    default Boolean getBoolean(String attributeName) {
        return get(attributeName, Boolean.class);
    }

    default byte[] getBytes(String attributeName) {
        return get(attributeName, byte[].class);
    }

    // With default values
    default String getString(String attributeName, String defaultValue) {
        String value = getString(attributeName);
        return value != null ? value : defaultValue;
    }

    default Integer getInteger(String attributeName, Integer defaultValue) {
        Integer value = getInteger(attributeName);
        return value != null ? value : defaultValue;
    }

    default Long getLong(String attributeName, Long defaultValue) {
        Long value = getLong(attributeName);
        return value != null ? value : defaultValue;
    }

    default Double getDouble(String attributeName, Double defaultValue) {
        Double value = getDouble(attributeName);
        return value != null ? value : defaultValue;
    }

    default Boolean getBoolean(String attributeName, Boolean defaultValue) {
        Boolean value = getBoolean(attributeName);
        return value != null ? value : defaultValue;
    }

    // Raw database access
    Object getRaw(String columnName);

    <T> T getRaw(String columnName, Class<T> type);

    // Setters for mutable operations
    void set(String attributeName, Object value);

    // Metadata
    boolean hasAttribute(String attributeName);

    boolean isNull(String attributeName);

    // Get all data
    Map<String, Object> toMap();

    // Context access
    QueryContext getContext();
}