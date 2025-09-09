package com.balsam.oasis.common.registry.web.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

/**
 * Parsed query request from HTTP parameters
 */
@Data
@Builder
public class QueryRequest {
    private Map<String, Object> params;
    private Map<String, QueryContext.Filter> filters;
    private List<QueryContext.SortSpec> sorts;
    private int start;
    private int end;
    private String metadataLevel;
    private Set<String> selectedFields;
}