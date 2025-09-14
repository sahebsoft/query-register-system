package com.balsam.oasis.common.registry.domain.execution;

import java.util.Map;

import com.balsam.oasis.common.registry.builder.PlsqlDefinitionBuilder;
import com.balsam.oasis.common.registry.engine.plsql.PlsqlExecutorImpl;

public class PlsqlExecution {
    private final PlsqlContext context;
    private final PlsqlExecutorImpl executor;

    public PlsqlExecution(PlsqlDefinitionBuilder definition, PlsqlExecutorImpl executor) {
        this.executor = executor;
        this.context = PlsqlContext.builder()
                .definition(definition)
                .build();
    }

    public PlsqlExecution withParam(String name, Object value) {
        context.addParam(name, value);
        return this;
    }

    public PlsqlExecution withParams(Map<String, Object> params) {
        params.forEach(context::addParam);
        return this;
    }

    public PlsqlExecution includeMetadata(boolean include) {
        context.setIncludeMetadata(include);
        return this;
    }

    public Map<String, Object> execute() {
        return executor.doExecute(context);
    }

    public java.util.concurrent.CompletableFuture<Map<String, Object>> executeAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(this::execute);
    }
}