package com.balsam.oasis.common.registry.processor;

import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.query.QueryContext;

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
    Row process(Row row, QueryContext context);
}