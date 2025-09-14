package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QueryRow;

@FunctionalInterface
public interface RowProcessor {
    QueryRow process(QueryRow row, QueryContext context);
}