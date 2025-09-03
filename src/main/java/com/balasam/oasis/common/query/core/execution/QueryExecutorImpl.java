package com.balasam.oasis.common.query.core.execution;

import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.core.result.QueryResult;
import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.exception.QueryExecutionException;
import com.balasam.oasis.common.query.exception.QueryNotFoundException;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of QueryExecutor using JdbcTemplate
 */
public class QueryExecutorImpl implements QueryExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(QueryExecutorImpl.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final Map<String, QueryDefinition> queryRegistry;
    private final SqlBuilder sqlBuilder;
    private final DynamicRowMapper rowMapper;
    
    public QueryExecutorImpl(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.queryRegistry = new ConcurrentHashMap<>();
        this.sqlBuilder = sqlBuilder;
        this.rowMapper = new DynamicRowMapper();
    }
    
    /**
     * Register a query definition
     */
    public void registerQuery(QueryDefinition definition) {
        queryRegistry.put(definition.getName(), definition);
        log.info("Registered query: {}", definition.getName());
    }
    
    /**
     * Register multiple query definitions
     */
    public void registerQueries(Collection<QueryDefinition> definitions) {
        definitions.forEach(this::registerQuery);
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
            
            // Execute query with row mapper
            return namedJdbcTemplate.query(sql, params, 
                (rs, rowNum) -> rowMapper.mapRow(rs, rowNum, context));
                
        } catch (Exception e) {
            throw new QueryExecutionException(
                context.getDefinition().getName(), 
                "Failed to execute query: " + e.getMessage(), e);
        }
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
        
        // Process virtual attributes first
        var virtualAttrs = definition.getAttributes().values().stream()
            .filter(attr -> attr.isVirtual() && attr.hasProcessor())
            .toList();
        
        // Apply virtual attribute processors
        if (!virtualAttrs.isEmpty()) {
            for (Row row : rows) {
                for (var attr : virtualAttrs) {
                    // Processor handles virtual attribute calculation with full context
                    Object value = attr.getProcessor().process(null, row, context);
                    row.setVirtual(attr.getName(), value);
                }
            }
        }
        
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