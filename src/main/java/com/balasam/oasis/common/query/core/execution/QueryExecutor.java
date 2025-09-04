package com.balasam.oasis.common.query.core.execution;

import com.balasam.oasis.common.query.core.definition.QueryDefinition;

/**
 * Main interface for executing queries in the Query Registration System.
 * Provides methods to execute queries by name or definition, with support
 * for parameters, filters, sorting, and pagination.
 *
 * <p>This interface is the primary entry point for query execution.
 * Implementations handle SQL building, parameter binding, and result mapping.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * QueryExecutor executor = new QueryExecutorImpl(jdbcTemplate);
 * 
 * QueryResult result = executor.execute("userQuery")
 *     .withParam("status", "ACTIVE")
 *     .withFilter("department", FilterOp.IN, Arrays.asList("IT", "HR"))
 *     .withSort("name", SortDir.ASC)
 *     .withPagination(0, 50)
 *     .execute();
 * </pre>
 *
 * @author Query Registration System
 * @since 1.0
 * @see QueryExecution
 * @see QueryResult
 */
public interface QueryExecutor {
    
    /**
     * Execute a query by name
     * @param queryName the name of the registered query
     * @return a QueryExecution builder for configuring and executing the query
     */
    QueryExecution execute(String queryName);
    
    /**
     * Execute a query definition directly
     * @param definition the query definition
     * @return a QueryExecution builder for configuring and executing the query
     */
    QueryExecution execute(QueryDefinition definition);
    
    /**
     * Prepare a query definition for execution
     * @param definition the query definition
     * @return a QueryExecution builder for configuring and executing the query
     */
    QueryExecution prepare(QueryDefinition definition);
    
    /**
     * Register a query definition
     * @param definition the query definition to register
     */
    void registerQuery(QueryDefinition definition);
    
    /**
     * Get a registered query definition by name
     * @param queryName the name of the query
     * @return the query definition, or null if not found
     */
    QueryDefinition getQueryDefinition(String queryName);
}