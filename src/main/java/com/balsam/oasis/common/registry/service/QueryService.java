package com.balsam.oasis.common.registry.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.api.QueryExecutor;
import com.balsam.oasis.common.registry.domain.api.QueryRegistry;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.exception.QueryValidationException;
import com.balsam.oasis.common.registry.domain.execution.QueryExecution;
import com.balsam.oasis.common.registry.web.dto.request.QueryRequest;

/**
 * Shared service for executing queries and selects.
 * Provides common business logic for both QueryController and SelectController.
 */
@Service
public class QueryService {
    
    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    
    private final QueryExecutor queryExecutor;
    private final QueryRegistry queryRegistry;
    
    public QueryService(QueryExecutor queryExecutor, QueryRegistry queryRegistry) {
        this.queryExecutor = queryExecutor;
        this.queryRegistry = queryRegistry;
    }
    
    /**
     * Execute a query with the provided request parameters.
     *
     * @param queryName The name of the registered query
     * @param request The parsed query request containing parameters, filters, etc.
     * @return QueryResult containing the execution results
     * @throws QueryException if query not found or execution fails
     */
    public QueryResult executeQuery(String queryName, QueryRequest request) {
        log.info("Executing query: {} with params: {}", queryName, request.getParams());
        
        // Get the query definition
        QueryDefinitionBuilder queryDefinition = queryRegistry.get(queryName);
        
        if (queryDefinition == null) {
            throw new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "Query not found: " + queryName);
        }
        
        // Validate request
        validateRequest(queryDefinition, request);
        
        // Create execution
        QueryExecution execution = queryExecutor.execute(queryDefinition);
        
        // Apply parameters
        if (request.getParams() != null) {
            request.getParams().forEach(execution::withParam);
        }
        
        // Apply filters
        if (request.getFilters() != null) {
            request.getFilters().forEach((key, filter) -> {
                if (filter.getValues() != null && !filter.getValues().isEmpty()) {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), filter.getValues());
                } else if (filter.getValue2() != null) {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), 
                            filter.getValue(), filter.getValue2());
                } else if (filter.getValue() != null) {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), filter.getValue());
                } else {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), null);
                }
            });
        }
        
        // Apply sorting
        if (request.getSorts() != null) {
            request.getSorts().forEach(sort -> 
                execution.withSort(sort.getAttribute(), sort.getDirection())
            );
        }
        
        // Apply pagination
        if (request.getPagination() != null) {
            execution.withPagination(request.getPagination().getOffset(), 
                    request.getPagination().getLimit());
        }
        
        // Execute and return result
        return execution.execute();
    }
    
    /**
     * Execute a query with default empty parameters.
     *
     * @param queryName The name of the registered query
     * @return QueryResult containing the execution results
     * @throws QueryException if query not found or execution fails
     */
    public QueryResult executeQuery(String queryName) {
        return executeQuery(queryName, QueryRequest.builder().build());
    }
    
    /**
     * Execute a query with direct execution object.
     * Used for programmatic query execution.
     *
     * @param queryDefinition The query definition
     * @return QueryExecution object for fluent configuration
     */
    public QueryExecution createExecution(QueryDefinitionBuilder queryDefinition) {
        if (queryDefinition == null) {
            throw new QueryException(QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "Query definition cannot be null");
        }
        
        return queryExecutor.execute(queryDefinition);
    }
    
    /**
     * Get a registered query definition.
     *
     * @param queryName The name of the query
     * @return QueryDefinitionBuilder or null if not found
     */
    public QueryDefinitionBuilder getQueryDefinition(String queryName) {
        return queryRegistry.get(queryName);
    }
    
    /**
     * Check if a query is registered.
     *
     * @param queryName The name of the query
     * @return true if registered, false otherwise
     */
    public boolean isQueryRegistered(String queryName) {
        return queryRegistry.get(queryName) != null;
    }
    
    /**
     * Get all registered query names.
     *
     * @return List of registered query names
     */
    public List<String> getRegisteredQueryNames() {
        // TODO: Add list() method to QueryRegistry interface
        return List.of();
    }
    
    /**
     * Validate the request against the query definition.
     */
    private void validateRequest(QueryDefinitionBuilder queryDefinition, QueryRequest request) {
        // Note: QueryDefinitionBuilder doesn't expose a way to get all parameters
        // Parameter validation will be done at execution level
        
        // Validate filters against attributes
        if (request.getFilters() != null) {
            request.getFilters().forEach((key, filter) -> {
                var attr = queryDefinition.getAttribute(filter.getAttribute());
                if (attr == null) {
                    throw new QueryValidationException(
                            "Unknown attribute for filter: " + filter.getAttribute());
                }
                if (!attr.filterable()) {
                    throw new QueryValidationException(
                            "Attribute '" + filter.getAttribute() + "' is not filterable");
                }
            });
        }
        
        // Validate sorts against attributes
        if (request.getSorts() != null) {
            request.getSorts().forEach(sort -> {
                var attr = queryDefinition.getAttribute(sort.getAttribute());
                if (attr == null) {
                    throw new QueryValidationException(
                            "Unknown attribute for sort: " + sort.getAttribute());
                }
                if (!attr.sortable()) {
                    throw new QueryValidationException(
                            "Attribute '" + sort.getAttribute() + "' is not sortable");
                }
            });
        }
    }
}