package com.balasam.oasis.common.query.processor;

import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.core.execution.QueryContext;

/**
 * Processor for individual rows during query execution
 */
@FunctionalInterface
public interface RowProcessor {
    /**
     * Process a single row
     * @param row the row to process
     * @param context the query context
     * @return the processed row (can be the same or a new instance)
     */
    Row process(Row row, QueryContext context);
}