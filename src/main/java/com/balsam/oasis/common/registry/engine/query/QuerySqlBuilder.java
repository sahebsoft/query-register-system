package com.balsam.oasis.common.registry.engine.query;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.SqlResult;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.util.QueryUtils;

/**
 * Builds dynamic SQL from query definition and context.
 * Simplified to support Oracle 11g only.
 */
public class QuerySqlBuilder {

    private static final Logger log = LoggerFactory.getLogger(QuerySqlBuilder.class);

    public QuerySqlBuilder() {
        log.info("SqlBuilder initialized for Oracle 11g");
    }

    public SqlResult build(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = QueryUtils.applyCriteria(sql, context, bindParams);
        sql = QueryUtils.applyFilters(sql, context, bindParams);
        sql = QueryUtils.applySorting(sql, context);

        if (context.hasPagination() && context.getDefinition().isPaginationEnabled()) {
            sql = QueryUtils.applyPagination(sql, context);
        }

        sql = QueryUtils.cleanPlaceholders(sql);

        return new SqlResult(sql, bindParams);
    }

    public String buildCountQuery(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = QueryUtils.applyCriteria(sql, context, bindParams);
        sql = QueryUtils.applyFilters(sql, context, bindParams);
        sql = QueryUtils.cleanPlaceholders(sql);

        return QueryUtils.wrapForCount(sql);
    }

    public String buildCountQueryWithProcessedParams(QueryContext context, Map<String, Object> processedParams) {
        // Build count query reusing already processed parameters to avoid double processing
        QueryDefinitionBuilder definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(processedParams);

        sql = QueryUtils.applyCriteria(sql, context, bindParams);
        sql = QueryUtils.applyFilters(sql, context, bindParams);
        sql = QueryUtils.cleanPlaceholders(sql);

        return QueryUtils.wrapForCount(sql);
    }
}