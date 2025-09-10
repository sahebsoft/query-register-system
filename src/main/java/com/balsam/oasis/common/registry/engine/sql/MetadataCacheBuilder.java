package com.balsam.oasis.common.registry.engine.sql;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.SqlResult;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QuerySqlBuilder;
import com.balsam.oasis.common.registry.engine.sql.util.SqlTypeMapper;
import com.balsam.oasis.common.registry.exception.QueryExecutionException;

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

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final QuerySqlBuilder sqlBuilder;

    public MetadataCacheBuilder(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder) {
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
            SqlResult sqlResult = sqlBuilder.build(metadataContext);
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
                            SqlTypeMapper.setDummyParameter(ps, i, paramType);
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
                    ResultSetMetaData metaData = rs.getMetaData();
                    return buildCacheFromMetaData(metaData, definition);
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
     * Build cache from ResultSetMetaData without executing the query.
     * Used when PreparedStatement.getMetaData() returns metadata without execution.
     */
    private MetadataCache buildCacheFromMetaData(ResultSetMetaData metaData, QueryDefinition definition)
            throws SQLException {
        int columnCount = metaData.getColumnCount();

        // Create maps for the cache
        Map<String, Integer> columnIndexMap = new ConcurrentHashMap<>();
        Map<Integer, Integer> columnTypeMap = new ConcurrentHashMap<>();
        Map<String, Class<?>> columnJavaTypes = new ConcurrentHashMap<>();
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

            // Map column names to indexes (uppercase only for consistency with Oracle)
            columnIndexMap.put(columnName.toUpperCase(), i);
            if (!columnName.equals(columnLabel)) {
                columnIndexMap.put(columnLabel.toUpperCase(), i);
            }

            // Map column index to SQL type
            columnTypeMap.put(i, columnType);

            // Map column names to Java types (uppercase only)
            Class<?> javaType = SqlTypeMapper.sqlTypeToJavaClass(columnType);
            columnJavaTypes.put(columnName.toUpperCase(), javaType);
            if (!columnName.equals(columnLabel)) {
                columnJavaTypes.put(columnLabel.toUpperCase(), javaType);
            }

            log.trace("Cached column {}: name='{}', label='{}', sqlType={}, javaType={}",
                    i, columnName, columnLabel, columnType, javaType.getSimpleName());
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
            Integer columnIndex = columnKey != null ? columnIndexMap.get(columnKey.toUpperCase()) : null;

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
                .columnJavaTypes(columnJavaTypes)
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
     * Create a context for metadata-only query
     */
    private QueryContext createMetadataContext(QueryDefinition definition) {
        // Create params map with default values for all parameters
        Map<String, Object> params = new HashMap<>();

        // Add default values for all defined parameters to avoid missing parameter
        // errors
        for (Map.Entry<String, ParamDef> entry : definition.getParameters().entrySet()) {
            String paramName = entry.getKey();
            ParamDef paramDef = entry.getValue();

            // Use the default value if defined, otherwise use a dummy value based on type
            if (paramDef.defaultValue() != null) {
                params.put(paramName, paramDef.defaultValue());
            } else {
                // Provide dummy values based on type using centralized logic
                Object dummyValue = SqlTypeMapper.getDummyValue(paramDef.type());
                params.put(paramName, dummyValue);
            }
        }

        return QueryContext.builder()
                .definition(definition)
                .params(params)
                .filters(new HashMap<>())
                .sorts(new ArrayList<>())
                .build();
    }

}