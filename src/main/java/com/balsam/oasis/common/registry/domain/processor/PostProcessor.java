package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

@FunctionalInterface
public interface PostProcessor {
    QueryData process(QueryData result, QueryContext context);
}