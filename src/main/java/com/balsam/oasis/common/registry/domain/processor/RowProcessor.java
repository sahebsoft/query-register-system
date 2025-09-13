package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QueryRow;

@FunctionalInterface
public interface RowProcessor extends QueryProcessor {
    QueryRow process(QueryRow row, QueryContext context);

    @Override
    default Object process(Object input, QueryContext context) {
        if (!(input instanceof QueryRow)) {
            throw new IllegalArgumentException("Row processor requires QueryRow input");
        }
        return process((QueryRow) input, context);
    }
}