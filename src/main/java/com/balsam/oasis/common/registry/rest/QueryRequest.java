package com.balsam.oasis.common.registry.rest;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.query.QueryContext;

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