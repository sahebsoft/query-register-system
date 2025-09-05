package com.balsam.oasis.common.query.core.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.balsam.oasis.common.query.core.definition.QueryDefinition;
import com.balsam.oasis.common.query.core.result.QueryResult;
import com.balsam.oasis.common.query.core.result.Row;
import com.balsam.oasis.common.query.exception.QueryExecutionException;
import com.balsam.oasis.common.query.exception.QueryNotFoundException;
import com.balsam.oasis.common.query.registry.QueryRegistry;
import com.google.common.collect.ImmutableList;

/**
 * Default implementation of QueryExecutor using JdbcTemplate
 */
public class QueryExecutorImpl implements QueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutorImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final QueryRegistry queryRegistry;
    private final SqlBuilder sqlBuilder;
    private final DynamicRowMapper rowMapper;
    private final OptimizedRowMapper optimizedRowMapper;
    private final MetadataCacheBuilder metadataCacheBuilder;
    private boolean useOptimizedMapper = true;

    public QueryExecutorImpl(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder, QueryRegistry queryRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.queryRegistry = queryRegistry;
        this.sqlBuilder = sqlBuilder;
        this.rowMapper = new DynamicRowMapper();
        this.optimizedRowMapper = new OptimizedRowMapper();
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
            SqlBuilder.SqlResult sqlResult = sqlBuilder.build(context);
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

            // Get metadata cache for optimized mapping
            QueryDefinition definition = context.getDefinition();
            MetadataCache cache = definition.getMetadataCache();

            // If cache doesn't exist, try to build it on-the-fly
            if (cache == null || !cache.isInitialized()) {
                log.debug("Building metadata cache on-the-fly for query: {}", definition.getName());
                try {
                    cache = metadataCacheBuilder.buildCache(definition);
                    definition.setMetadataCache(cache);
                } catch (Exception e) {
                    log.warn("Failed to build metadata cache, falling back to DynamicRowMapper: {}", e.getMessage());
                }
            }

            // Choose appropriate row mapper based on configuration and cache availability
            final boolean useOptimized = useOptimizedMapper && OptimizedRowMapper.canUse(cache);

            if (useOptimized) {
                log.trace("Using OptimizedRowMapper for query: {}", definition.getName());
                final MetadataCache finalCache = cache;
                return namedJdbcTemplate.query(sql, params,
                        (rs, rowNum) -> optimizedRowMapper.mapRow(rs, rowNum, context, finalCache));
            } else {
                log.trace("Using DynamicRowMapper for query: {}", definition.getName());
                return namedJdbcTemplate.query(sql, params,
                        (rs, rowNum) -> rowMapper.mapRow(rs, rowNum, context));
            }

        } catch (Exception e) {
            throw new QueryExecutionException(
                    context.getDefinition().getName(),
                    "Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * Enable or disable the optimized row mapper
     */
    public void setUseOptimizedMapper(boolean useOptimized) {
        this.useOptimizedMapper = useOptimized;
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

    private int executeTotalCountQuery(QueryContext context, SqlBuilder.SqlResult sqlResult) {
        try {
            // Build count query without pagination
            String baseSql = sqlResult.getBaseSql();
            // Oracle doesn't require the AS keyword for subquery aliases
            String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") count_query";
            Map<String, Object> params = sqlResult.getParamsWithoutPagination();

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
            if (processorResult instanceof QueryResult) {
                processedResult = (QueryResult) processorResult;
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
