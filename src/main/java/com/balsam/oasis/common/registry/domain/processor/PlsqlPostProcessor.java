package com.balsam.oasis.common.registry.domain.processor;

import java.util.Map;

import com.balsam.oasis.common.registry.domain.execution.PlsqlContext;

@FunctionalInterface
public interface PlsqlPostProcessor {
    Map<String, Object> process(Map<String, Object> result, PlsqlContext context);
}