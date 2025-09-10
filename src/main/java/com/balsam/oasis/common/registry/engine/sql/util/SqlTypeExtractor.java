package com.balsam.oasis.common.registry.engine.sql.util;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting values from ResultSet based on SQL types.
 * Centralizes type-specific extraction logic with Oracle optimizations.
 */
public class SqlTypeExtractor {

    private static final Logger log = LoggerFactory.getLogger(SqlTypeExtractor.class);

    /**
     * Extract value by column index with proper type handling.
     * Uses SqlTypeMapper to determine the expected Java type for consistency.
     * 
     * @param rs the ResultSet to extract from
     * @param columnIndex 1-based column index
     * @param sqlType SQL type from java.sql.Types
     * @return the extracted value or null
     * @throws SQLException if extraction fails
     */
    public static Object extractValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        // Check for NULL first
        rs.getObject(columnIndex);
        if (rs.wasNull()) {
            return null;
        }

        // Use SqlTypeMapper to determine expected Java type
        Class<?> expectedType = SqlTypeMapper.sqlTypeToJavaClass(sqlType);

        // Handle types based on expected Java type for best performance
        if (expectedType == String.class) {
            return rs.getString(columnIndex);
        } else if (expectedType == Integer.class) {
            return rs.getInt(columnIndex);
        } else if (expectedType == Long.class) {
            return rs.getLong(columnIndex);
        } else if (expectedType == java.math.BigDecimal.class) {
            return rs.getBigDecimal(columnIndex);
        } else if (expectedType == Float.class) {
            return rs.getFloat(columnIndex);
        } else if (expectedType == Double.class) {
            return rs.getDouble(columnIndex);
        } else if (expectedType == Boolean.class) {
            return rs.getBoolean(columnIndex);
        } else if (expectedType == java.time.LocalDate.class) {
            Date date = rs.getDate(columnIndex);
            return date != null ? date.toLocalDate() : null;
        } else if (expectedType == java.time.LocalTime.class) {
            Time time = rs.getTime(columnIndex);
            return time != null ? time.toLocalTime() : null;
        } else if (expectedType == java.time.LocalDateTime.class) {
            Timestamp timestamp = rs.getTimestamp(columnIndex);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        } else if (expectedType == byte[].class) {
            return extractBinaryData(rs, columnIndex, sqlType);
        } else if (sqlType == Types.CLOB || sqlType == Types.NCLOB) {
            return extractClobData(rs, columnIndex);
        } else if (sqlType == Types.ARRAY) {
            return rs.getArray(columnIndex);
        } else {
            // Fall back to generic getObject for unknown types
            return rs.getObject(columnIndex);
        }
    }

    /**
     * Extract value by column name with automatic type detection.
     * 
     * @param rs the ResultSet to extract from
     * @param columnName the column name
     * @return the extracted value or null
     * @throws SQLException if extraction fails
     */
    public static Object extractValue(ResultSet rs, String columnName) throws SQLException {
        try {
            return rs.getObject(columnName);
        } catch (SQLException e) {
            log.trace("Column not found: {}", columnName);
            return null;
        }
    }

    /**
     * Extract binary data handling both BLOB and raw binary types.
     */
    private static byte[] extractBinaryData(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        if (sqlType == Types.BLOB) {
            Blob blob = rs.getBlob(columnIndex);
            if (blob != null) {
                try {
                    return blob.getBytes(1, (int) blob.length());
                } catch (SQLException e) {
                    log.warn("Error reading BLOB at column {}: {}", columnIndex, e.getMessage());
                    // Return the Blob object itself as fallback
                    return null;
                }
            }
            return null;
        } else {
            return rs.getBytes(columnIndex);
        }
    }

    /**
     * Extract CLOB data as String.
     */
    private static String extractClobData(ResultSet rs, int columnIndex) throws SQLException {
        Clob clob = rs.getClob(columnIndex);
        if (clob != null) {
            try {
                return clob.getSubString(1, (int) clob.length());
            } catch (SQLException e) {
                log.warn("Error reading CLOB at column {}: {}", columnIndex, e.getMessage());
                return null;
            }
        }
        return null;
    }
}