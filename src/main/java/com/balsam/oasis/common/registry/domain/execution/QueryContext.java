package com.balsam.oasis.common.registry.domain.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.Pagination;
import com.balsam.oasis.common.registry.domain.definition.FilterOp;
import com.balsam.oasis.common.registry.domain.definition.SortDir;

import lombok.Builder;
import lombok.Data;

/**
 * Simple context for query execution containing runtime parameters
 */
@Data
@Builder
public class QueryContext {

    protected QueryDefinitionBuilder definition;

    @Builder.Default
    protected Map<String, Object> params = new HashMap<>();

    protected Pagination pagination;

    protected Long startTime;
    protected Long endTime;

    @Builder.Default
    protected boolean includeMetadata = true;

    protected Integer totalCount;

    @Builder.Default
    private Map<String, Filter> filters = new LinkedHashMap<>();

    @Builder.Default
    private List<SortSpec> sorts = new ArrayList<>();

    private Object securityContext;

    @Data
    @Builder
    public static class Filter {
        private String attribute;
        private FilterOp operator;
        private Object value;
        private Object value2; // For BETWEEN
        private List<Object> values; // For IN

        public boolean requiresTwoValues() {
            return operator == FilterOp.BETWEEN;
        }

        public boolean hasMultipleValues() {
            return operator == FilterOp.IN || operator == FilterOp.NOT_IN;
        }
    }

    @Data
    @Builder
    public static class SortSpec {
        private String attribute;
        private SortDir direction;
    }

    // Helper methods
    public void addParam(String name, Object value) {
        params.put(name, value);
    }

    public void addFilter(String attribute, FilterOp operator, Object value) {
        filters.put(attribute, Filter.builder()
                .attribute(attribute)
                .operator(operator)
                .value(value)
                .build());
    }

    public void addFilter(String attribute, FilterOp operator, Object value1, Object value2) {
        filters.put(attribute, Filter.builder()
                .attribute(attribute)
                .operator(operator)
                .value(value1)
                .value2(value2)
                .build());
    }

    public void addFilter(String attribute, FilterOp operator, List<Object> values) {
        filters.put(attribute, Filter.builder()
                .attribute(attribute)
                .operator(operator)
                .values(values)
                .build());
    }

    public void addSort(String attribute, SortDir direction) {
        sorts.add(SortSpec.builder()
                .attribute(attribute)
                .direction(direction)
                .build());
    }

    public Object getParam(String name) {
        return params.get(name);
    }

    public boolean hasParam(String name) {
        return params.containsKey(name) && params.get(name) != null;
    }

    public boolean hasPagination() {
        return pagination != null;
    }

    public boolean hasSorts() {
        return sorts != null && !sorts.isEmpty();
    }

    public void startExecution() {
        this.startTime = System.currentTimeMillis();
    }

    public void endExecution() {
        this.endTime = System.currentTimeMillis();
    }

    public long getExecutionTime() {
        return (startTime != null && endTime != null) ? endTime - startTime : 0;
    }
}