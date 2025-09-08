package com.balsam.oasis.common.registry.domain.result;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of Row interface
 */
public class RowImpl implements Row {

    private final Map<String, Object> data;
    private final Map<String, Object> rawData;
    private final Object context;

    public RowImpl(Map<String, Object> data, Object context) {
        this.data = new HashMap<>(data);
        this.rawData = new HashMap<>();
        this.context = context;
    }

    public RowImpl(Map<String, Object> data, Map<String, Object> rawData, Object context) {
        this.data = new HashMap<>(data);
        this.rawData = new HashMap<>(rawData);
        this.context = context;
    }

    public RowImpl(Map<String, Object> data, ResultSet rs, Object context) throws SQLException {
        this.data = new HashMap<>(data);
        this.rawData = extractRawData(rs);
        this.context = context;
    }

    private Map<String, Object> extractRawData(ResultSet rs) throws SQLException {
        Map<String, Object> raw = new HashMap<>();
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = rs.getMetaData().getColumnName(i);
            raw.put(columnName.toLowerCase(), rs.getObject(i));
        }
        return raw;
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

    @Override
    public String getString(String attributeName) {
        return get(attributeName, String.class);
    }

    @Override
    public String getString(String attributeName, String defaultValue) {
        return ObjectUtils.defaultIfNull(getString(attributeName), defaultValue);
    }

    @Override
    public Integer getInteger(String attributeName) {
        return get(attributeName, Integer.class);
    }

    @Override
    public Integer getInteger(String attributeName, Integer defaultValue) {
        return ObjectUtils.defaultIfNull(getInteger(attributeName), defaultValue);
    }

    @Override
    public Long getLong(String attributeName) {
        return get(attributeName, Long.class);
    }

    @Override
    public Long getLong(String attributeName, Long defaultValue) {
        return ObjectUtils.defaultIfNull(getLong(attributeName), defaultValue);
    }

    @Override
    public Double getDouble(String attributeName) {
        return get(attributeName, Double.class);
    }

    @Override
    public Double getDouble(String attributeName, Double defaultValue) {
        return ObjectUtils.defaultIfNull(getDouble(attributeName), defaultValue);
    }

    @Override
    public Float getFloat(String attributeName) {
        return get(attributeName, Float.class);
    }

    @Override
    public BigDecimal getBigDecimal(String attributeName) {
        return get(attributeName, BigDecimal.class);
    }

    @Override
    public LocalDate getLocalDate(String attributeName) {
        return get(attributeName, LocalDate.class);
    }

    @Override
    public LocalDateTime getLocalDateTime(String attributeName) {
        return get(attributeName, LocalDateTime.class);
    }

    @Override
    public Boolean getBoolean(String attributeName) {
        return get(attributeName, Boolean.class);
    }

    @Override
    public Boolean getBoolean(String attributeName, Boolean defaultValue) {
        return ObjectUtils.defaultIfNull(getBoolean(attributeName), defaultValue);
    }

    @Override
    public byte[] getBytes(String attributeName) {
        return get(attributeName, byte[].class);
    }

    @Override
    public Object getRaw(String columnName) {
        return rawData.get(columnName.toLowerCase());
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
        // Filter out null values for ImmutableMap
        data.forEach((k, v) -> {
            if (v != null)
                result.put(k, v);
        });
        return ImmutableMap.copyOf(result);
    }

    @Override
    public Object getContext() {
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
            if (value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }
            return (T) Integer.valueOf(value.toString());
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return (T) Long.valueOf(((Number) value).longValue());
            }
            return (T) Long.valueOf(value.toString());
        }

        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            }
            return (T) Double.valueOf(value.toString());
        }

        if (targetType == Float.class || targetType == float.class) {
            if (value instanceof Number) {
                return (T) Float.valueOf(((Number) value).floatValue());
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
            if (value instanceof Number) {
                return (T) Boolean.valueOf(((Number) value).intValue() != 0);
            }
            return (T) Boolean.valueOf(value.toString());
        }

        // Date/Time conversions handled by specific converters

        // Default: try casting
        return (T) value;
    }
}