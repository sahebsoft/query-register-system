package com.balasam.oasis.common.query.processor;

import com.balasam.oasis.common.query.core.result.QueryResult;
import com.balasam.oasis.common.query.core.execution.QueryContext;

/**
 * Processor executed after query execution
 */
@FunctionalInterface
public interface PostProcessor {
    /**
     * Process the query result after execution
     * @param result the query result
     * @param context the query context
     * @return the processed result
     */
    QueryResult process(QueryResult result, QueryContext context);
}