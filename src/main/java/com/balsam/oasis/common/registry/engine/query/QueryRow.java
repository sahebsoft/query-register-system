package com.balsam.oasis.common.registry.engine.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.sql.util.TypeConverter;
import com.google.common.collect.ImmutableMap;

/**
 * Represents a single row of query results.
 * This is now a concrete class with static factory methods.
 */
public class QueryRow {

    private final Map<String, Object> data;
    private final Map<String, Object> rawData;
    private final QueryContext context;

    /**
     * Private constructor - use factory methods
     */
    private QueryRow(Map<String, Object> data, Map<String, Object> rawData, QueryContext context) {
        this.data = new HashMap<>(data);
        this.rawData = rawData != null ? new HashMap<>(rawData) : new HashMap<>();
        this.context = context;
    }

    /**
     * Factory method to create a QueryRow
     */
    public static QueryRow create(Map<String, Object> data, Map<String, Object> rawData, QueryContext context) {
        return new QueryRow(data, rawData, context);
    }

    /**
     * Factory method for simple cases without raw data
     */
    public static QueryRow create(Map<String, Object> data, QueryContext context) {
        return new QueryRow(data, null, context);
    }

    // Basic accessors
    public Object get(String attributeName) {
        return data.get(attributeName);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String attributeName, Class<T> type) {
        Object value = data.get(attributeName);
        if (value == null) {
            return null;
        }
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return TypeConverter.convert(value, type);
    }

    public <T> Optional<T> getOptional(String attributeName, Class<T> type) {
        return Optional.ofNullable(get(attributeName, type));
    }

    // Type-specific accessors
    public String getString(String attributeName) {
        return get(attributeName, String.class);
    }

    public Integer getInteger(String attributeName) {
        return get(attributeName, Integer.class);
    }

    public Long getLong(String attributeName) {
        return get(attributeName, Long.class);
    }

    public Double getDouble(String attributeName) {
        return get(attributeName, Double.class);
    }

    public Float getFloat(String attributeName) {
        return get(attributeName, Float.class);
    }

    public BigDecimal getBigDecimal(String attributeName) {
        return get(attributeName, BigDecimal.class);
    }

    public LocalDate getLocalDate(String attributeName) {
        return get(attributeName, LocalDate.class);
    }

    public LocalDateTime getLocalDateTime(String attributeName) {
        return get(attributeName, LocalDateTime.class);
    }

    public Boolean getBoolean(String attributeName) {
        return get(attributeName, Boolean.class);
    }

    public byte[] getBytes(String attributeName) {
        return get(attributeName, byte[].class);
    }

    // With default values
    public String getString(String attributeName, String defaultValue) {
        String value = getString(attributeName);
        return value != null ? value : defaultValue;
    }

    public Integer getInteger(String attributeName, Integer defaultValue) {
        Integer value = getInteger(attributeName);
        return value != null ? value : defaultValue;
    }

    public Long getLong(String attributeName, Long defaultValue) {
        Long value = getLong(attributeName);
        return value != null ? value : defaultValue;
    }

    public Double getDouble(String attributeName, Double defaultValue) {
        Double value = getDouble(attributeName);
        return value != null ? value : defaultValue;
    }

    public Boolean getBoolean(String attributeName, Boolean defaultValue) {
        Boolean value = getBoolean(attributeName);
        return value != null ? value : defaultValue;
    }

    // Raw database access
    public Object getRaw(String columnName) {
        return rawData.get(columnName.toUpperCase());
    }

    @SuppressWarnings("unchecked")
    public <T> T getRaw(String columnName, Class<T> type) {
        Object value = getRaw(columnName);
        if (value == null) {
            return null;
        }
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return TypeConverter.convert(value, type);
    }

    // Setters for mutable operations
    public void set(String attributeName, Object value) {
        data.put(attributeName, value);
    }

    // Metadata
    public boolean hasAttribute(String attributeName) {
        return data.containsKey(attributeName);
    }

    public boolean isNull(String attributeName) {
        return data.get(attributeName) == null;
    }

    // Get all data
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        // Filter based on field selection and null values
        data.forEach((k, v) -> {
            // Only include if field is selected and value is not null
            if (v != null && (context == null || context.isFieldSelected(k))) {
                result.put(k, v);
            }
        });
        return ImmutableMap.copyOf(result);
    }

    // Context access
    public QueryContext getContext() {
        return context;
    }
}