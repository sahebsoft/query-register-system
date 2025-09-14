package com.balsam.oasis.common.registry.engine.query;

import java.util.HashMap;
import java.util.Map;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

/**
 * Simple container for query result row data.
 * Stores raw column data with minimal processing.
 */
public class QueryRow {

    private final Map<String, Object> data;
    private final QueryContext context;

    private QueryRow(Map<String, Object> data, QueryContext context) {
        this.data = data; // Use the data directly to avoid copying
        this.context = context;
    }

    /**
     * Create a QueryRow from clean attribute data
     */
    public static QueryRow create(Map<String, Object> attributeData, QueryContext context) {
        // Use attribute data directly - no SQL column names
        return new QueryRow(attributeData, context);
    }

    /**
     * Legacy method - kept for backward compatibility
     */
    @Deprecated
    public static QueryRow create(Map<String, Object> data, Map<String, Object> rawData, QueryContext context) {
        // Use the raw data directly for legacy compatibility
        return new QueryRow(rawData, context);
    }

    /**
     * Get value by attribute name
     */
    public Object get(String attributeName) {
        // With clean attribute-only data, use direct lookup
        return data.get(attributeName);
    }

    /**
     * Get raw value by column name - maintained for compatibility with calculators
     * that might still access SQL column names directly
     */
    public Object getRaw(String columnName) {
        // Try uppercase first (SQL standard)
        Object value = data.get(columnName.toUpperCase());
        if (value != null) {
            return value;
        }
        // Try exact match as fallback
        return data.get(columnName);
    }

    /**
     * Set a value (used for calculated attributes)
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Get all data as a map
     */
    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }

    /**
     * Get the query context
     */
    public QueryContext getContext() {
        return context;
    }

    /**
     * Check if an attribute exists
     */
    public boolean has(String attributeName) {
        return data.containsKey(attributeName);
    }

    /**
     * Get typed value with simple casting
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return null;
        }
    }

    // Convenience methods for common types
    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

    public Integer getInteger(String key) {
        return get(key, Integer.class);
    }

    public Long getLong(String key) {
        return get(key, Long.class);
    }

    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }
}