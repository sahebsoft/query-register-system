package com.balasam.oasis.common.query.processor;

import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.core.execution.QueryContext;

/**
 * Calculator for computed/calculated field values
 */
@FunctionalInterface
public interface Calculator {
    /**
     * Calculate a value based on row data and context
     * @param value the current value (may be null)
     * @param row the current row
     * @param context the query context
     * @return the calculated value
     */
    Object calculate(Object value, Row row, QueryContext context);
}