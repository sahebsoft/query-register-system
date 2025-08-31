package com.balasam.oasis.common.query.core.execution;

import com.balasam.oasis.common.query.core.definition.QueryDefinition;

/**
 * Main interface for executing queries
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
}