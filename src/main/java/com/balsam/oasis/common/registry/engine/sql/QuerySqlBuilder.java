package com.balsam.oasis.common.registry.engine.sql;

import java.util.HashMap;
import java.util.Map;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.SqlResult;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.util.CriteriaUtils;
import com.balsam.oasis.common.registry.util.FilterUtils;
import com.balsam.oasis.common.registry.util.PaginationUtils;
import com.balsam.oasis.common.registry.util.SortUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds dynamic SQL from query definition and context.
 */
public class QuerySqlBuilder {

    private static final Logger log = LoggerFactory.getLogger(QuerySqlBuilder.class);

    protected final String dialect;

    public QuerySqlBuilder(String dialectName) {
        this.dialect = dialectName != null ? dialectName : PaginationUtils.ORACLE_11G;
        log.info("SqlBuilder initialized with database dialect: {}", dialect);
    }

    public QuerySqlBuilder() {
        this(PaginationUtils.ORACLE_11G);
    }

    public SqlResult build(QueryContext context) {
        QueryDefinition definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = CriteriaUtils.applyCriteria(sql, definition.getCriteria(), context, bindParams, true);

        sql = applyCustomModifications(sql, context, bindParams);

        if (context.hasPagination()) {
            sql = PaginationUtils.applyPagination(sql, context.getPagination(), dialect, bindParams);
        }

        sql = SqlUtils.cleanPlaceholders(sql);

        return new SqlResult(sql, bindParams);
    }

    public String buildCountQuery(QueryContext context) {
        QueryDefinition definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = CriteriaUtils.applyCriteria(sql, definition.getCriteria(), context, bindParams, false);

        sql = applyCustomModifications(sql, context, bindParams);

        sql = SqlUtils.cleanPlaceholders(sql);

        return SqlUtils.wrapForCount(sql);
    }

    protected String applyCustomModifications(String sql, QueryContext context, Map<String, Object> bindParams) {
        // Apply filters and sorting using utility classes
        sql = FilterUtils.applyFilters(sql, context, bindParams);
        sql = SortUtils.applySorting(sql, context);
        return sql;
    }

    public String getDatabaseDialect() {
        return dialect;
    }
}