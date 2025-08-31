package com.balasam.oasis.common.query.core.execution;

import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.core.result.RowImpl;
import com.balasam.oasis.common.query.exception.QueryExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
        
        // Map attributes based on definition
        for (Map.Entry<String, AttributeDef> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef attr = entry.getValue();
            
            // Skip virtual attributes (they're calculated later)
            if (attr.isVirtual()) {
                continue;
            }
            
            // Check security
            if (attr.isSecured() && context.getSecurityContext() != null) {
                if (!attr.getSecurityRule().apply(context.getSecurityContext())) {
                    // Attribute is restricted
                    if (attr.isMasked()) {
                        rowData.put(attrName, "***MASKED***");
                    } else {
                        rowData.put(attrName, null);
                    }
                    continue;
                }
            }
            
            // Get value from ResultSet
            Object rawValue = null;
            String dbColumn = attr.getDbColumn();
            
            if (dbColumn != null) {
                // Try different variations of the column name
                rawValue = rawData.get(dbColumn.toLowerCase());
                if (rawValue == null) {
                    rawValue = rawData.get(dbColumn.toUpperCase());
                }
                if (rawValue == null) {
                    rawValue = rawData.get(dbColumn);
                }
            }
            
            // Apply converter
            Object convertedValue = rawValue;
            if (attr.hasConverter()) {
                try {
                    convertedValue = attr.getConverter().apply(new Object[]{rawValue, attr.getType()});
                } catch (Exception e) {
                    log.warn("Failed to convert value for attribute {}: {}", attrName, e.getMessage());
                }
            } else if (rawValue != null && attr.getType() != null) {
                // Apply default type conversion
                convertedValue = convertToType(rawValue, attr.getType());
            }
            
            // Apply processor
            if (attr.hasProcessor()) {
                try {
                    convertedValue = attr.getProcessor().apply(convertedValue);
                } catch (Exception e) {
                    log.warn("Failed to process value for attribute {}: {}", attrName, e.getMessage());
                }
            }
            
            // Apply formatter
            if (attr.hasFormatter()) {
                try {
                    convertedValue = attr.getFormatter().apply(convertedValue);
                } catch (Exception e) {
                    log.warn("Failed to format value for attribute {}: {}", attrName, e.getMessage());
                }
            }
            
            // Set default value if null and default exists
            if (convertedValue == null && attr.getDefaultValue() != null) {
                convertedValue = attr.getDefaultValue();
            }
            
            rowData.put(attrName, convertedValue);
        }
        
        // Create Row instance
        Row row = new RowImpl(rowData, rawData, context);
        
        // Calculate calculated fields (non-virtual)
        for (Map.Entry<String, AttributeDef> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef attr = entry.getValue();
            
            if (attr.isCalculated() && !attr.isVirtual() && attr.hasCalculator()) {
                try {
                    Object currentValue = row.get(attrName);
                    Object calculatedValue = attr.getCalculator().apply(
                        new Object[]{currentValue, row, context});
                    row.set(attrName, calculatedValue);
                } catch (Exception e) {
                    log.warn("Failed to calculate value for attribute {}: {}", attrName, e.getMessage());
                }
            }
        }
        
        return row;
    }
    
    private Object extractValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        // Handle NULL values
        Object value = rs.getObject(columnIndex);
        if (rs.wasNull()) {
            return null;
        }
        
        // Extract based on SQL type for better type preservation
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return rs.getString(columnIndex);
                
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return rs.getInt(columnIndex);
                
            case Types.BIGINT:
                return rs.getLong(columnIndex);
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                return rs.getBigDecimal(columnIndex);
                
            case Types.FLOAT:
            case Types.REAL:
                return rs.getFloat(columnIndex);
                
            case Types.DOUBLE:
                return rs.getDouble(columnIndex);
                
            case Types.BOOLEAN:
            case Types.BIT:
                return rs.getBoolean(columnIndex);
                
            case Types.DATE:
                Date date = rs.getDate(columnIndex);
                return date != null ? date.toLocalDate() : null;
                
            case Types.TIME:
                Time time = rs.getTime(columnIndex);
                return time != null ? time.toLocalTime() : null;
                
            case Types.TIMESTAMP:
                Timestamp timestamp = rs.getTimestamp(columnIndex);
                return timestamp != null ? timestamp.toLocalDateTime() : null;
                
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return rs.getBytes(columnIndex);
                
            case Types.CLOB:
                Clob clob = rs.getClob(columnIndex);
                return clob != null ? clob.getSubString(1, (int) clob.length()) : null;
                
            case Types.BLOB:
                Blob blob = rs.getBlob(columnIndex);
                return blob != null ? blob.getBytes(1, (int) blob.length()) : null;
                
            default:
                return rs.getObject(columnIndex);
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
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return Integer.valueOf(value.toString());
            }
            
            if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                return Long.valueOf(value.toString());
            }
            
            if (targetType == Double.class || targetType == double.class) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.valueOf(value.toString());
            }
            
            if (targetType == Float.class || targetType == float.class) {
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
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
                if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
                String str = value.toString().toLowerCase();
                return "true".equals(str) || "1".equals(str) || "yes".equals(str) || "y".equals(str);
            }
            
            // Date/Time conversions
            if (targetType == LocalDate.class) {
                if (value instanceof LocalDate) {
                    return value;
                }
                if (value instanceof Date) {
                    return ((Date) value).toLocalDate();
                }
                if (value instanceof LocalDateTime) {
                    return ((LocalDateTime) value).toLocalDate();
                }
            }
            
            if (targetType == LocalDateTime.class) {
                if (value instanceof LocalDateTime) {
                    return value;
                }
                if (value instanceof Timestamp) {
                    return ((Timestamp) value).toLocalDateTime();
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to convert value {} to type {}: {}", 
                value, targetType.getSimpleName(), e.getMessage());
        }
        
        // Return original value if conversion fails
        return value;
    }
}