package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QueryRow;

@FunctionalInterface
public interface Calculator<T> extends QueryProcessor {
    T calculate(QueryRow row, QueryContext context);

    @Override
    @SuppressWarnings("unchecked")
    default Object process(Object input, QueryContext context) {
        if (!(input instanceof QueryRow)) {
            throw new IllegalArgumentException("Calculator processor requires QueryRow input");
        }
        return calculate((QueryRow) input, context);
    }
}