package com.balsam.oasis.common.registry.engine.sql.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.execution.QueryContext.Filter;

/**
 * Consolidated SQL building utilities.
 * Combines functionality from SqlUtils, FilterUtils, SortUtils, CriteriaUtils, and PaginationUtils.
 */
public class SqlBuilderUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SqlBuilderUtils.class);
    private static final Pattern BIND_PARAM_PATTERN = Pattern.compile(":(\\w+)");
    
    // SQL placeholder operations (from SqlUtils)
    
    public static String replacePlaceholder(String sql, String placeholder, String replacement) {
        return sql.replace("--" + placeholder, replacement != null ? replacement : "");
    }
    
    public static String cleanPlaceholders(String sql) {
        return sql.replaceAll("--\\w+", "");
    }
    
    public static String wrapForCount(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") count_query";
    }
    
    public static Map<String, Object> extractBindParams(String sql, Map<String, Object> allParams) {
        Map<String, Object> bindParams = new HashMap<>();
        Matcher matcher = BIND_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (allParams.containsKey(paramName)) {
                bindParams.put(paramName, allParams.get(paramName));
            }
        }
        return bindParams;
    }
    
    public static boolean hasBindParameter(String sql, String paramName) {
        Pattern pattern = Pattern.compile(":" + Pattern.quote(paramName) + "\\b");
        return pattern.matcher(sql).find();
    }
    
    // Filter operations (from FilterUtils)
    
    public static String applyFilters(String sql, QueryContext context, Map<String, Object> params) {
        if (context.getFilters() == null || context.getFilters().isEmpty()) {
            return sql;
        }
        
        StringBuilder filterClause = new StringBuilder();
        int paramIndex = 0;
        
        for (Filter filter : context.getFilters().values()) {
            AttributeDef<?> attr = context.getDefinition().getAttribute(filter.getAttribute());
            if (attr == null || !attr.filterable()) {
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
            sql = "SELECT * FROM (" + sql + ") WHERE " + filterClause.toString();
        }
        
        return sql;
    }
    
    private static String buildFilterCondition(Filter filter, AttributeDef<?> attr,
            Map<String, Object> params, int index) {
        String column = attr.aliasName() != null ? attr.aliasName() : attr.name();
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
                throw new QueryException(QueryException.ErrorCode.PARAMETER_ERROR,
                    "Unsupported filter operator: " + filter.getOperator());
        }
    }
    
    // Sort operations (from SortUtils)
    
    public static String applySorting(String sql, QueryContext context) {
        if (context.getSorts() == null || context.getSorts().isEmpty()) {
            return sql;
        }
        
        String orderByClause = context.getSorts().stream()
            .map(sort -> {
                AttributeDef<?> attr = context.getDefinition().getAttribute(sort.getAttribute());
                if (attr == null || !attr.sortable()) {
                    log.warn("Attribute {} is not sortable or does not exist", sort.getAttribute());
                    return null;
                }
                String column = attr.aliasName() != null ? attr.aliasName() : attr.name();
                return column + " " + sort.getDirection().name();
            })
            .filter(s -> s != null)
            .collect(Collectors.joining(", "));
        
        if (!orderByClause.isEmpty()) {
            sql = replacePlaceholder(sql, "orderBy", "ORDER BY " + orderByClause);
        }
        
        return sql;
    }
    
    // Criteria operations (from CriteriaUtils)
    
    public static String applyCriteria(String sql, QueryContext context, Map<String, Object> params) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (definition.getCriteria() == null || definition.getCriteria().isEmpty()) {
            return sql;
        }
        
        for (Map.Entry<String, CriteriaDef> entry : definition.getCriteria().entrySet()) {
            CriteriaDef criteria = entry.getValue();
            if (criteria.condition() == null || criteria.condition().test(context)) {
                String placeholder = entry.getKey();
                String sqlFragment = buildCriteriaSql(criteria, context, params);
                sql = replacePlaceholder(sql, placeholder, sqlFragment);
            }
        }
        
        return sql;
    }
    
    private static String buildCriteriaSql(CriteriaDef criteria, QueryContext context, 
            Map<String, Object> params) {
        // CriteriaDef is a record with sql field
        return criteria.sql();
    }
    
    // Pagination operations (from PaginationUtils)
    
    public static String applyPagination(String sql, QueryContext context) {
        // Check if context has pagination
        if (context.getPagination() == null) {
            return sql;
        }
        
        int offset = context.getPagination().getOffset();
        Integer limit = context.getPagination().getLimit();
        
        return applyOracle11gPagination(sql, offset, limit);
    }
    
    private static String applyOracle11gPagination(String sql, int offset, Integer limit) {
        if (limit == null) {
            return sql;
        }
        
        StringBuilder paginated = new StringBuilder();
        paginated.append("SELECT * FROM (");
        paginated.append("SELECT query_.*, ROWNUM rnum_ FROM (");
        paginated.append(sql);
        paginated.append(") query_ WHERE ROWNUM <= ").append(offset + limit);
        paginated.append(") WHERE rnum_ > ").append(offset);
        
        return paginated.toString();
    }
    
}