package com.balsam.oasis.common.registry.core.execution;

import java.sql.PreparedStatement;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.balsam.oasis.common.registry.base.BaseExecutor;
import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.exception.QueryExecutionException;
import com.balsam.oasis.common.registry.query.QueryContext;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.balsam.oasis.common.registry.query.QuerySqlBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Builds and manages metadata caches for queries. This builder is responsible
 * for:
 * <ul>
 * <li>Using PreparedStatement.getMetaData() as primary approach (fastest)</li>
 * <li>Falling back to WHERE ROWNUM = 0 for Oracle when needed</li>
 * <li>Using WHERE 1=0 as final fallback</li>
 * <li>Building column index and type mappings</li>
 * <li>Pre-calculating attribute to column mappings</li>
 * <li>Caching metadata for reuse across query executions</li>
 * </ul>
 * 
 * <p>
 * Based on METADATA.md recommendations, this implementation prioritizes
 * PreparedStatement.getMetaData() to avoid query execution when possible.
 * </p>
 * 
 * @author Query Registration System
 * @since 1.0
 */
public class MetadataCacheBuilder {

    private static final Logger log = LoggerFactory.getLogger(MetadataCacheBuilder.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final QuerySqlBuilder sqlBuilder;

    public MetadataCacheBuilder(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.sqlBuilder = sqlBuilder;
    }

    /**
     * Build metadata cache for a query definition.
     * Uses PreparedStatement.getMetaData() as the primary approach,
     * with wrapped WHERE 1=0 as fallback.
     * 
     * @param definition The query definition
     * @return A populated metadata cache
     */
    public MetadataCache buildCache(QueryDefinition definition) {
        String queryName = definition.getName();
        log.debug("Building metadata cache for query: {}", queryName);

        try {
            // Create a context for metadata query
            QueryContext metadataContext = createMetadataContext(definition);

            // Build SQL
            BaseExecutor.SqlResult sqlResult = sqlBuilder.build(metadataContext);
            String sql = sqlResult.getSql();
            Map<String, Object> params = sqlResult.getParams();

            // Primary approach: PreparedStatement.getMetaData() without executing
            MetadataCache cache = tryPreparedStatementMetadata(sql, params, definition);
            if (cache != null && cache.isInitialized()) {
                log.info("Retrieved metadata using PreparedStatement.getMetaData() for query: {}", queryName);
                return cache;
            }

            // Fallback: Execute with wrapped WHERE 1=0
            cache = tryExecuteWithWhere10(sql, params, definition);
            if (cache != null && cache.isInitialized()) {
                log.info("Retrieved metadata using wrapped WHERE 1=0 for query: {}", queryName);
                return cache;
            }

            throw new QueryExecutionException(
                    "Failed to retrieve metadata for query '" + queryName + "': Both approaches failed");

        } catch (Exception e) {
            log.error("Failed to build metadata cache for query '{}': {}", queryName, e.getMessage(), e);
            throw new QueryExecutionException(
                    "Failed to build metadata cache for query '" + queryName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Try to get metadata using PreparedStatement.getMetaData() without executing
     * the query.
     * This is the fastest approach as recommended in METADATA.md.
     */
    private MetadataCache tryPreparedStatementMetadata(String sql, Map<String, Object> params,
            QueryDefinition definition) {
        try {
            return namedJdbcTemplate.execute(sql, params, (PreparedStatement ps) -> {
                try {
                    // Try to get metadata WITHOUT executing the query
                    ResultSetMetaData metaData = ps.getMetaData();

                    if (metaData != null) {
                        log.trace("PreparedStatement.getMetaData() returned metadata without parameters");
                        return buildCacheFromMetaData(metaData, definition);
                    }

                    // If null, try setting dummy parameters and get metadata again
                    log.trace("PreparedStatement.getMetaData() returned null, trying with dummy parameters");
                    ParameterMetaData pmd = ps.getParameterMetaData();
                    int paramCount = pmd.getParameterCount();

                    for (int i = 1; i <= paramCount; i++) {
                        try {
                            int paramType = pmd.getParameterType(i);
                            setDummyParameter(ps, i, paramType);
                        } catch (SQLException e) {
                            // If can't get type, use generic string
                            ps.setString(i, "DUMMY");
                        }
                    }

                    // Try again with parameters set
                    metaData = ps.getMetaData();
                    if (metaData != null) {
                        log.trace("PreparedStatement.getMetaData() returned metadata with dummy parameters");
                        return buildCacheFromMetaData(metaData, definition);
                    }

                } catch (Exception e) {
                    log.trace("Error in PreparedStatement.getMetaData() approach: {}", e.getMessage());
                }
                return null;
            });
        } catch (Exception e) {
            log.trace("PreparedStatement.getMetaData() approach failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback: Get metadata by executing wrapped query with WHERE 1=0.
     * This executes the query but returns no rows, working for all query types.
     * We wrap the original query to avoid syntax issues with complex queries.
     */
    private MetadataCache tryExecuteWithWhere10(String sql, Map<String, Object> params, QueryDefinition definition) {
        try {
            // Wrap the query to ensure WHERE 1=0 works with all query types
            // This handles UNION, GROUP BY, HAVING, etc. correctly
            String wrappedSql = "SELECT * FROM (" + sql + ") WHERE 1=0";

            return namedJdbcTemplate.execute(wrappedSql, params, (PreparedStatement ps) -> {
                try (ResultSet rs = ps.executeQuery()) {
                    return buildCacheFromResultSet(rs, definition);
                } catch (Exception e) {
                    log.trace("Error in WHERE 1=0 approach: {}", e.getMessage());
                    return null;
                }
            });
        } catch (Exception e) {
            log.trace("WHERE 1=0 approach failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build metadata cache from an active ResultSet.
     * This can be called during first query execution to build cache on-the-fly.
     * 
     * @param rs         The ResultSet to extract metadata from
     * @param definition The query definition
     * @return A populated metadata cache
     * @throws SQLException if metadata access fails
     */
    public MetadataCache buildCacheFromResultSet(ResultSet rs, QueryDefinition definition) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        return buildCacheFromMetaData(metaData, definition);
    }

    /**
     * Build cache from ResultSetMetaData without executing the query.
     * Used when PreparedStatement.getMetaData() returns metadata without execution.
     */
    private MetadataCache buildCacheFromMetaData(ResultSetMetaData metaData, QueryDefinition definition)
            throws SQLException {
        int columnCount = metaData.getColumnCount();

        // Create maps for the cache
        Map<String, Integer> columnIndexMap = new ConcurrentHashMap<>();
        Map<Integer, Integer> columnTypeMap = new ConcurrentHashMap<>();
        String[] columnNames = new String[columnCount];
        String[] columnLabels = new String[columnCount];

        // Build column metadata mappings
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String columnLabel = metaData.getColumnLabel(i);
            int columnType = metaData.getColumnType(i);

            // Store in arrays (0-based)
            columnNames[i - 1] = columnName;
            columnLabels[i - 1] = columnLabel;

            // Map column names to indexes (case variations for Oracle compatibility)
            columnIndexMap.put(columnName.toLowerCase(), i);
            columnIndexMap.put(columnLabel.toLowerCase(), i);
            columnIndexMap.put(columnName.toUpperCase(), i);
            columnIndexMap.put(columnLabel.toUpperCase(), i);
            columnIndexMap.put(columnName, i);
            columnIndexMap.put(columnLabel, i);

            // Map column index to SQL type
            columnTypeMap.put(i, columnType);

            log.trace("Cached column {}: name='{}', label='{}', type={}",
                    i, columnName, columnLabel, columnType);
        }

        // Pre-calculate attribute to column mappings
        Map<String, Integer> attributeToColumnIndex = new ConcurrentHashMap<>();
        Map<String, Integer> attributeToColumnType = new ConcurrentHashMap<>();

        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip transient attributes
            if (attr.isVirual()) {
                continue;
            }

            // Find column index for this attribute
            String columnKey = attr.getAliasName() != null ? attr.getAliasName() : attrName;
            Integer columnIndex = findColumnIndex(columnKey, columnIndexMap);

            if (columnIndex != null) {
                attributeToColumnIndex.put(attrName, columnIndex);
                attributeToColumnType.put(attrName, columnTypeMap.get(columnIndex));
                log.trace("Mapped attribute '{}' to column index {}", attrName, columnIndex);
            } else {
                log.warn("No column found for attribute '{}' with alias '{}'",
                        attrName, attr.getAliasName());
            }
        }

        // Build and return the cache
        MetadataCache cache = MetadataCache.builder()
                .queryName(definition.getName())
                .columnIndexMap(columnIndexMap)
                .columnTypeMap(columnTypeMap)
                .columnNames(columnNames)
                .columnLabels(columnLabels)
                .attributeToColumnIndex(attributeToColumnIndex)
                .attributeToColumnType(attributeToColumnType)
                .columnCount(columnCount)
                .createdAt(System.currentTimeMillis())
                .initialized(true)
                .build();

        log.info("Built metadata cache for query '{}': {} columns, {} attribute mappings",
                definition.getName(), columnCount, attributeToColumnIndex.size());

        return cache;
    }

    /**
     * Pre-warm metadata caches for multiple queries.
     * This can be called on application startup to avoid first-query latency.
     * 
     * @param definitions Query definitions to pre-warm
     * @return Map of query names to their metadata caches
     * @throws QueryExecutionException if any cache fails to build
     */
    public Map<String, MetadataCache> prewarmCaches(Iterable<QueryDefinition> definitions) {
        Map<String, MetadataCache> caches = new HashMap<>();
        Map<String, Exception> failures = new HashMap<>();

        for (QueryDefinition definition : definitions) {
            try {
                MetadataCache cache = buildCache(definition);
                if (cache.isInitialized()) {
                    caches.put(definition.getName(), cache);
                    log.debug("Pre-warmed metadata cache for query: {}", definition.getName());
                }
            } catch (Exception e) {
                log.error("Failed to pre-warm cache for query '{}': {}",
                        definition.getName(), e.getMessage(), e);
                failures.put(definition.getName(), e);
            }
        }

        if (!failures.isEmpty()) {
            String failedQueries = String.join(", ", failures.keySet());
            throw new QueryExecutionException(
                    String.format("Failed to pre-warm metadata caches for %d queries: %s",
                            failures.size(), failedQueries));
        }

        log.info("Successfully pre-warmed {} metadata caches", caches.size());
        return caches;
    }

    /**
     * Create a context for metadata-only query
     */
    private QueryContext createMetadataContext(QueryDefinition definition) {
        // Create params map with default values for all parameters
        Map<String, Object> params = new HashMap<>();

        // Add default values for all defined parameters to avoid missing parameter
        // errors
        for (Map.Entry<String, ParamDef<?>> entry : definition.getParams().entrySet()) {
            String paramName = entry.getKey();
            ParamDef<?> paramDef = entry.getValue();

            // Use the default value if defined, otherwise use a dummy value based on type
            if (paramDef.getDefaultValue() != null) {
                params.put(paramName, paramDef.getDefaultValue());
            } else {
                // Provide dummy values based on type
                Class<?> type = paramDef.getType();
                if (type == String.class) {
                    params.put(paramName, "dummy");
                } else if (type == Integer.class || type == int.class) {
                    params.put(paramName, 0);
                } else if (type == Long.class || type == long.class) {
                    params.put(paramName, 0L);
                } else if (type == Boolean.class || type == boolean.class) {
                    params.put(paramName, false);
                } else if (type == Double.class || type == double.class) {
                    params.put(paramName, 0.0);
                } else {
                    // For other types, use null and let the query handle it
                    params.put(paramName, null);
                }
            }
        }

        return QueryContext.builder()
                .definition(definition)
                .params(params)
                .filters(new HashMap<>())
                .sorts(new ArrayList<>())
                .build();
    }

    /**
     * Set a dummy parameter value based on SQL type.
     */
    private void setDummyParameter(PreparedStatement ps, int index, int sqlType) throws SQLException {
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                ps.setString(index, "DUMMY");
                break;
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.TINYINT:
                ps.setInt(index, 0);
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.REAL:
                ps.setDouble(index, 0.0);
                break;
            case Types.DATE:
                ps.setDate(index, new java.sql.Date(System.currentTimeMillis()));
                break;
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                ps.setTimestamp(index, new java.sql.Timestamp(System.currentTimeMillis()));
                break;
            case Types.BOOLEAN:
            case Types.BIT:
                ps.setBoolean(index, false);
                break;
            default:
                // For unknown types, try null
                ps.setObject(index, null);
        }
    }

    /**
     * Find column index for a column name, trying various case variations
     */
    private Integer findColumnIndex(String columnName, Map<String, Integer> columnIndexMap) {
        if (columnName == null)
            return null;

        // Try lowercase first (most common)
        Integer index = columnIndexMap.get(columnName.toLowerCase());
        if (index != null)
            return index;

        // Try uppercase (Oracle)
        index = columnIndexMap.get(columnName.toUpperCase());
        if (index != null)
            return index;

        // Try original case
        return columnIndexMap.get(columnName);
    }

    /**
     * Validate that a cache is still valid for the current ResultSet structure
     */
    public boolean isCacheValid(MetadataCache cache, ResultSetMetaData currentMetaData) throws SQLException {
        if (cache == null || !cache.isInitialized()) {
            return false;
        }

        // Check column count matches
        int currentColumnCount = currentMetaData.getColumnCount();
        if (cache.getColumnCount() != currentColumnCount) {
            log.debug("Cache invalid: column count changed from {} to {}",
                    cache.getColumnCount(), currentColumnCount);
            return false;
        }

        // Optionally validate column names match (more expensive)
        // This could be made configurable
        for (int i = 1; i <= currentColumnCount; i++) {
            String cachedName = cache.getColumnName(i);
            String currentName = currentMetaData.getColumnName(i);

            if (!equalsIgnoreCase(cachedName, currentName)) {
                log.debug("Cache invalid: column {} name changed from '{}' to '{}'",
                        i, cachedName, currentName);
                return false;
            }
        }

        return true;
    }

    private boolean equalsIgnoreCase(String s1, String s2) {
        if (s1 == s2)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equalsIgnoreCase(s2);
    }
}