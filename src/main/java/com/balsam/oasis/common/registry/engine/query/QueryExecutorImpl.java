package com.balsam.oasis.common.registry.engine.query;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.domain.common.SqlResult;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.execution.QueryExecution;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.google.common.collect.ImmutableList;

/**
 * Default implementation of QueryExecutor using JdbcTemplate
 */
public class QueryExecutorImpl {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutorImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final QueryRegistryImpl queryRegistry;
    private final QuerySqlBuilder sqlBuilder;
    private final QueryRowMapperImpl rowMapper;

    public QueryExecutorImpl(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder, QueryRegistryImpl queryRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.queryRegistry = queryRegistry;
        this.sqlBuilder = sqlBuilder;
        this.rowMapper = new QueryRowMapperImpl();
    }

    public QueryExecution execute(String queryName) {
        QueryDefinitionBuilder definition = queryRegistry.get(queryName);
        if (definition == null) {
            throw new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND, "Query not found: " + queryName);
        }
        return new QueryExecution(definition, this);
    }

    public QueryExecution execute(QueryDefinitionBuilder definition) {
        return new QueryExecution(definition, this);
    }

    public QueryExecution prepare(QueryDefinitionBuilder definition) {
        return new QueryExecution(definition, this);
    }

    /**
     * Internal execution method
     */
    @Transactional(readOnly = true)
    public QueryData doExecute(QueryContext context) {
        context.startExecution();

        try {
            // Run pre-processors
            runPreProcessors(context);

            // Build SQL
            SqlResult sqlResult = sqlBuilder.build(context);
            String finalSql = sqlResult.getSql();
            Map<String, Object> params = sqlResult.getParams();

            log.debug("Executing query '{}': {}", context.getDefinition().getName(), finalSql);
            log.debug("Parameters: {}", params);

            // Calculate total count if pagination is used
            if (context.hasPagination() && context.getDefinition().isPaginationEnabled()) {
                int totalCount = executeTotalCountQuery(context, sqlResult);
                context.setTotalCount(totalCount);
            }

            // Execute query
            List<QueryRow> rows = executeQuery(context, finalSql, params);

            // Run row processors (includes virtual attribute calculation)
            rows = runRowProcessors(context, rows);

            // Build initial result
            QueryData result = QueryData.builder()
                    .rows(ImmutableList.copyOf(rows))
                    .context(context)
                    .build();

            // Run result-aware parameter processors
            runResultAwareParamProcessors(context, result);

            // Run post-processors
            result = runPostProcessors(context, result);

            // Add metadata if requested
            if (context.isIncludeMetadata()) {
                result = addMetadata(context, result);
            }

            context.endExecution();

            return result.toBuilder()
                    .executionTimeMs(context.getExecutionTime())
                    .build();

        } catch (Exception e) {
            context.endExecution();
            log.error("Query execution failed for '{}': {}",
                    context.getDefinition().getName(), e.getMessage(), e);

            // Throw the exception so it can be properly handled by the REST controller
            // This ensures proper HTTP status codes and error messages are returned
            if (e instanceof QueryException queryException) {
                throw queryException;
            } else {
                throw new QueryException(
                        context.getDefinition().getName(),
                        QueryException.ErrorCode.EXECUTION_ERROR,
                        "Query execution failed: " + e.getMessage(), e);
            }
        }
    }

    private void runPreProcessors(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (definition.hasPreProcessors()) {
            definition.getPreProcessors().forEach(processor -> processor.process(context));
        }
    }

    private void runResultAwareParamProcessors(QueryContext context, QueryData result) {
        QueryDefinitionBuilder definition = context.getDefinition();

        if (!definition.hasParams()) {
            return;
        }

        // Allow parameters to be processed again with access to query results
        // This enables parameters to be transformed based on result data
        definition.getParameters().forEach((paramName, paramDef) -> {
            if (paramDef.hasProcessor()) {
                try {
                    Object currentValue = context.getParam(paramName);
                    @SuppressWarnings("unchecked")
                    var processor = (com.balsam.oasis.common.registry.domain.processor.ParamProcessor<Object>) paramDef.processor();

                    // Use the same context - the processor can access full context if needed
                    Object processedValue = processor.process(currentValue, context);
                    context.addParam(paramName, processedValue);
                } catch (Exception e) {
                    log.warn("Failed to process result-aware parameter {}: {}", paramName, e.getMessage());
                }
            }
        });
    }

    private List<QueryRow> executeQuery(QueryContext context, String sql, Map<String, Object> params) {
        try {
            // Set query timeout if configured
            if (context.getDefinition().getQueryTimeout() != null) {
                jdbcTemplate.setQueryTimeout(context.getDefinition().getQueryTimeout());
            }

            final QueryContext finalContext = context;
            final QueryDefinitionBuilder finalDefinition = context.getDefinition();

            // Execute query with fetch size optimization
            return namedJdbcTemplate.execute(sql, params, (ps) -> {
                // Apply fetch size if configured
                if (finalDefinition.getFetchSize() != null) {
                    ps.setFetchSize(finalDefinition.getFetchSize());
                } else {
                    ps.setFetchSize(100);
                }
                // Set fetch direction for optimization
                ps.setFetchDirection(ResultSet.FETCH_FORWARD);

                // Execute and map results
                try (ResultSet rs = ps.executeQuery()) {
                    List<QueryRow> results = new ArrayList<>();
                    int rowNum = 0;
                    while (rs.next()) {
                        results.add(rowMapper.mapRow(rs, rowNum++, finalContext));
                    }
                    return results;
                }
            });

        } catch (Exception e) {
            throw new QueryException(
                    context.getDefinition().getName(),
                    QueryException.ErrorCode.EXECUTION_ERROR,
                    "Failed to execute query: " + e.getMessage(), e);
        }
    }

    private int executeTotalCountQuery(QueryContext context, SqlResult sqlResult) {
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

    private List<QueryRow> runRowProcessors(QueryContext context, List<QueryRow> rows) {
        QueryDefinitionBuilder definition = context.getDefinition();

        // First recalculate virtual attributes with full context access
        List<QueryRow> processedRows = recalculateVirtualAttributesWithFullContext(context, rows);

        // Then run custom row processors if any
        if (definition.hasRowProcessors()) {
            List<QueryRow> tempProcessedRows = new ArrayList<>();
            for (QueryRow row : processedRows) {
                QueryRow processedRow = row;
                for (var processor : definition.getRowProcessors()) {
                    processedRow = processor.process(processedRow, context);
                }
                tempProcessedRows.add(processedRow);
            }
            processedRows = tempProcessedRows;
        }

        // Finally apply attribute formatters
        processedRows = applyAttributeFormatters(context, processedRows);

        return processedRows;
    }

    private List<QueryRow> recalculateVirtualAttributesWithFullContext(QueryContext context, List<QueryRow> rows) {
        QueryDefinitionBuilder definition = context.getDefinition();

        if (!definition.hasAttributes()) {
            return rows;
        }

        // Check if any attributes need full context calculation
        boolean hasAggregateCalculators = definition.getAttributes().values().stream()
                .anyMatch(attr -> attr.virtual() && attr.hasCalculator());

        if (!hasAggregateCalculators) {
            return rows;
        }

        // Recalculate virtual attributes with access to all rows
        List<QueryRow> enhancedRows = new ArrayList<>();
        for (QueryRow row : rows) {
            QueryRow enhancedRow = row;
            for (Map.Entry<String, com.balsam.oasis.common.registry.domain.definition.AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
                String attrName = entry.getKey();
                var attr = entry.getValue();

                if (attr.virtual() && attr.hasCalculator()) {
                    try {
                        @SuppressWarnings("unchecked")
                        var calculator = (com.balsam.oasis.common.registry.domain.processor.Calculator<Object>) attr.calculator();
                        Object enhancedValue = calculator.calculateWithAllRows(enhancedRow, rows, context);
                        enhancedRow.set(attrName, enhancedValue);
                    } catch (Exception e) {
                        log.warn("Failed to recalculate virtual attribute {}: {}", attrName, e.getMessage());
                    }
                }
            }
            enhancedRows.add(enhancedRow);
        }

        return enhancedRows;
    }

    private List<QueryRow> applyAttributeFormatters(QueryContext context, List<QueryRow> rows) {
        QueryDefinitionBuilder definition = context.getDefinition();

        if (!definition.hasAttributes()) {
            return rows;
        }

        boolean hasFormatters = definition.getAttributes().values().stream()
                .anyMatch(com.balsam.oasis.common.registry.domain.definition.AttributeDef::hasFormatter);

        if (!hasFormatters) {
            return rows;
        }

        List<QueryRow> formattedRows = new ArrayList<>();
        for (QueryRow row : rows) {
            QueryRow formattedRow = applyRowAttributeFormatters(row, definition);
            formattedRows.add(formattedRow);
        }

        return formattedRows;
    }

    private QueryRow applyRowAttributeFormatters(QueryRow row, QueryDefinitionBuilder definition) {
        for (Map.Entry<String, com.balsam.oasis.common.registry.domain.definition.AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            var attr = entry.getValue();

            if (attr.hasFormatter()) {
                Object value = row.get(attrName);
                if (value != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        var formatter = (com.balsam.oasis.common.registry.domain.processor.AttributeFormatter<Object>) attr.formatter();
                        String formattedValue = formatter.format(value);
                        row.set(attrName, formattedValue);
                    } catch (Exception e) {
                        log.warn("Failed to format attribute {}: {}", attrName, e.getMessage());
                    }
                }
            }
        }
        return row;
    }

    private QueryData runPostProcessors(QueryContext context, QueryData result) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (!definition.hasPostProcessors()) {
            return result;
        }

        QueryData processedResult = result;
        for (var processor : definition.getPostProcessors()) {
            processedResult = processor.process(processedResult, context);
        }

        return processedResult;
    }

    private QueryData addMetadata(QueryContext context, QueryData result) {
        var metadataBuilder = new QueryMetadata.MetadataBuilder(context, result);
        var metadata = metadataBuilder.build();

        return result.toBuilder()
                .metadata(metadata)
                .build();
    }
}
