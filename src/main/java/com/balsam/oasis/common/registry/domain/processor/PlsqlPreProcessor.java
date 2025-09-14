package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.PlsqlContext;

@FunctionalInterface
public interface PlsqlPreProcessor {
    void process(PlsqlContext context);
}