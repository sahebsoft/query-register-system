package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

@FunctionalInterface
public interface PreProcessor extends QueryProcessor {
    void process(QueryContext context);

    @Override
    default Object process(Object input, QueryContext context) {
        process(context);
        return null; // Pre-processors don't return values
    }
}