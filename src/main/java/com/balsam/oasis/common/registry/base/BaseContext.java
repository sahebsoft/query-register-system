package com.balsam.oasis.common.registry.base;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
public abstract class BaseContext<D extends BaseDefinition> {
    protected D definition;
    
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
    
    @Data
    @Builder
    public static class Pagination {
        private int start;
        private int end;
        private int total;
        private boolean hasNext;
        private boolean hasPrevious;
        
        public int getPageSize() {
            return end - start;
        }
        
        public int getOffset() {
            return start;
        }
        
        public int getLimit() {
            return getPageSize();
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
}