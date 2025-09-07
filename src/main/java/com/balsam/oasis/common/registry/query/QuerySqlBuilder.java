package com.balsam.oasis.common.registry.query;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.balsam.oasis.common.registry.base.BaseSqlBuilder;
import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.exception.QueryExecutionException;
import com.balsam.oasis.common.registry.shared.PaginationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds dynamic SQL from query definition and context.
 * Extends BaseSqlBuilder for common SQL operations.
 */
public class QuerySqlBuilder extends BaseSqlBuilder<QueryDefinition, QueryContext> {

    private static final Logger log = LoggerFactory.getLogger(QuerySqlBuilder.class);

    public QuerySqlBuilder(String dialectName) {
        super(dialectName != null ? dialectName : PaginationUtils.ORACLE_11G);
        log.info("SqlBuilder initialized with database dialect: {}", dialect);
    }

    public QuerySqlBuilder() {
        this(PaginationUtils.ORACLE_11G);
    }

    @Override
    protected String applyCustomModifications(String sql, QueryContext context, Map<String, Object> bindParams) {
        // Apply filters and sorting specific to QueryContext
        if (context.getFilters() != null && !context.getFilters().isEmpty()) {
            sql = applyFilters(sql, context, bindParams);
        }

        if (context.getSorts() != null && !context.getSorts().isEmpty()) {
            sql = applySorting(sql, context);
        }

        return sql;
    }

    private String applyFilters(String sql, QueryContext context, Map<String, Object> params) {
        StringBuilder filterClause = new StringBuilder();
        int paramIndex = 0;

        for (QueryContext.Filter filter : context.getFilters().values()) {
            AttributeDef<?> attr = context.getDefinition().getAttribute(filter.getAttribute());
            if (attr == null || !attr.isFilterable()) {
                log.warn("Attribute {} is not filterable or does not exist", filter.getAttribute());
                continue;
            }

            if (filterClause.length() > 0) {
                filterClause.append(" AND ");
            }

            String condition = buildFilterCondition(filter, attr, params, paramIndex++);
            filterClause.append(condition);
        }

        if (filterClause.length() > 0) {
            // Check if there's a --filters placeholder
            if (sql.contains("--filters")) {
                sql = sql.replace("--filters", " AND " + filterClause.toString());
            } else {
                // Wrap the query to add filters
                sql = "SELECT * FROM (" + sql + ") WHERE " + filterClause.toString();
            }
        }

        return sql;
    }

    private String buildFilterCondition(QueryContext.Filter filter, AttributeDef<?> attr,
            Map<String, Object> params, int index) {
        String column = attr.getAliasName() != null ? attr.getAliasName() : attr.getName();
        String paramName = "filter_" + filter.getAttribute() + "_" + index;

        switch (filter.getOperator()) {
            case EQUALS:
                params.put(paramName, filter.getValue());
                return column + " = :" + paramName;
            case NOT_EQUALS:
                params.put(paramName, filter.getValue());
                return column + " != :" + paramName;
            case LIKE:
                params.put(paramName, filter.getValue());
                return "UPPER(" + column + ") LIKE UPPER(:" + paramName + ")";
            case NOT_LIKE:
                params.put(paramName, filter.getValue());
                return "UPPER(" + column + ") NOT LIKE UPPER(:" + paramName + ")";
            case IN:
                params.put(paramName, filter.getValues() != null ? filter.getValues() : filter.getValue());
                return column + " IN (:" + paramName + ")";
            case NOT_IN:
                params.put(paramName, filter.getValues() != null ? filter.getValues() : filter.getValue());
                return column + " NOT IN (:" + paramName + ")";
            case GREATER_THAN:
                params.put(paramName, filter.getValue());
                return column + " > :" + paramName;
            case GREATER_THAN_OR_EQUAL:
                params.put(paramName, filter.getValue());
                return column + " >= :" + paramName;
            case LESS_THAN:
                params.put(paramName, filter.getValue());
                return column + " < :" + paramName;
            case LESS_THAN_OR_EQUAL:
                params.put(paramName, filter.getValue());
                return column + " <= :" + paramName;
            case BETWEEN:
                params.put(paramName + "_1", filter.getValue());
                params.put(paramName + "_2", filter.getValue2());
                return column + " BETWEEN :" + paramName + "_1 AND :" + paramName + "_2";
            case IS_NULL:
                return column + " IS NULL";
            case IS_NOT_NULL:
                return column + " IS NOT NULL";
            default:
                throw new QueryExecutionException("Unsupported filter operator: " + filter.getOperator());
        }
    }

    private String applySorting(String sql, QueryContext context) {
        List<String> orderClauses = context.getSorts().stream()
                .map(sort -> {
                    AttributeDef<?> attr = context.getDefinition().getAttribute(sort.getAttribute());
                    if (attr == null || !attr.isSortable()) {
                        log.warn("Attribute {} is not sortable or does not exist", sort.getAttribute());
                        return null;
                    }
                    String column = attr.getAliasName() != null ? attr.getAliasName() : attr.getName();
                    return column + " " + sort.getDirection().name();
                })
                .filter(clause -> clause != null)
                .collect(Collectors.toList());

        if (!orderClauses.isEmpty()) {
            String orderByClause = String.join(", ", orderClauses);

            // Check if there's an --orderBy placeholder
            if (sql.contains("--orderBy")) {
                sql = sql.replace("--orderBy", " ORDER BY " + orderByClause);
            } else {
                sql = sql + " ORDER BY " + orderByClause;
            }
        }

        return sql;
    }

    public String getDatabaseDialect() {
        return dialect;
    }
}