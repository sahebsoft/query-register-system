package com.balsam.oasis.common.query.processor;

import com.balsam.oasis.common.query.core.execution.QueryContext;

/**
 * Processor executed before query execution
 */
@FunctionalInterface
public interface PreProcessor {
    /**
     * Process the context before query execution
     * 
     * @param context the query context to process
     */
    void process(QueryContext context);
}