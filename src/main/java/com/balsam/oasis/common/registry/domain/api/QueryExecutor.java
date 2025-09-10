package com.balsam.oasis.common.registry.domain.api;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.execution.QueryExecution;

/**
 * Main interface for executing queries in the Query Registration System.
 * Provides methods to execute queries by name or definition, with support
 * for parameters, filters, sorting, and pagination.
 *
 * <p>
 * This interface is the primary entry point for query execution.
 * Implementations handle SQL building, parameter binding, and result mapping.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * QueryExecutor executor = new QueryExecutorImpl(jdbcTemplate);
 * 
 * QueryResult result = executor.execute("userQuery")
 *         .withParam("status", "ACTIVE")
 *         .withFilter("department", FilterOp.IN, Arrays.asList("IT", "HR"))
 *         .withSort("name", SortDir.ASC)
 *         .withPagination(0, 50)
 *         .execute();
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
     * 
     * @param queryName the name of the registered query
     * @return a QueryExecution builder for configuring and executing the query
     */
    QueryExecution execute(String queryName);

    /**
     * Execute a query definition directly
     * 
     * @param definition the query definition
     * @return a QueryExecution builder for configuring and executing the query
     */
    QueryExecution execute(QueryDefinition definition);

    /**
     * Prepare a query definition for execution
     * 
     * @param definition the query definition
     * @return a QueryExecution builder for configuring and executing the query
     */
    QueryExecution prepare(QueryDefinition definition);
}