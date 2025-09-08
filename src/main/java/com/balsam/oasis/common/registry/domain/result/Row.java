package com.balsam.oasis.common.registry.domain.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Interface representing a single row of query results
 */
public interface Row {

    // Basic accessors
    Object get(String attributeName);

    <T> T get(String attributeName, Class<T> type);

    <T> Optional<T> getOptional(String attributeName, Class<T> type);

    // Type-specific accessors
    String getString(String attributeName);

    Integer getInteger(String attributeName);

    Long getLong(String attributeName);

    Double getDouble(String attributeName);

    Float getFloat(String attributeName);

    BigDecimal getBigDecimal(String attributeName);

    LocalDate getLocalDate(String attributeName);

    LocalDateTime getLocalDateTime(String attributeName);

    Boolean getBoolean(String attributeName);

    byte[] getBytes(String attributeName);

    // With default values
    String getString(String attributeName, String defaultValue);

    Integer getInteger(String attributeName, Integer defaultValue);

    Long getLong(String attributeName, Long defaultValue);

    Double getDouble(String attributeName, Double defaultValue);

    Boolean getBoolean(String attributeName, Boolean defaultValue);

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
    Object getContext();
}