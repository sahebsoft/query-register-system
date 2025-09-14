package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

/**
 * Processes parameter values during query execution.
 * <p>
 * Parameter processors ALWAYS receive String values (or null) from the web interface.
 * They are responsible for converting strings to the appropriate target types and
 * performing any validation or transformation logic.
 *
 * @param <T> the target type that this processor returns
 */
@FunctionalInterface
public interface ParamProcessor<T> {
    /**
     * Process a parameter value.
     *
     * @param value the input value - ALWAYS a String or null from HTTP parameters
     * @param context the query execution context for accessing other parameters/state
     * @return the processed value of type T
     * @throws com.balsam.oasis.common.registry.domain.exception.QueryException if the value cannot be processed
     */
    T process(String value, QueryContext context);
}
