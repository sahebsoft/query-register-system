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
import com.balsam.oasis.common.registry.domain.exception.QueryExecutionException;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QuerySqlBuilder;
import com.balsam.oasis.common.registry.engine.sql.util.SqlTypeMapper;

/**
 * Builds and manages metadata caches for queries.
 * Uses PreparedStatement.getMetaData() as primary approach (fastest),
 * with wrapped WHERE 1=0 as fallback.
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
     */
    public MetadataCache buildCache(QueryDefinition definition) {
        String queryName = definition.getName();
        log.debug("Building metadata cache for query: {}", queryName);

        try {
            // Create a context for metadata query
            QueryContext metadataContext = createMetadataContext(definition);
            SqlResult sqlResult = sqlBuilder.build(metadataContext);

            // Try approaches in order
            MetadataCache cache = tryPreparedStatementMetadata(sqlResult.getSql(),
                    sqlResult.getParams(), definition);
            if (cache != null && cache.isInitialized()) {
                log.info("Retrieved metadata using PreparedStatement.getMetaData() for query: {}", queryName);
                return cache;
            }

            cache = tryExecuteWithWhere10(sqlResult.getSql(), sqlResult.getParams(), definition);
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
     * Try to get metadata using PreparedStatement.getMetaData() without executing.
     */
    private MetadataCache tryPreparedStatementMetadata(String sql, Map<String, Object> params,
            QueryDefinition definition) {
        try {
            return namedJdbcTemplate.execute(sql, params, (PreparedStatement ps) -> {
                try {
                    // Try without parameters first
                    ResultSetMetaData metaData = ps.getMetaData();
                    if (metaData != null) {
                        log.trace("Got metadata without parameters");
                        return buildCacheFromMetaData(metaData, definition);
                    }

                    // Try with dummy parameters
                    setDummyParameters(ps);
                    metaData = ps.getMetaData();
                    if (metaData != null) {
                        log.trace("Got metadata with dummy parameters");
                        return buildCacheFromMetaData(metaData, definition);
                    }
                } catch (Exception e) {
                    log.trace("PreparedStatement.getMetaData() approach failed: {}", e.getMessage());
                }
                return null;
            });
        } catch (Exception e) {
            log.trace("PreparedStatement approach failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Set dummy parameters on PreparedStatement for metadata retrieval.
     */
    private void setDummyParameters(PreparedStatement ps) throws SQLException {
        ParameterMetaData pmd = ps.getParameterMetaData();
        int paramCount = pmd.getParameterCount();

        for (int i = 1; i <= paramCount; i++) {
            try {
                SqlTypeMapper.setDummyParameter(ps, i, pmd.getParameterType(i));
            } catch (SQLException e) {
                // Fallback to string if type unknown
                ps.setString(i, "DUMMY");
            }
        }
    }

    /**
     * Fallback: Get metadata by executing wrapped query with WHERE 1=0.
     */
    private MetadataCache tryExecuteWithWhere10(String sql, Map<String, Object> params,
            QueryDefinition definition) {
        try {
            // Wrap to handle all query types
            String wrappedSql = "SELECT * FROM (" + sql + ") WHERE 1=0";

            return namedJdbcTemplate.execute(wrappedSql, params, (PreparedStatement ps) -> {
                try (ResultSet rs = ps.executeQuery()) {
                    return buildCacheFromMetaData(rs.getMetaData(), definition);
                } catch (Exception e) {
                    log.trace("WHERE 1=0 approach failed: {}", e.getMessage());
                    return null;
                }
            });
        } catch (Exception e) {
            log.trace("WHERE 1=0 approach failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build cache from ResultSetMetaData.
     */
    private MetadataCache buildCacheFromMetaData(ResultSetMetaData metaData,
            QueryDefinition definition) throws SQLException {
        int columnCount = metaData.getColumnCount();

        // Initialize collections
        Map<String, Integer> columnIndexMap = new ConcurrentHashMap<>();
        Map<Integer, Integer> columnTypeMap = new ConcurrentHashMap<>();
        Map<String, Class<?>> columnJavaTypes = new ConcurrentHashMap<>();
        String[] columnNames = new String[columnCount];
        String[] columnLabels = new String[columnCount];

        // Process columns
        for (int i = 1; i <= columnCount; i++) {
            processColumn(i, metaData, columnIndexMap, columnTypeMap,
                    columnJavaTypes, columnNames, columnLabels);
        }

        // Map attributes to columns
        Map<String, Integer> attributeToColumnIndex = new ConcurrentHashMap<>();
        Map<String, Integer> attributeToColumnType = new ConcurrentHashMap<>();
        mapAttributesToColumns(definition, columnIndexMap, columnTypeMap,
                attributeToColumnIndex, attributeToColumnType);

        // Build cache
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

        log.info("Built metadata cache for '{}': {} columns, {} attributes",
                definition.getName(), columnCount, attributeToColumnIndex.size());

        return cache;
    }

    /**
     * Process a single column's metadata.
     */
    private void processColumn(int index, ResultSetMetaData metaData,
            Map<String, Integer> columnIndexMap, Map<Integer, Integer> columnTypeMap,
            Map<String, Class<?>> columnJavaTypes, String[] columnNames,
            String[] columnLabels) throws SQLException {

        String name = metaData.getColumnName(index);
        String label = metaData.getColumnLabel(index);
        int type = metaData.getColumnType(index);
        Class<?> javaType = SqlTypeMapper.sqlTypeToJavaClass(type);

        // Store in arrays
        columnNames[index - 1] = name;
        columnLabels[index - 1] = label;

        // Map names to index (uppercase for Oracle)
        String upperName = name.toUpperCase();
        String upperLabel = label.toUpperCase();

        columnIndexMap.put(upperName, index);
        if (!upperName.equals(upperLabel)) {
            columnIndexMap.put(upperLabel, index);
        }

        // Map types
        columnTypeMap.put(index, type);
        columnJavaTypes.put(upperName, javaType);
        if (!upperName.equals(upperLabel)) {
            columnJavaTypes.put(upperLabel, javaType);
        }

        log.trace("Column {}: name='{}', label='{}', type={}, javaType={}",
                index, name, label, type, javaType.getSimpleName());
    }

    /**
     * Map attributes to their corresponding columns.
     */
    private void mapAttributesToColumns(QueryDefinition definition,
            Map<String, Integer> columnIndexMap, Map<Integer, Integer> columnTypeMap,
            Map<String, Integer> attributeToColumnIndex,
            Map<String, Integer> attributeToColumnType) {

        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip virtual attributes
            if (attr.isVirtual()) {
                continue;
            }

            // Find column for attribute
            String columnKey = attr.getAliasName() != null ? attr.getAliasName() : attrName;
            Integer columnIndex = columnIndexMap.get(columnKey.toUpperCase());

            if (columnIndex != null) {
                attributeToColumnIndex.put(attrName, columnIndex);
                attributeToColumnType.put(attrName, columnTypeMap.get(columnIndex));
                log.trace("Mapped attribute '{}' to column {}", attrName, columnIndex);
            } else {
                log.warn("No column found for attribute '{}' with alias '{}'",
                        attrName, attr.getAliasName());
            }
        }
    }

    /**
     * Create a context for metadata-only query.
     */
    private QueryContext createMetadataContext(QueryDefinition definition) {
        Map<String, Object> params = new HashMap<>();

        // Add default/dummy values for all parameters
        for (Map.Entry<String, ParamDef> entry : definition.getParameters().entrySet()) {
            ParamDef paramDef = entry.getValue();
            Object value = paramDef.defaultValue() != null
                    ? paramDef.defaultValue()
                    : SqlTypeMapper.getDummyValue(paramDef.type());
            params.put(entry.getKey(), value);
        }

        return QueryContext.builder()
                .definition(definition)
                .params(params)
                .filters(new HashMap<>())
                .sorts(new ArrayList<>())
                .build();
    }
}