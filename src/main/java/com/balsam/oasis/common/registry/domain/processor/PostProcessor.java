package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

@FunctionalInterface
public interface PostProcessor extends QueryProcessor {
    QueryResult process(QueryResult result, QueryContext context);

    @Override
    default Object process(Object input, QueryContext context) {
        if (!(input instanceof QueryResult)) {
            throw new IllegalArgumentException("Post processor requires QueryResult input");
        }
        return process((QueryResult) input, context);
    }
}