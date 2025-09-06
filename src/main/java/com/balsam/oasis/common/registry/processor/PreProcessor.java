package com.balsam.oasis.common.registry.processor;

import com.balsam.oasis.common.registry.query.QueryContext;

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