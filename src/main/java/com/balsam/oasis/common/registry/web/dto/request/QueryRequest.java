package com.balsam.oasis.common.registry.web.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.common.Pagination;

/**
 * Parsed query request from HTTP parameters
 */
@Data
@Builder
public class QueryRequest {
    private Map<String, Object> params;
    private Map<String, QueryContext.Filter> filters;
    private List<QueryContext.SortSpec> sorts;
    private Pagination pagination;
    private String metadataLevel;
    private Set<String> selectedFields;

    // Helper method to create pagination
    public static class QueryRequestBuilder {
        public QueryRequestBuilder pagination(Integer start, Integer end) {
            if (start == null || end == null)
                return this;
            this.pagination = Pagination.builder()
                    .start(start)
                    .end(end)
                    .build();
            return this;
        }
    }
}