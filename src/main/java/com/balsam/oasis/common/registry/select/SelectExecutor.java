package com.balsam.oasis.common.registry.select;

/**
 * Interface for executing select queries for dropdown/select components.
 * Completely separate from QueryExecutor to maintain clean separation.
 */
public interface SelectExecutor {

    /**
     * Create a select execution for the given select name
     * 
     * @param selectName the name of the registered select definition
     * @return a SelectExecution instance for building and executing the query
     */
    SelectExecution select(String selectName);

    /**
     * Create a select execution for the given select definition
     * 
     * @param definition the select definition
     * @return a SelectExecution instance for building and executing the query
     */
    SelectExecution select(SelectDefinition definition);

    /**
     * Prepare a select execution that can be reused multiple times
     * 
     * @param definition the select definition
     * @return a reusable SelectExecution instance
     */
    SelectExecution prepare(SelectDefinition definition);
}