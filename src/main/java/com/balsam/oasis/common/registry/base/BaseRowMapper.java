package com.balsam.oasis.common.registry.base;

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

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.execution.MetadataCache;
import com.balsam.oasis.common.registry.util.TypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

/**
 * Base row mapper that provides adaptive column access - uses cached indexes
 * when
 * available for performance, falls back to name-based access otherwise.
 * 
 * @param <T> The type of object this mapper produces (Row, SelectItem, etc.)
 */
public abstract class BaseRowMapper<T> implements RowMapper<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseRowMapper.class);

    /**
     * Extract raw column data from ResultSet into a map.
     * Uses cache for index-based access when available, otherwise uses metadata.
     */
    protected Map<String, Object> extractRawData(ResultSet rs, MetadataCache cache) throws SQLException {
        Map<String, Object> rawData = new HashMap<>();

        if (cache != null && cache.isInitialized()) {
            // Optimized path: use cached metadata
            String[] columnNames = cache.getColumnNames();
            String[] columnLabels = cache.getColumnLabels();

            for (int i = 0; i < columnNames.length; i++) {
                int columnIndex = i + 1; // JDBC uses 1-based indexing
                Integer sqlType = cache.getColumnType(columnIndex);
                Object value = extractValueByIndex(rs, columnIndex, sqlType != null ? sqlType : Types.OTHER);

                String columnName = columnNames[i].toLowerCase();
                rawData.put(columnName, value);

                if (columnLabels != null && i < columnLabels.length) {
                    String label = columnLabels[i].toLowerCase();
                    if (!columnName.equals(label)) {
                        rawData.put(label, value);
                    }
                }
            }
        } else {
            // Fallback path: use ResultSetMetaData
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i).toLowerCase();
                String columnLabel = metaData.getColumnLabel(i).toLowerCase();
                Object value = extractValueByIndex(rs, i, metaData.getColumnType(i));

                rawData.put(columnName, value);
                if (!columnName.equals(columnLabel)) {
                    rawData.put(columnLabel, value);
                }
            }
        }

        return rawData;
    }

    /**
     * Extract value for a specific attribute using the most efficient method
     * available.
     */
    protected Object extractAttributeValue(ResultSet rs, AttributeDef<?> attr,
            MetadataCache cache, Map<String, Object> rawData) throws SQLException {
        // First try to get from raw data if available
        if (rawData != null && attr.getAliasName() != null) {
            String aliasName = attr.getAliasName().toLowerCase();
            if (rawData.containsKey(aliasName)) {
                return rawData.get(aliasName);
            }
            // Try uppercase variant
            aliasName = attr.getAliasName().toUpperCase();
            if (rawData.containsKey(aliasName.toLowerCase())) {
                return rawData.get(aliasName.toLowerCase());
            }
        }

        // If cache is available, use index-based access
        if (cache != null && cache.isInitialized()) {
            Integer columnIndex = cache.getColumnIndexForAttribute(attr.getName());
            if (columnIndex == null && attr.getAliasName() != null) {
                columnIndex = cache.getColumnIndex(attr.getAliasName());
            }

            if (columnIndex != null) {
                Integer sqlType = cache.getColumnType(columnIndex);
                return extractValueByIndex(rs, columnIndex, sqlType != null ? sqlType : Types.OTHER);
            }
        }

        // Fallback to name-based access
        if (attr.getAliasName() != null) {
            try {
                return rs.getObject(attr.getAliasName());
            } catch (SQLException e) {
                log.trace("Column not found: {}", attr.getAliasName());
                return null;
            }
        }

        return null;
    }

    /**
     * Extract value by column index with proper type handling.
     */
    protected Object extractValueByIndex(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        // Check for NULL first
        Object value = rs.getObject(columnIndex);
        if (rs.wasNull()) {
            return null;
        }

        // Handle types based on SQL type for best performance
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
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
            case Types.TIMESTAMP_WITH_TIMEZONE:
                Timestamp timestamp = rs.getTimestamp(columnIndex);
                return timestamp != null ? timestamp.toLocalDateTime() : null;

            case Types.BLOB:
                Blob blob = rs.getBlob(columnIndex);
                if (blob != null) {
                    try {
                        return blob.getBytes(1, (int) blob.length());
                    } catch (SQLException e) {
                        log.warn("Error reading BLOB at column {}: {}", columnIndex, e.getMessage());
                        return blob;
                    }
                }
                return null;

            case Types.CLOB:
            case Types.NCLOB:
                Clob clob = rs.getClob(columnIndex);
                if (clob != null) {
                    try {
                        return clob.getSubString(1, (int) clob.length());
                    } catch (SQLException e) {
                        log.warn("Error reading CLOB at column {}: {}", columnIndex, e.getMessage());
                        return clob;
                    }
                }
                return null;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return rs.getBytes(columnIndex);

            case Types.ARRAY:
                return rs.getArray(columnIndex);

            default:
                // Fall back to generic getObject for unknown types
                return rs.getObject(columnIndex);
        }
    }

    /**
     * Convert value to target type using centralized TypeConverter.
     */
    protected Object convertToType(Object value, Class<?> targetType) {
        if (value == null || targetType == null) {
            return value;
        }

        // If already the target type, return as-is
        if (targetType.isInstance(value)) {
            return value;
        }

        try {
            return TypeConverter.convert(value, targetType);
        } catch (Exception e) {
            log.warn("Failed to convert value {} to type {}: {}",
                    value, targetType.getSimpleName(), e.getMessage());
            return value;
        }
    }

    /**
     * Get MetadataCache from context if available.
     * Subclasses can override to provide cache from different sources.
     */
    protected MetadataCache getCache(BaseContext<?> context) {
        // Default implementation returns null
        // Subclasses should override this to provide cache if available
        return null;
    }
}