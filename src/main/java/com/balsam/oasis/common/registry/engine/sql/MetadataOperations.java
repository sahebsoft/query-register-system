package com.balsam.oasis.common.registry.engine.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.engine.sql.util.TypeConversionUtils;

/**
 * Handles metadata operations and raw data extraction from ResultSet.
 * Centralizes column resolution and metadata caching logic.
 */
public class MetadataOperations {

    /**
     * Extract raw column data from ResultSet into a map.
     * Uses cache for index-based access when available, otherwise uses metadata.
     * 
     * @param rs ResultSet to extract from
     * @param cache MetadataCache if available
     * @return Map with uppercase column names as keys
     * @throws SQLException if extraction fails
     */
    public static Map<String, Object> extractRawData(ResultSet rs, MetadataCache cache) throws SQLException {
        Map<String, Object> rawData = new HashMap<>();

        if (cache != null && cache.isInitialized()) {
            extractWithCache(rs, cache, rawData);
        } else {
            extractWithMetadata(rs, rawData);
        }

        return rawData;
    }

    /**
     * Extract value for a specific attribute using the most efficient method available.
     * 
     * @param rs ResultSet to extract from
     * @param attr AttributeDef defining what to extract
     * @param cache MetadataCache if available
     * @param rawData Pre-extracted raw data map
     * @return Extracted value or null
     * @throws SQLException if extraction fails
     */
    public static Object extractAttributeValue(ResultSet rs, AttributeDef<?> attr,
            MetadataCache cache, Map<String, Object> rawData) throws SQLException {
        
        // First try to get from raw data if available
        if (rawData != null && attr.aliasName() != null) {
            // Use uppercase for lookup (Oracle standard)
            String aliasName = attr.aliasName().toUpperCase();
            if (rawData.containsKey(aliasName)) {
                return rawData.get(aliasName);
            }
        }

        // If cache is available, use index-based access
        if (cache != null && cache.isInitialized()) {
            Integer columnIndex = resolveColumnIndex(attr, cache);
            if (columnIndex != null) {
                Integer sqlType = cache.getColumnType(columnIndex);
                return TypeConversionUtils.extractValue(rs, columnIndex, 
                    sqlType != null ? sqlType : Types.OTHER);
            }
        }

        // Fallback to name-based access
        if (attr.aliasName() != null) {
            return TypeConversionUtils.extractValue(rs, attr.aliasName());
        }

        return null;
    }

    /**
     * Resolve column index for an attribute using cache.
     */
    private static Integer resolveColumnIndex(AttributeDef<?> attr, MetadataCache cache) {
        // Try attribute name first
        Integer columnIndex = cache.getColumnIndexForAttribute(attr.name());
        
        // Fall back to alias name
        if (columnIndex == null && attr.aliasName() != null) {
            columnIndex = cache.getColumnIndex(attr.aliasName());
        }
        
        return columnIndex;
    }

    /**
     * Extract data using cached metadata (optimized path).
     */
    private static void extractWithCache(ResultSet rs, MetadataCache cache, 
            Map<String, Object> rawData) throws SQLException {
        String[] columnNames = cache.getColumnNames();
        String[] columnLabels = cache.getColumnLabels();

        for (int i = 0; i < columnNames.length; i++) {
            int columnIndex = i + 1; // JDBC uses 1-based indexing
            Integer sqlType = cache.getColumnType(columnIndex);
            Object value = TypeConversionUtils.extractValue(rs, columnIndex, 
                sqlType != null ? sqlType : Types.OTHER);

            // Store with uppercase keys (Oracle standard)
            String columnName = columnNames[i].toUpperCase();
            rawData.put(columnName, value);

            if (columnLabels != null && i < columnLabels.length) {
                String label = columnLabels[i].toUpperCase();
                if (!columnName.equals(label)) {
                    rawData.put(label, value);
                }
            }
        }
    }

    /**
     * Extract data using ResultSetMetaData (fallback path).
     */
    private static void extractWithMetadata(ResultSet rs, 
            Map<String, Object> rawData) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i).toUpperCase();
            String columnLabel = metaData.getColumnLabel(i).toUpperCase();
            Object value = TypeConversionUtils.extractValue(rs, i, metaData.getColumnType(i));

            rawData.put(columnName, value);
            if (!columnName.equals(columnLabel)) {
                rawData.put(columnLabel, value);
            }
        }
    }
}