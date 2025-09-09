package com.balsam.oasis.common.registry.domain.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.AppliedCriteria;
import com.balsam.oasis.common.registry.domain.common.Pagination;
import com.balsam.oasis.common.registry.domain.definition.FilterOp;
import com.balsam.oasis.common.registry.domain.definition.SortDir;

import lombok.Builder;
import lombok.Data;

/**
 * Mutable context for query execution containing all runtime parameters
 */
@Data
@Builder
public class QueryContext {
    // Base fields from BaseContext
    protected QueryDefinition definition;

    @Builder.Default
    protected Map<String, Object> params = new HashMap<>();

    protected Pagination pagination;

    @Builder.Default
    protected Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    protected List<AppliedCriteria> appliedCriteria = new ArrayList<>();

    protected Long startTime;
    protected Long endTime;

    @Builder.Default
    protected boolean includeMetadata = true;

    @Builder.Default
    protected boolean auditEnabled = true;

    @Builder.Default
    protected boolean cacheEnabled = true;

    protected String cacheKey;
    protected Integer totalCount;

    @Builder.Default
    private Map<String, Filter> filters = new LinkedHashMap<>();

    @Builder.Default
    private List<SortSpec> sorts = new ArrayList<>();

    private Object securityContext;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    private Set<String> selectedFields; // null or empty = all fields

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

    public void recordAppliedCriteria(String name, String sql) {
        addAppliedCriteria(AppliedCriteria.builder()
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

    // Methods from BaseContext
    public void startExecution() {
        this.startTime = System.currentTimeMillis();
    }

    public void endExecution() {
        this.endTime = System.currentTimeMillis();
    }

    public long getExecutionTime() {
        return (startTime != null && endTime != null) ? endTime - startTime : 0;
    }

    public void setParam(String name, Object value) {
        params.put(name, value);
    }

    public Object getParam(String name) {
        return params.get(name);
    }

    public boolean hasParam(String name) {
        return params.containsKey(name) && params.get(name) != null;
    }

    public void addAppliedCriteria(AppliedCriteria criteria) {
        appliedCriteria.add(criteria);
    }

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public boolean hasPagination() {
        return pagination != null;
    }

    public void selectFields(String... fields) {
        this.selectedFields = new HashSet<>(Arrays.asList(fields));
    }

    public void selectFields(Set<String> fields) {
        this.selectedFields = fields != null ? new HashSet<>(fields) : null;
    }

    public boolean isFieldSelected(String fieldName) {
        return selectedFields == null || selectedFields.isEmpty() || 
               selectedFields.contains(fieldName);
    }

    /**
     * Returns a new context with the updated definition.
     * Used when we need to update the definition with metadata cache.
     */
    public QueryContext withDefinition(QueryDefinition definition) {
        QueryContext newContext = QueryContext.builder()
                .definition(definition)
                .params(new HashMap<>(this.params))
                .pagination(this.pagination)
                .sorts(this.sorts)
                .filters(this.filters)
                .appliedCriteria(new ArrayList<>(this.appliedCriteria))
                .metadata(new HashMap<>(this.metadata))
                .startTime(this.startTime)
                .endTime(this.endTime)
                .build();
        newContext.setSelectedFields(this.selectedFields);
        return newContext;
    }
}