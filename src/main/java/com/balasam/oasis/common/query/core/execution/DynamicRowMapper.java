package com.balasam.oasis.common.query.core.execution;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.core.result.RowImpl;
import com.balasam.oasis.common.query.processor.AttributeProcessor;

/**
 * Dynamic row mapper that maps ResultSet to Row based on QueryDefinition
 */
public class DynamicRowMapper {

    private static final Logger log = LoggerFactory.getLogger(DynamicRowMapper.class);

    public Row mapRow(ResultSet rs, int rowNum, QueryContext context) throws SQLException {
        QueryDefinition definition = context.getDefinition();
        Map<String, Object> rowData = new HashMap<>();
        Map<String, Object> rawData = new HashMap<>();

        // Extract raw data from ResultSet
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i).toLowerCase();
            String columnLabel = metaData.getColumnLabel(i).toLowerCase();
            Object value = extractValue(rs, i, metaData.getColumnType(i));

            rawData.put(columnName, value);
            if (!columnName.equals(columnLabel)) {
                rawData.put(columnLabel, value);
            }
        }

        // First pass: Map non-virtual attributes
        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip virtual attributes (they're calculated later)
            if (attr.isVirtual()) {
                continue;
            }

            // Check security
            if (attr.isSecured() && context.getSecurityContext() != null) {
                Boolean allowed = attr.getSecurityRule().apply(context.getSecurityContext());
                if (!Boolean.TRUE.equals(allowed)) {
                    // Attribute is restricted - set to null or apply processor for masking
                    rowData.put(attrName, null);
                    continue;
                }
            }

            // Get value from ResultSet
            Object rawValue = null;
            String aliasName = attr.getAliasName();

            if (aliasName != null) {
                // Try different variations of the column name
                rawValue = rawData.get(aliasName.toLowerCase());
                if (rawValue == null) {
                    rawValue = rawData.get(aliasName.toUpperCase());
                }
                if (rawValue == null) {
                    rawValue = rawData.get(aliasName);
                }
            }

            // Apply automatic type conversion based on attr.getType()
            Object convertedValue = rawValue;
            if (rawValue != null && attr.getType() != null) {
                convertedValue = convertToType(rawValue, attr.getType());
            }

            rowData.put(attrName, convertedValue);
        }

        // Create Row instance
        Row row = new RowImpl(rowData, rawData, context);

        // Second pass: Apply processors to non-virtual attributes (now that we have the
        // Row)
        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip virtual attributes
            if (attr.isVirtual()) {
                continue;
            }

            // Apply processor with full context (value, row, context)
            if (attr.hasProcessor()) {
                try {
                    Object currentValue = row.get(attrName);
                    AttributeProcessor<Object> processor = (AttributeProcessor<Object>) attr.getProcessor();
                    Object processedValue = processor.process(currentValue, row, context);
                    row.set(attrName, processedValue);
                } catch (Exception e) {
                    log.warn("Failed to process value for attribute {}: {}", attrName, e.getMessage());
                }
            }
        }

        // Calculate calculated fields (non-virtual) - now handled by processor
        // Virtual fields are calculated separately by VirtualAttributeProcessor
        return row;
    }

    private Object extractValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        // Handle NULL values
        rs.getObject(columnIndex);
        if (rs.wasNull()) {
            return null;
        }

        // Extract based on SQL type for better type preservation
        switch (sqlType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR -> {
                return rs.getString(columnIndex);
            }
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> {
                return rs.getInt(columnIndex);
            }
            case Types.BIGINT -> {
                return rs.getLong(columnIndex);
            }

            case Types.DECIMAL, Types.NUMERIC -> {
                return rs.getBigDecimal(columnIndex);
            }
            case Types.FLOAT, Types.REAL -> {
                return rs.getFloat(columnIndex);
            }
            case Types.DOUBLE -> {
                return rs.getDouble(columnIndex);
            }

            case Types.BOOLEAN, Types.BIT -> {
                return rs.getBoolean(columnIndex);
            }

            case Types.DATE -> {
                Date date = rs.getDate(columnIndex);
                return date != null ? date.toLocalDate() : null;
            }
            case Types.TIME -> {
                Time time = rs.getTime(columnIndex);
                return time != null ? time.toLocalTime() : null;
            }

            case Types.TIMESTAMP -> {
                Timestamp timestamp = rs.getTimestamp(columnIndex);
                return timestamp != null ? timestamp.toLocalDateTime() : null;
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> {
                return rs.getBytes(columnIndex);
            }

            case Types.CLOB -> {
                Clob clob = rs.getClob(columnIndex);
                return clob != null ? clob.getSubString(1, (int) clob.length()) : null;
            }

            case Types.BLOB -> {
                Blob blob = rs.getBlob(columnIndex);
                return blob != null ? blob.getBytes(1, (int) blob.length()) : null;
            }
            default -> {
                return rs.getObject(columnIndex);
            }
        }
    }

    private Object convertToType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // If already the correct type
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        try {
            // String conversions
            if (targetType == String.class) {
                return value.toString();
            }

            // Numeric conversions
            if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number numberValue) {
                    return numberValue.intValue();
                }
                return Integer.valueOf(value.toString());
            }

            if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number number) {
                    return number.longValue();
                }
                return Long.valueOf(value.toString());
            }

            if (targetType == Double.class || targetType == double.class) {
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
                return Double.valueOf(value.toString());
            }

            if (targetType == Float.class || targetType == float.class) {
                if (value instanceof Number number) {
                    return number.floatValue();
                }
                return Float.valueOf(value.toString());
            }

            if (targetType == BigDecimal.class) {
                if (value instanceof BigDecimal) {
                    return value;
                }
                if (value instanceof Number) {
                    return new BigDecimal(value.toString());
                }
                return new BigDecimal(value.toString());
            }

            // Boolean conversion
            if (targetType == Boolean.class || targetType == boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                }
                if (value instanceof Number number) {
                    return number.intValue() != 0;
                }
                String str = value.toString().toLowerCase();
                return "true".equals(str) || "1".equals(str) || "yes".equals(str) || "y".equals(str);
            }

            // Date/Time conversions
            if (targetType == LocalDate.class) {
                if (value instanceof LocalDate) {
                    return value;
                }
                if (value instanceof Date date) {
                    return date.toLocalDate();
                }
                if (value instanceof LocalDateTime localDateTime) {
                    return localDateTime.toLocalDate();
                }
            }

            if (targetType == LocalDateTime.class) {
                if (value instanceof LocalDateTime) {
                    return value;
                }
                if (value instanceof Timestamp timestamp) {
                    return timestamp.toLocalDateTime();
                }
            }

        } catch (NumberFormatException e) {
            log.warn("Failed to convert value {} to type {}: {}",
                    value, targetType.getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to convert value {} to type {}: {}",
                    value, targetType.getSimpleName(), e.getMessage());
        }

        // Return original value if conversion fails
        return value;
    }
}
