package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

@FunctionalInterface
public interface AttributeFormatter<T> extends QueryProcessor {
    String format(T value);

    @Override
    @SuppressWarnings("unchecked")
    default Object process(Object input, QueryContext context) {
        if (input == null) return null;
        return format((T) input);
    }
}