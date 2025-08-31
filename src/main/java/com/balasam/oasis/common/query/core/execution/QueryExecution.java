package com.balasam.oasis.common.query.core.execution;

import com.balasam.oasis.common.query.core.definition.FilterOp;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.core.definition.SortDir;
import com.balasam.oasis.common.query.core.result.QueryResult;
import com.balasam.oasis.common.query.exception.QueryValidationException;

import java.util.*;

/**
 * Fluent builder for query execution
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
        context.setPagination(QueryContext.Pagination.fromStartEnd(start, end));
        return this;
    }
    
    public QueryExecution withOffsetLimit(int offset, int limit) {
        context.setPagination(QueryContext.Pagination.fromOffsetLimit(offset, limit));
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
        
        // Validate required parameters
        definition.getParams().forEach((name, paramDef) -> {
            if (paramDef.isRequired() && !context.hasParam(name)) {
                if (paramDef.hasDefaultValue()) {
                    context.addParam(name, paramDef.getDefaultValue());
                } else {
                    violations.add("Required parameter missing: " + name);
                }
            }
            
            // Validate parameter values
            if (context.hasParam(name)) {
                Object value = context.getParam(name);
                if (paramDef.hasValidator() && !paramDef.getValidator().apply(value)) {
                    violations.add("Parameter validation failed: " + name);
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
            } else if (!attrDef.supportsOperator(filter.getOperator())) {
                violations.add(String.format("Operator %s not supported for attribute %s", 
                    filter.getOperator(), attribute));
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
        // Auto-validate before execution
        validate();
        
        // Execute the query
        return executor.doExecute(context);
    }
    
    // Execute for single object (findByKey)
    public Object executeSingle() {
        // Check if query has findByKey
        if (!definition.hasFindByKey()) {
            throw new QueryValidationException(definition.getName(), 
                "Query does not support single object retrieval. Use execute() for list results.");
        }
        
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