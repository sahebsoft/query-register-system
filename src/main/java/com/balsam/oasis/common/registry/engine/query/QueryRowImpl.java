package com.balsam.oasis.common.registry.engine.query;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.google.common.collect.ImmutableMap;

/**
 * Default implementation of Row interface
 */
public class QueryRowImpl implements QueryRow {

    private final Map<String, Object> data;
    private final Map<String, Object> rawData;
    private final QueryContext context;

    public QueryRowImpl(Map<String, Object> data, Map<String, Object> rawData, QueryContext context) {
        this.data = new HashMap<>(data);
        this.rawData = new HashMap<>(rawData);
        this.context = context;
    }

    @Override
    public Object get(String attributeName) {
        return data.get(attributeName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String attributeName, Class<T> type) {
        Object value = data.get(attributeName);
        if (value == null) {
            return null;
        }
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return convertValue(value, type);
    }

    @Override
    public <T> Optional<T> getOptional(String attributeName, Class<T> type) {
        return Optional.ofNullable(get(attributeName, type));
    }

    // Type-specific accessors now use default methods from Row interface
    // Only override if custom behavior is needed

    @Override
    public Object getRaw(String columnName) {
        return rawData.get(columnName.toUpperCase());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRaw(String columnName, Class<T> type) {
        Object value = getRaw(columnName);
        if (value == null) {
            return null;
        }
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return convertValue(value, type);
    }

    @Override
    public void set(String attributeName, Object value) {
        data.put(attributeName, value);
    }

    @Override
    public boolean hasAttribute(String attributeName) {
        return data.containsKey(attributeName);
    }

    @Override
    public boolean isNull(String attributeName) {
        return data.get(attributeName) == null;
    }

    @Override
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

    @Override
    public QueryContext getContext() {
        return context;
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        // String conversions
        if (targetType == String.class) {
            return (T) value.toString();
        }

        // Numeric conversions
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number number) {
                return (T) Integer.valueOf(number.intValue());
            }
            return (T) Integer.valueOf(value.toString());
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number number) {
                return (T) Long.valueOf(number.longValue());
            }
            return (T) Long.valueOf(value.toString());
        }

        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number number) {
                return (T) Double.valueOf(number.doubleValue());
            }
            return (T) Double.valueOf(value.toString());
        }

        if (targetType == Float.class || targetType == float.class) {
            if (value instanceof Number number) {
                return (T) Float.valueOf(number.floatValue());
            }
            return (T) Float.valueOf(value.toString());
        }

        if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return (T) value;
            }
            if (value instanceof Number) {
                return (T) new BigDecimal(value.toString());
            }
            return (T) new BigDecimal(value.toString());
        }

        // Boolean conversion
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            }
            if (value instanceof Number number) {
                return (T) Boolean.valueOf(number.intValue() != 0);
            }
            return (T) Boolean.valueOf(value.toString());
        }

        // Date/Time conversions handled by specific converters

        // Default: try casting
        return (T) value;
    }
}