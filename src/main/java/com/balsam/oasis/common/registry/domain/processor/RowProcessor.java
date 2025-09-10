package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QueryRow;

/**
 * Processor for individual rows during query execution
 */
@FunctionalInterface
public interface RowProcessor {
    /**
     * Process a single row
     * 
     * @param row     the row to process
     * @param context the query context
     * @return the processed row (can be the same or a new instance)
     */
    QueryRow process(QueryRow row, QueryContext context);
}