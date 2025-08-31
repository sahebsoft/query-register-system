package com.balasam.oasis.common.query.core.execution;

import com.balasam.oasis.common.query.core.definition.*;
import com.balasam.oasis.common.query.exception.QueryExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builds dynamic SQL from query definition and context
 */
public class SqlBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(SqlBuilder.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("--(\\w+)");
    
    public static class SqlResult {
        private final String sql;
        private final Map<String, Object> params;
        private String baseSql;
        private Map<String, Object> paramsWithoutPagination;
        
        public SqlResult(String sql, Map<String, Object> params) {
            this.sql = sql;
            this.params = params;
            this.baseSql = sql;
            this.paramsWithoutPagination = new HashMap<>(params);
        }
        
        public String getSql() {
            return sql;
        }
        
        public Map<String, Object> getParams() {
            return params;
        }
        
        public String getBaseSql() {
            return baseSql;
        }
        
        public Map<String, Object> getParamsWithoutPagination() {
            return paramsWithoutPagination;
        }
    }
    
    public SqlResult build(QueryContext context) {
        QueryDefinition definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> params = new HashMap<>(context.getParams());
        
        // Apply criteria (including findByKey)
        sql = applyCriteria(sql, context, params);
        
        // Clean up any remaining placeholders from criteria
        sql = removeUnusedPlaceholders(sql);
        
        // Store base SQL for count query
        String baseSql = sql;
        Map<String, Object> baseParams = new HashMap<>(params);
        
        // Wrap the query to apply filters, sorting, and pagination
        sql = wrapQueryForFiltersAndPagination(sql, context, params);
        
        log.debug("Built SQL: {}", sql);
        log.debug("Parameters: {}", params);
        
        SqlResult result = new SqlResult(sql, params);
        result.baseSql = baseSql;
        result.paramsWithoutPagination = baseParams;
        return result;
    }
    
    private String applyCriteria(String sql, QueryContext context, Map<String, Object> params) {
        QueryDefinition definition = context.getDefinition();
        
        if (!definition.hasCriteria()) {
            return sql;
        }
        
        // Sort criteria by priority
        List<CriteriaDef> sortedCriteria = definition.getCriteria().values().stream()
            .sorted(Comparator.comparingInt(CriteriaDef::getPriority))
            .collect(Collectors.toList());
        
        for (CriteriaDef criteria : sortedCriteria) {
            String placeholder = "--" + criteria.getName();
            
            if (!sql.contains(placeholder)) {
                continue;
            }
            
            // Check if criteria should be applied
            boolean shouldApply = true;
            if (criteria.hasCondition()) {
                shouldApply = criteria.getCondition().test(context);
            }
            
            if (shouldApply) {
                // Process context if processor exists
                if (criteria.hasProcessor()) {
                    criteria.getProcessor().apply(context);
                }
                
                // Generate SQL if dynamic
                String criteriaSql = criteria.getSql();
                if (criteria.isDynamic() && criteria.hasGenerator()) {
                    criteriaSql = criteria.getGenerator().apply(context);
                }
                
                // Add bind parameters
                for (String paramName : criteria.getBindParams()) {
                    if (context.hasParam(paramName)) {
                        params.put(paramName, context.getParam(paramName));
                    }
                }
                
                // Replace placeholder
                sql = sql.replace(placeholder, criteriaSql);
                
                // Record applied criteria
                context.recordAppliedCriteria(
                    criteria.getName(),
                    criteriaSql,
                    extractCriteriaParams(criteria, params),
                    criteria.getAppliedReason(),
                    criteria.isSecurityRelated()
                );
            } else {
                // Remove placeholder if criteria not applied
                sql = sql.replace(placeholder, "");
            }
        }
        
        return sql;
    }
    
    private Map<String, Object> extractCriteriaParams(CriteriaDef criteria, Map<String, Object> params) {
        Map<String, Object> criteriaParams = new HashMap<>();
        for (String paramName : criteria.getBindParams()) {
            if (params.containsKey(paramName)) {
                criteriaParams.put(paramName, params.get(paramName));
            }
        }
        return criteriaParams;
    }
    
    private String wrapQueryForFiltersAndPagination(String sql, QueryContext context, Map<String, Object> params) {
        // Check if we need to wrap the query
        boolean hasFilters = !context.getFilters().isEmpty();
        boolean hasSorting = context.hasSorts() && !context.getSorts().isEmpty();
        boolean hasPagination = context.hasPagination() && context.getDefinition().isPaginationEnabled();
        
        if (!hasFilters && !hasSorting && !hasPagination) {
            return sql;
        }
        
        // Wrap the original query in a CTE or subquery
        StringBuilder wrappedSql = new StringBuilder();
        wrappedSql.append("WITH base_query AS (\n");
        wrappedSql.append(sql);
        wrappedSql.append("\n)\n");
        wrappedSql.append("SELECT * FROM base_query");
        
        // Add filters
        if (hasFilters) {
            String filterClause = buildFilterClause(context, params);
            if (!filterClause.isEmpty()) {
                wrappedSql.append("\nWHERE ").append(filterClause);
            }
        }
        
        // Add sorting
        if (hasSorting) {
            String orderByClause = buildOrderByClause(context);
            if (!orderByClause.isEmpty()) {
                wrappedSql.append("\n").append(orderByClause);
            }
        }
        
        // Add pagination
        if (hasPagination) {
            String paginationClause = buildPaginationClause(context.getPagination(), params);
            wrappedSql.append("\n").append(paginationClause);
        }
        
        return wrappedSql.toString();
    }
    
    private String buildFilterClause(QueryContext context, Map<String, Object> params) {
        StringBuilder filterSql = new StringBuilder();
        int filterIndex = 0;
        
        for (QueryContext.Filter filter : context.getFilters().values()) {
            AttributeDef attr = context.getDefinition().getAttribute(filter.getAttribute());
            if (attr == null || !attr.isFilterable()) {
                continue;
            }
            
            String condition = buildFilterCondition(filter, attr, params, filterIndex++);
            if (!condition.isEmpty()) {
                if (filterSql.length() > 0) {
                    filterSql.append(" AND ");
                }
                filterSql.append(condition);
            }
        }
        
        return filterSql.toString();
    }
    
    private String buildFilterCondition(QueryContext.Filter filter, AttributeDef attr, 
                                       Map<String, Object> params, int index) {
        String column = attr.getDbColumn();
        FilterOp op = filter.getOperator();
        String paramName = "filter_" + attr.getName() + "_" + index;
        
        switch (op) {
            case EQUALS:
                params.put(paramName, filter.getValue());
                return column + " = :" + paramName;
                
            case NOT_EQUALS:
                params.put(paramName, filter.getValue());
                return column + " != :" + paramName;
                
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
                
            case LIKE:
                params.put(paramName, filter.getValue());
                return column + " LIKE :" + paramName;
                
            case NOT_LIKE:
                params.put(paramName, filter.getValue());
                return column + " NOT LIKE :" + paramName;
                
            case IN:
                if (filter.getValues() != null && !filter.getValues().isEmpty()) {
                    params.put(paramName, filter.getValues());
                    return column + " IN (:" + paramName + ")";
                }
                return "";
                
            case NOT_IN:
                if (filter.getValues() != null && !filter.getValues().isEmpty()) {
                    params.put(paramName, filter.getValues());
                    return column + " NOT IN (:" + paramName + ")";
                }
                return "";
                
            case BETWEEN:
                if (filter.getValue() != null && filter.getValue2() != null) {
                    params.put(paramName + "_1", filter.getValue());
                    params.put(paramName + "_2", filter.getValue2());
                    return column + " BETWEEN :" + paramName + "_1 AND :" + paramName + "_2";
                }
                return "";
                
            case IS_NULL:
                return column + " IS NULL";
                
            case IS_NOT_NULL:
                return column + " IS NOT NULL";
                
            case CONTAINS:
                params.put(paramName, "%" + filter.getValue() + "%");
                return column + " LIKE :" + paramName;
                
            case STARTS_WITH:
                params.put(paramName, filter.getValue() + "%");
                return column + " LIKE :" + paramName;
                
            case ENDS_WITH:
                params.put(paramName, "%" + filter.getValue());
                return column + " LIKE :" + paramName;
                
            default:
                throw new QueryExecutionException("Unsupported filter operator: " + op);
        }
    }
    
    private String buildOrderByClause(QueryContext context) {
        if (!context.hasSorts() || context.getSorts().isEmpty()) {
            return "";
        }
        
        StringBuilder orderBySql = new StringBuilder("ORDER BY ");
        boolean first = true;
        
        for (QueryContext.SortSpec sort : context.getSorts()) {
            AttributeDef attr = context.getDefinition().getAttribute(sort.getAttribute());
            if (attr == null || !attr.isSortable()) {
                continue;
            }
            
            if (!first) {
                orderBySql.append(", ");
            }
            orderBySql.append(attr.getDbColumn())
                     .append(" ")
                     .append(sort.getDirection().getSql());
            first = false;
        }
        
        return first ? "" : orderBySql.toString();
    }
    
    
    private String buildPaginationClause(QueryContext.Pagination pagination, Map<String, Object> params) {
        // Using standard LIMIT/OFFSET (works with PostgreSQL, MySQL, H2)
        // For other databases, this would need to be adapted
        
        int limit = pagination.getPageSize();
        int offset = pagination.getOffset() != null ? pagination.getOffset() : pagination.getStart();
        
        params.put("limit", limit);
        params.put("offset", offset);
        
        return "LIMIT :limit OFFSET :offset";
    }
    
    private String removeUnusedPlaceholders(String sql) {
        // Remove any remaining comment placeholders
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);
        
        // Clean up extra whitespace
        String cleaned = sb.toString()
            .replaceAll("\\s+", " ")
            .replaceAll("\\s+WHERE\\s+AND", " WHERE")
            .replaceAll("WHERE\\s+ORDER BY", " ORDER BY")
            .replaceAll("WHERE\\s+LIMIT", " LIMIT")
            .trim();
        
        return cleaned;
    }
}