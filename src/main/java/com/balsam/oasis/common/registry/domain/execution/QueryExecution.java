package com.balsam.oasis.common.registry.domain.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.api.QueryExecutor;
import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.Pagination;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.definition.FilterOp;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.definition.SortDir;
import com.balsam.oasis.common.registry.domain.result.Row;
import com.balsam.oasis.common.registry.engine.QueryExecutorImpl;
import com.balsam.oasis.common.registry.exception.QueryValidationException;
import com.balsam.oasis.common.registry.processor.ParamProcessor;

/**
 * Fluent builder for configuring and executing queries.
 * Provides a chainable API for setting parameters, filters, sorts, and
 * pagination.
 *
 * <p>
 * This class implements the builder pattern to construct query executions
 * with various configuration options. All methods return the QueryExecution
 * instance for method chaining.
 * </p>
 *
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Parameter binding with type conversion and validation</li>
 * <li>Dynamic filtering with multiple operators</li>
 * <li>Sorting by multiple attributes</li>
 * <li>Pagination with start/end or page/size</li>
 * <li>Conditional filtering</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * QueryResult result = queryExecution
 *         .withParam("minSalary", 50000)
 *         .withFilter("department", FilterOp.IN, Arrays.asList("IT", "HR"))
 *         .filterIf(includeInactive, "status", FilterOp.EQUALS, "INACTIVE")
 *         .withSort("salary", SortDir.DESC)
 *         .withPagination(0, 100)
 *         .validate()
 *         .execute();
 * </pre>
 *
 * @author Query Registration System
 * @since 1.0
 * @see QueryExecutor
 * @see QueryContext
 * @see QueryResult
 */
public class QueryExecution {

    private final QueryDefinition definition;
    private final QueryContext context;
    private final QueryExecutorImpl executor;

    public QueryExecution(QueryDefinition definition, QueryExecutorImpl executor) {
        this.definition = definition;
        this.executor = executor;
        this.context = QueryContext.builder()
                .definition(definition)
                .build();
    }

    // Parameter methods
    public QueryExecution withParam(String name, Object value) {
        context.addParam(name, value);
        return this;
    }

    public QueryExecution withParams(Map<String, Object> params) {
        params.forEach(context::addParam);
        return this;
    }

    // Filter methods
    public QueryExecution withFilter(String attributeName, FilterOp op, Object value) {
        context.addFilter(attributeName, op, value);
        return this;
    }

    public QueryExecution withFilter(String attributeName, FilterOp op, Object value1, Object value2) {
        context.addFilter(attributeName, op, value1, value2);
        return this;
    }

    public QueryExecution withFilter(String attributeName, FilterOp op, List<Object> values) {
        context.addFilter(attributeName, op, values);
        return this;
    }

    public QueryExecution withFilters(Map<String, QueryContext.Filter> filters) {
        filters.forEach((key, filter) -> {
            if (filter.hasMultipleValues()) {
                context.addFilter(filter.getAttribute(), filter.getOperator(), filter.getValues());
            } else if (filter.requiresTwoValues()) {
                context.addFilter(filter.getAttribute(), filter.getOperator(),
                        filter.getValue(), filter.getValue2());
            } else {
                context.addFilter(filter.getAttribute(), filter.getOperator(), filter.getValue());
            }
        });
        return this;
    }

    public QueryExecution filterIf(boolean condition, String attr, FilterOp op, Object value) {
        if (condition) {
            withFilter(attr, op, value);
        }
        return this;
    }

    // Sort methods
    public QueryExecution withSort(String attributeName, SortDir direction) {
        context.addSort(attributeName, direction);
        return this;
    }

    public QueryExecution withSort(List<QueryContext.SortSpec> sorts) {
        sorts.forEach(sort -> context.addSort(sort.getAttribute(), sort.getDirection()));
        return this;
    }

    // Pagination
    public QueryExecution withPagination(int start, int end) {
        // Validate pagination values
        if (start < 0) {
            start = 0; // Treat negative start as 0
        }
        if (end < start) {
            end = start; // Ensure end is not before start
        }
        int pageSize = end - start;
        if (pageSize > 1000) {
            // Limit page size to prevent excessive data retrieval
            end = start + 1000;
        }
        context.setPagination(Pagination.builder()
                .start(start)
                .end(end)
                .build());
        return this;
    }

    public QueryExecution withOffsetLimit(int offset, int limit) {
        // Validate offset and limit
        if (offset < 0) {
            offset = 0;
        }
        if (limit <= 0) {
            limit = 50; // Default page size
        }
        if (limit > 1000) {
            limit = 1000; // Max page size
        }
        context.setPagination(Pagination.builder()
                .start(offset)
                .end(offset + limit)
                .build());
        return this;
    }

    // Security context
    public QueryExecution withSecurityContext(Object securityContext) {
        context.setSecurityContext(securityContext);
        return this;
    }

    // Metadata control
    public QueryExecution includeMetadata(boolean include) {
        context.setIncludeMetadata(include);
        return this;
    }

    // Caching control
    public QueryExecution withCaching(boolean enabled) {
        context.setCacheEnabled(enabled);
        return this;
    }

    // Validation
    public QueryExecution validate() {
        List<String> violations = new ArrayList<>();

        // Apply default values and validate required parameters
        definition.getParams().forEach((name, paramDef) -> {
            // If parameter is not provided
            if (!context.hasParam(name)) {
                if (paramDef.hasDefaultValue()) {
                    // Apply default value if it has one
                    context.addParam(name, paramDef.getDefaultValue());
                } else if (!paramDef.isRequired()) {
                    // Initialize non-required params with null if no default value
                    context.addParam(name, null);
                }
            }

            // Check required parameters
            if (paramDef.isRequired() && !context.hasParam(name)) {
                violations.add("Required parameter missing: " + name);
            }

            // Process and validate parameter values
            if (context.hasParam(name)) {
                Object value = context.getParam(name);

                // Apply processor if exists (handles validation and transformation)
                if (paramDef.hasProcessor()) {
                    try {
                        @SuppressWarnings("unchecked")
                        ParamProcessor<Object> processor = (ParamProcessor<Object>) paramDef.getProcessor();
                        Object processedValue = processor.process(value, context);
                        // Update the parameter with processed value
                        context.addParam(name, processedValue);
                    } catch (Exception e) {
                        violations.add("Parameter validation/processing failed for " + name + ": " + e.getMessage());
                    }
                } else {
                    // Use the built-in validation method
                    @SuppressWarnings("unchecked")
                    ParamDef<Object> typedParam = (ParamDef<Object>) paramDef;
                    if (!typedParam.isValid(value, context)) {
                        violations.add("Parameter validation failed: " + name);
                    }
                }
            }
        });

        // Validate filters
        context.getFilters().forEach((attribute, filter) -> {
            var attrDef = definition.getAttribute(attribute);
            if (attrDef == null) {
                violations.add("Unknown attribute for filter: " + attribute);
            } else if (!attrDef.isFilterable()) {
                violations.add("Attribute not filterable: " + attribute);
            }
        });

        // Validate sorts
        context.getSorts().forEach(sort -> {
            var attrDef = definition.getAttribute(sort.getAttribute());
            if (attrDef == null) {
                violations.add("Unknown attribute for sort: " + sort.getAttribute());
            } else if (!attrDef.isSortable()) {
                violations.add("Attribute not sortable: " + sort.getAttribute());
            }
        });

        // Validate pagination
        if (context.hasPagination()) {
            var pagination = context.getPagination();
            if (pagination.getPageSize() > definition.getMaxPageSize()) {
                violations.add(String.format("Page size %d exceeds maximum %d",
                        pagination.getPageSize(), definition.getMaxPageSize()));
            }
        }

        // Run custom validation rules
        if (definition.hasValidationRules()) {
            definition.getValidationRules().forEach(rule -> {
                if (!rule.getRule().test(context)) {
                    violations.add(rule.getErrorMessage());
                }
            });
        }

        if (!violations.isEmpty()) {
            throw new QueryValidationException(definition.getName(), violations);
        }

        return this;
    }

    // Execution
    public QueryResult execute() {
        // Initialize non-required params with null if not provided
        initializeNonRequiredParams();

        // Auto-validate before execution
        validate();

        // Execute the query
        return executor.doExecute(context);
    }

    private void initializeNonRequiredParams() {
        definition.getParams().forEach((name, paramDef) -> {
            // If parameter is not provided and not required
            if (!context.hasParam(name) && !paramDef.isRequired()) {
                if (paramDef.hasDefaultValue()) {
                    context.addParam(name, paramDef.getDefaultValue());
                } else {
                    context.addParam(name, null);
                }
            }
        });
    }

    // Execute for single object (findByKey)
    public Row executeSingle() {
        // Initialize non-required params with null if not provided
        initializeNonRequiredParams();

        // Auto-validate before execution
        validate();

        // Execute the query
        QueryResult result = executor.doExecute(context);

        // Check result size
        if (result.getRows().isEmpty()) {
            return null;
        }

        if (result.getRows().size() > 1) {
            throw new QueryValidationException(definition.getName(),
                    String.format("FindByKey query returned %d results, expected 1", result.getRows().size()));
        }

        return result.getRows().get(0);
    }

    // Async execution
    public java.util.concurrent.CompletableFuture<QueryResult> executeAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(this::execute);
    }

    // Async execution for single object
    public java.util.concurrent.CompletableFuture<Object> executeSingleAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(this::executeSingle);
    }
}
