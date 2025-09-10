package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

/**
 * Processor for parameter validation and transformation.
 * Applied before query execution when no row is available.
 */
@FunctionalInterface
public interface ParamProcessor {

    /**
     * Process a parameter value with query context.
     * 
     * @param value   the parameter value
     * @param context the query context
     * @return processed/validated value
     * @throws IllegalArgumentException if validation fails
     */
    Object process(Object value, QueryContext context);
}
