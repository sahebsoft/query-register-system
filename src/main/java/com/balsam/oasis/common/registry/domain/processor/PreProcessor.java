package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

@FunctionalInterface
public interface PreProcessor {
    void process(QueryContext context);
}