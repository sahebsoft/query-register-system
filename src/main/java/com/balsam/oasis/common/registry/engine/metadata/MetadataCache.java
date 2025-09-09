package com.balsam.oasis.common.registry.engine.metadata;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.balsam.oasis.common.registry.builder.QueryDefinition;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

/**
 * Caches ResultSet metadata to avoid expensive metadata lookups on each row.
 * This cache stores column indexes, types, and pre-calculated attribute
 * mappings
 * for optimal performance when using index-based ResultSet access.
 * 
 * <p>
 * Benefits:
 * </p>
 * <ul>
 * <li>Eliminates metadata round trips to database</li>
 * <li>Enables fast index-based column access</li>
 * <li>Reduces memory allocation per row</li>
 * <li>Particularly beneficial for Oracle databases</li>
 * </ul>
 * 
 * @author Query Registration System
 * @since 1.0
 */
@Getter
@Builder(toBuilder = true)
public class MetadataCache {

    /**
     * Maps column names (lowercase) to their 1-based indexes in the ResultSet
     */
    private final Map<String, Integer> columnIndexMap;

    /**
     * Maps column indexes to their SQL types (from java.sql.Types)
     */
    private final Map<Integer, Integer> columnTypeMap;

    /**
     * Array of original column names in ResultSet order
     */
    private final String[] columnNames;

    /**
     * Array of column labels in ResultSet order (may differ from names)
     */
    private final String[] columnLabels;

    /**
     * Pre-calculated mapping from attribute names to column indexes
     * This avoids the need to lookup column name and then index
     */
    private final Map<String, Integer> attributeToColumnIndex;

    /**
     * Pre-calculated mapping from attribute names to column SQL types
     */
    private final Map<String, Integer> attributeToColumnType;

    /**
     * Total number of columns in the ResultSet
     */
    private final int columnCount;

    /**
     * The query name this cache belongs to
     */
    private final String queryName;

    /**
     * Timestamp when this cache was created
     */
    private final long createdAt;

    /**
     * Thread-safe flag indicating if cache has been initialized
     */
    private volatile boolean initialized;

    /**
     * Column metadata for a single column
     */
    @Value
    public static class ColumnMetadata {
        int index; // 1-based column index
        String name; // Original column name
        String label; // Column label (alias)
        int sqlType; // SQL type from java.sql.Types
        String typeName; // Database-specific type name
        int precision; // Column precision
        int scale; // Column scale
    }

    /**
     * Get column index for an attribute name
     * 
     * @param attributeName The attribute name
     * @return The 1-based column index, or null if not found
     */
    public Integer getColumnIndexForAttribute(String attributeName) {
        return attributeToColumnIndex.get(attributeName);
    }

    /**
     * Get column SQL type for an attribute name
     * 
     * @param attributeName The attribute name
     * @return The SQL type constant, or null if not found
     */
    public Integer getColumnTypeForAttribute(String attributeName) {
        return attributeToColumnType.get(attributeName);
    }

    /**
     * Get column index by column name (case-insensitive)
     * 
     * @param columnName The column name
     * @return The 1-based column index, or null if not found
     */
    public Integer getColumnIndex(String columnName) {
        if (columnName == null)
            return null;

        // Try lowercase first (most common)
        Integer index = columnIndexMap.get(columnName.toLowerCase());
        if (index != null)
            return index;

        // Try uppercase
        index = columnIndexMap.get(columnName.toUpperCase());
        if (index != null)
            return index;

        // Try original case
        return columnIndexMap.get(columnName);
    }

    /**
     * Get SQL type for a column index
     * 
     * @param columnIndex The 1-based column index
     * @return The SQL type constant, or null if invalid index
     */
    public Integer getColumnType(int columnIndex) {
        return columnTypeMap.get(columnIndex);
    }

    /**
     * Get column name by index
     * 
     * @param columnIndex The 1-based column index
     * @return The column name, or null if invalid index
     */
    public String getColumnName(int columnIndex) {
        if (columnIndex < 1 || columnIndex > columnCount) {
            return null;
        }
        return columnNames[columnIndex - 1];
    }

    /**
     * Get column label by index
     * 
     * @param columnIndex The 1-based column index
     * @return The column label, or null if invalid index
     */
    public String getColumnLabel(int columnIndex) {
        if (columnIndex < 1 || columnIndex > columnCount) {
            return null;
        }
        return columnLabels[columnIndex - 1];
    }

    /**
     * Check if a column exists by name
     * 
     * @param columnName The column name to check
     * @return true if column exists, false otherwise
     */
    public boolean hasColumn(String columnName) {
        return getColumnIndex(columnName) != null;
    }

    /**
     * Check if cache is valid for a given column count
     * 
     * @param resultSetColumnCount The column count from ResultSet
     * @return true if cache matches the column count
     */
    public boolean isValidForColumnCount(int resultSetColumnCount) {
        return this.columnCount == resultSetColumnCount;
    }

    /**
     * Get age of cache in milliseconds
     * 
     * @return Age in milliseconds
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - createdAt;
    }

    /**
     * Create an empty cache (not initialized)
     */
    public static MetadataCache empty(String queryName) {
        return MetadataCache.builder()
                .queryName(queryName)
                .columnIndexMap(Collections.emptyMap())
                .columnTypeMap(Collections.emptyMap())
                .columnNames(new String[0])
                .columnLabels(new String[0])
                .attributeToColumnIndex(Collections.emptyMap())
                .attributeToColumnType(Collections.emptyMap())
                .columnCount(0)
                .createdAt(System.currentTimeMillis())
                .initialized(false)
                .build();
    }

    /**
     * Mark cache as initialized
     */
    public void markInitialized() {
        this.initialized = true;
    }

    /**
     * Static factory to build cache for a query definition
     * (Will be populated by MetadataCacheBuilder)
     */
    public static MetadataCacheBuilder builderForQuery(QueryDefinition definition) {
        return MetadataCache.builder()
                .queryName(definition.getName())
                .columnIndexMap(new ConcurrentHashMap<>())
                .columnTypeMap(new ConcurrentHashMap<>())
                .attributeToColumnIndex(new ConcurrentHashMap<>())
                .attributeToColumnType(new ConcurrentHashMap<>())
                .createdAt(System.currentTimeMillis());
    }
}