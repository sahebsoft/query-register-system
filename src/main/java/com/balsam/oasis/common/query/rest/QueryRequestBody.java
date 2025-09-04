package com.balsam.oasis.common.query.rest;

import com.balsam.oasis.common.query.core.execution.QueryContext;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST query execution (JSON only)
 */
@Data
public class QueryRequestBody {
    private Map<String, Object> params;
    private Map<String, QueryContext.Filter> filters;
    private List<QueryContext.SortSpec> sorts;
    private int start = 0;
    private int end = 50;
    private boolean includeMetadata = true;
}