package com.balsam.oasis.common.registry.engine.sql.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.exception.QueryExecutionException;

/**
 * Utility class for applying filters to SQL queries.
 * Handles dynamic filter application based on QueryContext filters.
 */
public class FilterUtils {

    private static final Logger log = LoggerFactory.getLogger(FilterUtils.class);

    /**
     * Apply filters from QueryContext to the SQL query.
     * 
     * @param sql     The SQL query
     * @param context The query context containing filters
     * @param params  The parameter map to add filter parameters
     * @return The SQL with filters applied
     */
    public static String applyFilters(String sql, QueryContext context, Map<String, Object> params) {
        if (context.getFilters() == null || context.getFilters().isEmpty()) {
            return sql;
        }

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
            // Wrap the query to add filters
            sql = "SELECT * FROM (" + sql + ") WHERE " + filterClause.toString();
        }

        return sql;
    }

    /**
     * Build a filter condition for a specific filter and attribute.
     * 
     * @param filter The filter to apply
     * @param attr   The attribute being filtered
     * @param params The parameter map
     * @param index  The parameter index for uniqueness
     * @return The SQL condition string
     */
    private static String buildFilterCondition(QueryContext.Filter filter, AttributeDef<?> attr,
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
}