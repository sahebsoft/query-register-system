package com.balsam.oasis.common.registry.select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Rich context for select execution similar to QueryContext.
 * Holds parameters, search criteria, pagination, and execution metadata.
 */
@Data
@Builder
public class SelectContext {

    private SelectDefinition definition;

    @Builder.Default
    private Map<String, Object> params = new HashMap<>();

    // Select-specific fields
    private List<String> ids;
    private String searchTerm;

    // Pagination
    private Pagination pagination;

    // Security context
    private Object securityContext;

    // Metadata and audit
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    @Builder.Default
    private List<AppliedCriteria> appliedCriteria = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    private Long startTime;
    private Long endTime;

    @Builder.Default
    private boolean includeMetadata = true;

    @Builder.Default
    private boolean auditEnabled = true;

    @Builder.Default
    private boolean cacheEnabled = true;

    private String cacheKey;

    private Integer totalCount;

    @Data
    @Builder
    public static class Pagination {
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
    }

    @Data
    @Builder
    public static class AppliedCriteria {
        private String name;
        private String sql;
        private Map<String, Object> params;
        private String reason;
    }

    // Utility methods
    public void setParam(String name, Object value) {
        this.params.put(name, value);
    }

    public Object getParam(String name) {
        return params.get(name);
    }

    public boolean hasParam(String name) {
        return params.containsKey(name) && params.get(name) != null;
    }

    public boolean hasIds() {
        return ids != null && !ids.isEmpty();
    }

    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }

    public boolean hasPagination() {
        return pagination != null;
    }

    public void startExecution() {
        this.startTime = System.currentTimeMillis();
    }

    public void endExecution() {
        this.endTime = System.currentTimeMillis();
    }

    public long getExecutionTime() {
        if (startTime != null && endTime != null) {
            return endTime - startTime;
        }
        return 0;
    }

    public void addAppliedCriteria(String name, String sql, Map<String, Object> params, String reason) {
        appliedCriteria.add(AppliedCriteria.builder()
                .name(name)
                .sql(sql)
                .params(params)
                .reason(reason)
                .build());
    }
}