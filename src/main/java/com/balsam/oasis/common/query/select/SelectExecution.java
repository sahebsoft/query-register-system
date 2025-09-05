package com.balsam.oasis.common.query.select;

import com.balsam.oasis.common.query.rest.LovResponse;

import java.util.List;
import java.util.Map;

/**
 * Fluent interface for building and executing select queries.
 * Supports fetching by IDs, search, parameters, and pagination.
 */
public interface SelectExecution {
    
    /**
     * Fetch specific items by their IDs.
     * This will auto-wrap the query with WHERE value_column IN (:ids)
     * @param ids the list of IDs to fetch
     * @return this execution for chaining
     */
    SelectExecution withIds(List<String> ids);
    
    /**
     * Fetch specific items by a single ID.
     * @param id the ID to fetch
     * @return this execution for chaining
     */
    SelectExecution withId(String id);
    
    /**
     * Add search criteria.
     * If searchCriteria is defined, it will be used.
     * Otherwise, the query will be auto-wrapped with WHERE label_column LIKE :search
     * @param searchTerm the search term (will be wrapped with % automatically)
     * @return this execution for chaining
     */
    SelectExecution withSearch(String searchTerm);
    
    /**
     * Add a single parameter
     * @param name parameter name
     * @param value parameter value
     * @return this execution for chaining
     */
    SelectExecution withParam(String name, Object value);
    
    /**
     * Add multiple parameters
     * @param params map of parameter names to values
     * @return this execution for chaining
     */
    SelectExecution withParams(Map<String, Object> params);
    
    /**
     * Set pagination bounds
     * @param start the starting index (inclusive)
     * @param end the ending index (exclusive)
     * @return this execution for chaining
     */
    SelectExecution withPagination(int start, int end);
    
    /**
     * Validate the execution parameters against the select definition
     * @return this execution for chaining
     * @throws com.balsam.oasis.common.query.exception.QueryValidationException if validation fails
     */
    SelectExecution validate();
    
    /**
     * Execute the select query and return the results
     * @return LovResponse containing the data
     * @throws com.balsam.oasis.common.query.exception.QueryExecutionException if execution fails
     */
    LovResponse execute();
    
    /**
     * Reset this execution to its initial state for reuse
     * @return this execution for chaining
     */
    SelectExecution reset();
}