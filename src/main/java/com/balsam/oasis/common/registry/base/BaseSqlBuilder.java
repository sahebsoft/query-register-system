package com.balsam.oasis.common.registry.base;

import com.balsam.oasis.common.registry.engine.sql.SqlUtils;
import com.balsam.oasis.common.registry.util.CriteriaUtils;
import com.balsam.oasis.common.registry.util.PaginationUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseSqlBuilder<D extends BaseDefinition, C extends BaseContext<D>> {

    protected final String dialect;

    protected BaseSqlBuilder(String dialect) {
        this.dialect = dialect != null ? dialect : PaginationUtils.ORACLE_11G;
    }

    public BaseExecutor.SqlResult build(C context) {
        D definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = CriteriaUtils.applyCriteria(sql, definition.getCriteria(), context, bindParams, true);

        sql = applyCustomModifications(sql, context, bindParams);

        if (context.hasPagination()) {
            sql = PaginationUtils.applyPagination(sql, context.getPagination(), dialect, bindParams);
        }

        sql = SqlUtils.cleanPlaceholders(sql);

        return new BaseExecutor.SqlResult(sql, bindParams);
    }

    // Criteria and SQL utility methods are delegated to utility classes
    // See CriteriaUtils and SqlUtils for implementations

    public String buildCountQuery(C context) {
        D definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());

        sql = CriteriaUtils.applyCriteria(sql, definition.getCriteria(), context, bindParams, false);

        sql = applyCustomModifications(sql, context, bindParams);

        sql = SqlUtils.cleanPlaceholders(sql);

        return SqlUtils.wrapForCount(sql);
    }

    protected abstract String applyCustomModifications(String sql, C context,
            Map<String, Object> bindParams);
}