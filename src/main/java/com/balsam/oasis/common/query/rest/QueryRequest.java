package com.balsam.oasis.common.query.rest;

import com.balsam.oasis.common.query.core.execution.QueryContext;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
}