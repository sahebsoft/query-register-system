package com.balasam.oasis.common.query.rest;

import com.balasam.oasis.common.query.core.execution.QueryContext;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST query execution
 */
@Data
public class QueryRequestBody {
    private Map<String, Object> params;
    private Map<String, QueryContext.Filter> filters;
    private List<QueryContext.SortSpec> sorts;
    private int start = 0;
    private int end = 50;
    private boolean includeMetadata = true;
    private String format = "json";
}