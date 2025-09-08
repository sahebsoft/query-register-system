package com.balsam.oasis.common.registry.base;

import java.util.Map;

import com.balsam.oasis.common.registry.domain.common.Pagination;

public abstract class BaseExecution<C extends BaseContext<?>, R> {

    protected final C context;

    protected BaseExecution(C context) {
        this.context = context;
    }

    public BaseExecution<C, R> withParam(String name, Object value) {
        context.setParam(name, value);
        return this;
    }

    public BaseExecution<C, R> withParams(Map<String, Object> params) {
        if (params != null) {
            params.forEach(context::setParam);
        }
        return this;
    }

    public BaseExecution<C, R> withPagination(int start, int end) {
        context.setPagination(Pagination.builder()
                .start(start)
                .end(end)
                .build());
        return this;
    }

    public BaseExecution<C, R> withCacheEnabled(boolean enabled) {
        context.setCacheEnabled(enabled);
        return this;
    }

    public BaseExecution<C, R> withMetadataEnabled(boolean enabled) {
        context.setIncludeMetadata(enabled);
        return this;
    }

    public abstract R execute();

    public abstract BaseExecution<C, R> validate();
}