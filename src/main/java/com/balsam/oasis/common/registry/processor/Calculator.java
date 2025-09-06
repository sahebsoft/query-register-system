package com.balsam.oasis.common.registry.processor;

import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.query.QueryContext;

/**
 * Calculator interface for computing transient attribute values.
 * Used with TransientAttributeDef to calculate values based on row data and
 * query context.
 * 
 * @param <T> The type of value this calculator produces
 */
@FunctionalInterface
public interface Calculator<T> {

    /**
     * Calculate a value based on the current row and query context.
     * 
     * @param row     The current row containing data from the query
     * @param context The query execution context
     * @return The calculated value of type T
     */
    T calculate(Row row, QueryContext context);
}