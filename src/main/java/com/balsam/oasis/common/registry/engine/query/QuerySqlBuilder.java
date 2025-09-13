package com.balsam.oasis.common.registry.engine.query;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.SqlResult;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.sql.DatabaseDialect;
import com.balsam.oasis.common.registry.engine.sql.util.SqlBuilderUtils;

/**
 * Builds dynamic SQL from query definition and context.
 */
public class QuerySqlBuilder {

    private static final Logger log = LoggerFactory.getLogger(QuerySqlBuilder.class);

    protected final DatabaseDialect dialect;

    public QuerySqlBuilder(String dialectName) {
        this.dialect = dialectName != null ? DatabaseDialect.fromString(dialectName) : DatabaseDialect.ORACLE_11G;
        log.info("SqlBuilder initialized with database dialect: {}", dialect);
    }

    public QuerySqlBuilder() {
        this(DatabaseDialect.ORACLE_11G.name());
    }

    public SqlResult build(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = SqlBuilderUtils.applyCriteria(sql, context, bindParams);
        sql = SqlBuilderUtils.applyFilters(sql, context, bindParams);
        sql = SqlBuilderUtils.applySorting(sql, context);

        if (context.hasPagination() && context.getDefinition().isPaginationEnabled()) {
            sql = SqlBuilderUtils.applyPagination(sql, context, dialect);
        }

        sql = SqlBuilderUtils.cleanPlaceholders(sql);

        return new SqlResult(sql, bindParams);
    }

    public String buildCountQuery(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = SqlBuilderUtils.applyCriteria(sql, context, bindParams);
        sql = SqlBuilderUtils.applyFilters(sql, context, bindParams);
        sql = SqlBuilderUtils.cleanPlaceholders(sql);

        return SqlBuilderUtils.wrapForCount(sql);
    }
}