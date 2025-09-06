package com.balsam.oasis.common.registry.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.balsam.oasis.common.registry.core.execution.MetadataBuilder;
import com.balsam.oasis.common.registry.core.execution.MetadataCache;
import com.balsam.oasis.common.registry.core.execution.MetadataCacheBuilder;
import com.balsam.oasis.common.registry.base.BaseExecutor;
import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.exception.QueryExecutionException;
import com.balsam.oasis.common.registry.exception.QueryNotFoundException;
import com.google.common.collect.ImmutableList;

/**
 * Default implementation of QueryExecutor using JdbcTemplate
 */
public class QueryExecutorImpl implements QueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutorImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final QueryRegistry queryRegistry;
    private final QuerySqlBuilder sqlBuilder;
    private final QueryRowMapperImpl rowMapper;
    private final MetadataCacheBuilder metadataCacheBuilder;

    public QueryExecutorImpl(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder, QueryRegistry queryRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.queryRegistry = queryRegistry;
        this.sqlBuilder = sqlBuilder;
        this.rowMapper = new QueryRowMapperImpl();
        this.metadataCacheBuilder = new MetadataCacheBuilder(jdbcTemplate, sqlBuilder);
    }

    @Override
    public QueryExecution execute(String queryName) {
        QueryDefinition definition = queryRegistry.get(queryName);
        if (definition == null) {
            throw new QueryNotFoundException(queryName);
        }
        return new QueryExecution(definition, this);
    }

    @Override
    public QueryExecution execute(QueryDefinition definition) {
        return new QueryExecution(definition, this);
    }

    @Override
    public QueryExecution prepare(QueryDefinition definition) {
        return new QueryExecution(definition, this);
    }

    /**
     * Internal execution method
     */
    @Transactional(readOnly = true)
    QueryResult doExecute(QueryContext context) {
        context.startExecution();

        try {
            // Run pre-processors
            runPreProcessors(context);

            // Build SQL
            BaseExecutor.SqlResult sqlResult = sqlBuilder.build(context);
            String finalSql = sqlResult.getSql();
            Map<String, Object> params = sqlResult.getParams();

            log.debug("Executing query '{}': {}", context.getDefinition().getName(), finalSql);
            log.debug("Parameters: {}", params);

            // Calculate total count if pagination is used
            if (context.hasPagination()) {
                int totalCount = executeTotalCountQuery(context, sqlResult);
                context.setTotalCount(totalCount);
            }

            // Execute query
            List<Row> rows = executeQuery(context, finalSql, params);

            // Run row processors (includes virtual attribute calculation)
            rows = runRowProcessors(context, rows);

            // Build initial result
            QueryResult result = QueryResult.builder()
                    .rows(ImmutableList.copyOf(rows))
                    .context(context)
                    .build();

            // Run post-processors
            result = runPostProcessors(context, result);

            // Add metadata if requested
            if (context.isIncludeMetadata()) {
                result = addMetadata(context, result);
            }

            context.endExecution();

            return result.toBuilder()
                    .executionTimeMs(context.getExecutionTime())
                    .success(true)
                    .build();

        } catch (Exception e) {
            context.endExecution();
            log.error("Query execution failed for '{}': {}",
                    context.getDefinition().getName(), e.getMessage(), e);

            return QueryResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(context.getExecutionTime())
                    .build();
        }
    }

    private void runPreProcessors(QueryContext context) {
        QueryDefinition definition = context.getDefinition();
        if (definition.hasPreProcessors()) {
            definition.getPreProcessors().forEach(processor -> processor.apply(context));
        }
    }

    private List<Row> executeQuery(QueryContext context, String sql, Map<String, Object> params) {
        try {
            // Set query timeout if configured
            if (context.getDefinition().getQueryTimeout() != null) {
                jdbcTemplate.setQueryTimeout(context.getDefinition().getQueryTimeout());
            }

            // Try to ensure metadata cache exists for optimal performance
            QueryDefinition definition = context.getDefinition();
            if (definition.getMetadataCache() == null) {
                try {
                    log.debug("Building metadata cache for query: {}", definition.getName());
                    MetadataCache cache = metadataCacheBuilder.buildCache(definition);
                    definition.setMetadataCache(cache);
                } catch (Exception e) {
                    log.debug("Could not build metadata cache, will use name-based access: {}", e.getMessage());
                }
            }

            // Use the unified row mapper (it will adapt based on cache availability)
            return namedJdbcTemplate.query(sql, params,
                    (rs, rowNum) -> rowMapper.mapRow(rs, rowNum, context));

        } catch (Exception e) {
            throw new QueryExecutionException(
                    context.getDefinition().getName(),
                    "Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * @deprecated The row mapper now automatically optimizes based on cache availability
     */
    @Deprecated
    public void setUseOptimizedMapper(boolean useOptimized) {
        // No-op for backward compatibility
        log.debug("setUseOptimizedMapper is deprecated - optimization is now automatic");
    }

    /**
     * Pre-warm metadata caches for all registered queries
     */
    public void prewarmAllCaches() {
        Collection<QueryDefinition> queries = queryRegistry.getAllQueries();
        log.info("Pre-warming metadata caches for {} queries", queries.size());
        Map<String, MetadataCache> caches = metadataCacheBuilder.prewarmCaches(queries);

        // Set caches on definitions
        for (Map.Entry<String, MetadataCache> entry : caches.entrySet()) {
            QueryDefinition definition = queryRegistry.get(entry.getKey());
            if (definition != null) {
                definition.setMetadataCache(entry.getValue());
            }
        }

        log.info("Pre-warmed {} metadata caches", caches.size());
    }

    private int executeTotalCountQuery(QueryContext context, BaseExecutor.SqlResult sqlResult) {
        try {
            // Build count query without pagination
            // Use the original SQL without pagination for count
            String countSql = sqlBuilder.buildCountQuery(context);
            Map<String, Object> params = sqlResult.getParams();

            log.debug("Executing count query: {}", countSql);

            Integer count = namedJdbcTemplate.queryForObject(countSql, params, Integer.class);
            return count != null ? count : 0;

        } catch (Exception e) {
            log.warn("Failed to execute count query, using fallback: {}", e.getMessage());
            return 0;
        }
    }

    private List<Row> runRowProcessors(QueryContext context, List<Row> rows) {
        QueryDefinition definition = context.getDefinition();

        // Note: Transient attributes are now handled by DynamicRowMapper
        // This method focuses on running custom row processors

        // Then run row processors if any
        if (!definition.hasRowProcessors()) {
            return rows;
        }

        List<Row> processedRows = new ArrayList<>();
        for (Row row : rows) {
            Row processedRow = row;
            for (var processor : definition.getRowProcessors()) {
                Object[] args = new Object[] { processedRow, context };
                Object result = processor.apply(args);
                if (result instanceof Row) {
                    processedRow = (Row) result;
                }
            }
            processedRows.add(processedRow);
        }

        return processedRows;
    }

    private QueryResult runPostProcessors(QueryContext context, QueryResult result) {
        QueryDefinition definition = context.getDefinition();
        if (!definition.hasPostProcessors()) {
            return result;
        }

        QueryResult processedResult = result;
        for (var processor : definition.getPostProcessors()) {
            Object[] args = new Object[] { processedResult, context };
            Object processorResult = processor.apply(args);
            if (processorResult instanceof QueryResult queryResult) {
                processedResult = queryResult;
            }
        }

        return processedResult;
    }

    private QueryResult addMetadata(QueryContext context, QueryResult result) {
        var metadataBuilder = new MetadataBuilder(context, result);
        var metadata = metadataBuilder.build();

        return result.toBuilder()
                .metadata(metadata)
                .build();
    }
}
