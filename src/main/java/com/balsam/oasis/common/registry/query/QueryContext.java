package com.balsam.oasis.common.registry.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.base.BaseContext;
import com.balsam.oasis.common.registry.core.definition.FilterOp;
import com.balsam.oasis.common.registry.core.definition.SortDir;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Mutable context for query execution containing all runtime parameters
 */
@Data
@SuperBuilder
public class QueryContext extends BaseContext<QueryDefinition> {


    @Builder.Default
    private Map<String, Filter> filters = new LinkedHashMap<>();

    @Builder.Default
    private List<SortSpec> sorts = new ArrayList<>();

    private Object securityContext;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

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

    @Data
    @Builder
    public static class QueryPagination {
        private int start;
        private int end;
        private Integer limit;
        private Integer offset;
        private int total;
        private boolean hasNext;
        private boolean hasPrevious;

        public int getPageSize() {
            if (limit != null) {
                return limit;
            }
            return end - start;
        }

        public static QueryPagination fromStartEnd(int start, int end) {
            return QueryPagination.builder()
                    .start(start)
                    .end(end)
                    .build();
        }

        public static QueryPagination fromOffsetLimit(int offset, int limit) {
            return QueryPagination.builder()
                    .offset(offset)
                    .limit(limit)
                    .start(offset)
                    .end(offset + limit)
                    .build();
        }
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

    public void recordAppliedCriteria(String name, String sql) {
        addAppliedCriteria(BaseContext.AppliedCriteria.builder()
                .name(name)
                .sql(sql)
                .build());
    }

    public <T> T getParam(String name, Class<T> type) {
        Object value = getParam(name);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public boolean hasFilter(String attribute) {
        return filters.containsKey(attribute);
    }

    public boolean hasSorts() {
        return sorts != null && !sorts.isEmpty();
    }

}