package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

@FunctionalInterface
public interface PostProcessor extends QueryProcessor {
    QueryData process(QueryData result, QueryContext context);

    @Override
    default Object process(Object input, QueryContext context) {
        if (!(input instanceof QueryData)) {
            throw new IllegalArgumentException("Post processor requires QueryData input");
        }
        return process((QueryData) input, context);
    }
}