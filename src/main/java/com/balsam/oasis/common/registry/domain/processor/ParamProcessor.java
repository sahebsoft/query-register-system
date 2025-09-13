package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

@FunctionalInterface
public interface ParamProcessor<T> extends QueryProcessor {
    T process(Object value, QueryContext context);
}
