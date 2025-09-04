package com.balasam.oasis.common.query.core.execution;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.core.result.RowImpl;
import com.balasam.oasis.common.query.processor.AttributeFormatter;
import com.balasam.oasis.common.query.processor.Calculator;
import com.balasam.oasis.common.query.util.TypeConverter;

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

        // First pass: Map regular (non-transient) attributes from database
        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip transient attributes (they're calculated later)
            if (attr.isTransient()) {
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

        // Second pass: Apply formatters to regular attributes
        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip transient attributes
            if (attr.isTransient()) {
                continue;
            }

            // Apply formatter if present (converts to String)
            if (attr.hasFormatter()) {
                try {
                    Object currentValue = row.get(attrName);
                    if (currentValue != null) {
                        AttributeFormatter formatter = attr.getFormatter();
                        String formattedValue = formatter.format(currentValue);
                        row.set(attrName, formattedValue);
                    }
                } catch (Exception e) {
                    log.warn("Failed to format value for attribute {}: {}", attrName, e.getMessage());
                }
            }
        }

        // Third pass: Calculate transient attributes
        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Only process transient attributes
            if (!attr.isTransient()) {
                continue;
            }

            // Check security for transient attributes
            if (attr.isSecured() && context.getSecurityContext() != null) {
                Boolean allowed = attr.getSecurityRule().apply(context.getSecurityContext());
                if (!Boolean.TRUE.equals(allowed)) {
                    row.set(attrName, null);
                    continue;
                }
            }

            // Calculate the transient value
            if (attr.hasCalculator()) {
                try {
                    Calculator calculator = attr.getCalculator();
                    Object calculatedValue = calculator.calculate(row, context);

                    // Ensure type safety
                    if (calculatedValue != null && attr.getType() != null) {
                        calculatedValue = convertToType(calculatedValue, attr.getType());
                    }

                    row.set(attrName, calculatedValue);
                } catch (Exception e) {
                    log.warn("Failed to calculate transient attribute {}: {}", attrName, e.getMessage());
                    row.set(attrName, null);
                }
            }
        }
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

    /**
     * Converts a value to the specified target type.
     * Delegates to the centralized TypeConverter utility.
     *
     * @param value      The value to convert
     * @param targetType The target type class
     * @return The converted value or original value if conversion fails
     */
    private Object convertToType(Object value, Class<?> targetType) {
        try {
            return TypeConverter.convert(value, targetType);
        } catch (Exception e) {
            log.warn("Failed to convert value {} to type {}: {}",
                    value, targetType.getSimpleName(), e.getMessage());
            // Return original value if conversion fails
            return value;
        }
    }
}
