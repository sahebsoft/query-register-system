package com.balasam.oasis.common.query.core.execution;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.CriteriaDef;
import com.balasam.oasis.common.query.core.definition.DatabaseDialect;
import com.balasam.oasis.common.query.core.definition.FilterOp;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.exception.QueryExecutionException;

/**
 * Builds dynamic SQL from query definition and context
 * Supports multiple database dialects including Oracle 11g and 12c
 */
public class SqlBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(SqlBuilder.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("--(\\w+)");
    
    private final DatabaseDialect databaseDialect;
    
    public SqlBuilder(String dialect) {
        log.info("Dialect property value: {}", dialect);
        this.databaseDialect = DatabaseDialect.fromString(dialect);
        log.info("SqlBuilder initialized with database dialect: {} ({})", 
            databaseDialect.getDisplayName(), databaseDialect.getVersion());
    }
    
    // Constructor for testing
    public SqlBuilder(DatabaseDialect dialect) {
        this.databaseDialect = dialect;
    }
    
    // Default constructor - defaults to Oracle 12c
    public SqlBuilder() {
        this(DatabaseDialect.ORACLE_12C);
    }
    
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
        
        log.debug("Built SQL for {}: {}", databaseDialect.getDisplayName(), sql);
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
        
        // For Oracle 11g with pagination, we need to use ROWNUM approach
        if (databaseDialect.requiresRownum() && hasPagination) {
            return buildOracle11gQuery(sql, context, params, hasFilters, hasSorting);
        }
        
        // For other databases, use CTE or standard approach
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
    
    /**
     * Build Oracle 11g compatible query using ROWNUM
     * 
     * Oracle 11g pagination requires a specific pattern:
     * 1. Inner query: Apply filters and sorting
     * 2. Middle query: Add ROWNUM and limit to max row
     * 3. Outer query: Filter by min row (offset)
     */
    private String buildOracle11gQuery(String sql, QueryContext context, Map<String, Object> params,
                                      boolean hasFilters, boolean hasSorting) {
        StringBuilder oracle11gSql = new StringBuilder();
        
        // Determine pagination parameters
        int offset = 0;
        int limit = Integer.MAX_VALUE;
        boolean needsPagination = false;
        
        if (context.hasPagination()) {
            QueryContext.Pagination pagination = context.getPagination();
            limit = pagination.getPageSize();
            Integer offsetValue = pagination.getOffset();
            offset = (offsetValue != null) ? offsetValue : pagination.getStart();
            needsPagination = true;
        }
        
        if (needsPagination) {
            int endRow = offset + limit;
            
            // Outer query - filters by rnum > offset
            oracle11gSql.append("SELECT * FROM (\n");
            
            // Middle query - adds ROWNUM and limits to endRow
            oracle11gSql.append("  SELECT inner_query.*, ROWNUM rnum FROM (\n");
            
            // Inner query - the actual query with filters and sorting
            oracle11gSql.append("    -- Original query with filters and sorting\n");
            oracle11gSql.append("    ").append(sql.replace("\n", "\n    "));
            
            // Add filters if present
            if (hasFilters) {
                String filterClause = buildFilterClause(context, params);
                if (!filterClause.isEmpty()) {
                    // Check if WHERE already exists in the SQL
                    if (!sql.toUpperCase().contains("WHERE")) {
                        oracle11gSql.append("\n    WHERE ").append(filterClause);
                    } else {
                        oracle11gSql.append("\n    AND ").append(filterClause);
                    }
                }
            }
            
            // Add sorting (must be in innermost query for correct results)
            if (hasSorting) {
                String orderByClause = buildOrderByClause(context);
                if (!orderByClause.isEmpty()) {
                    oracle11gSql.append("\n    ").append(orderByClause);
                }
            }
            
            // Close inner query
            oracle11gSql.append("\n  ) inner_query\n");
            
            // Add ROWNUM <= endRow condition in middle query
            oracle11gSql.append("  WHERE ROWNUM <= :endRow\n");
            params.put("endRow", endRow);
            
            // Close middle query
            oracle11gSql.append(")\n");
            
            // Add outer WHERE for start row (offset)
            if (offset > 0) {
                oracle11gSql.append("WHERE rnum > :startRow");
                params.put("startRow", offset);
            }
        } else {
            // No pagination needed, just wrap with filters and sorting
            oracle11gSql.append(sql);
            
            // Add filters if present
            if (hasFilters) {
                String filterClause = buildFilterClause(context, params);
                if (!filterClause.isEmpty()) {
                    if (!sql.toUpperCase().contains("WHERE")) {
                        oracle11gSql.append("\nWHERE ").append(filterClause);
                    } else {
                        oracle11gSql.append("\nAND ").append(filterClause);
                    }
                }
            }
            
            // Add sorting
            if (hasSorting) {
                String orderByClause = buildOrderByClause(context);
                if (!orderByClause.isEmpty()) {
                    oracle11gSql.append("\n").append(orderByClause);
                }
            }
        }
        
        return oracle11gSql.toString();
    }
    
    private String buildFilterClause(QueryContext context, Map<String, Object> params) {
        StringBuilder filterSql = new StringBuilder();
        int filterIndex = 0;
        
        for (QueryContext.Filter filter : context.getFilters().values()) {
            AttributeDef<?> attr = context.getDefinition().getAttribute(filter.getAttribute());
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
    
    private String buildFilterCondition(QueryContext.Filter filter, AttributeDef<?> attr, 
                                       Map<String, Object> params, int index) {
        String column = attr.getAliasName();
        FilterOp op = filter.getOperator();
        String paramName = "filter_" + attr.getName() + "_" + index;
        
        switch (op) {
            case EQUALS -> {
                params.put(paramName, filter.getValue());
                return column + " = :" + paramName;
            }
                
            case NOT_EQUALS -> {
                params.put(paramName, filter.getValue());
                return column + " != :" + paramName;
            }
                
            case GREATER_THAN -> {
                params.put(paramName, filter.getValue());
                return column + " > :" + paramName;
            }
                
            case GREATER_THAN_OR_EQUAL -> {
                params.put(paramName, filter.getValue());
                return column + " >= :" + paramName;
            }
                
            case LESS_THAN -> {
                params.put(paramName, filter.getValue());
                return column + " < :" + paramName;
            }
                
            case LESS_THAN_OR_EQUAL -> {
                params.put(paramName, filter.getValue());
                return column + " <= :" + paramName;
            }
                
            case LIKE -> {
                params.put(paramName, filter.getValue());
                return column + " LIKE :" + paramName;
            }
                
            case NOT_LIKE -> {
                params.put(paramName, filter.getValue());
                return column + " NOT LIKE :" + paramName;
            }
                
            case IN -> {
                if (filter.getValues() != null && !filter.getValues().isEmpty()) {
                    params.put(paramName, filter.getValues());
                    return column + " IN (:" + paramName + ")";
                }
                return "";
            }
                
            case NOT_IN -> {
                if (filter.getValues() != null && !filter.getValues().isEmpty()) {
                    params.put(paramName, filter.getValues());
                    return column + " NOT IN (:" + paramName + ")";
                }
                return "";
            }
                
            case BETWEEN -> {
                if (filter.getValue() != null && filter.getValue2() != null) {
                    params.put(paramName + "_1", filter.getValue());
                    params.put(paramName + "_2", filter.getValue2());
                    return column + " BETWEEN :" + paramName + "_1 AND :" + paramName + "_2";
                }
                return "";
            }
                
            case IS_NULL -> {
                return column + " IS NULL";
            }
                
            case IS_NOT_NULL -> {
                return column + " IS NOT NULL";
            }
                
            case CONTAINS -> {
                params.put(paramName, "%" + filter.getValue() + "%");
                return column + " LIKE :" + paramName;
            }
                
            case STARTS_WITH -> {
                params.put(paramName, filter.getValue() + "%");
                return column + " LIKE :" + paramName;
            }
                
            case ENDS_WITH -> {
                params.put(paramName, "%" + filter.getValue());
                return column + " LIKE :" + paramName;
            }
                
            default -> throw new QueryExecutionException("Unsupported filter operator: " + op);
        }
    }
    
    private String buildOrderByClause(QueryContext context) {
        if (!context.hasSorts() || context.getSorts().isEmpty()) {
            return "";
        }
        
        StringBuilder orderBySql = new StringBuilder("ORDER BY ");
        boolean first = true;
        
        for (QueryContext.SortSpec sort : context.getSorts()) {
            AttributeDef<?> attr = context.getDefinition().getAttribute(sort.getAttribute());
            if (attr == null || !attr.isSortable()) {
                continue;
            }
            
            if (!first) {
                orderBySql.append(", ");
            }
            
            // Use the database column alias name
            orderBySql.append(attr.getAliasName())
                     .append(" ")
                     .append(sort.getDirection().getSql());
            first = false;
        }
        
        return first ? "" : orderBySql.toString();
    }
    
    private String buildPaginationClause(QueryContext.Pagination pagination, Map<String, Object> params) {
        int limit = pagination.getPageSize();
        Integer offsetValue = pagination.getOffset();
        int offset = (offsetValue != null) ? offsetValue : pagination.getStart();
        
        // Handle Oracle database dialects
        if (databaseDialect.supportsFetchOffset()) {
            // Oracle 12c+ (SQL:2008 standard)
            params.put("offset", offset);
            params.put("limit", limit);
            return "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        } else if (databaseDialect.requiresRownum()) {
            // Oracle 11g - handled separately in buildOracle11gQuery
            // This shouldn't be reached, but return empty as a safety
            return "";
        } else {
            // Should never reach here with only Oracle dialects
            throw new IllegalStateException("Unsupported database dialect: " + databaseDialect);
        }
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
            .replaceAll("WHERE\\s+OFFSET", " OFFSET")
            .replaceAll("WHERE\\s+FETCH", " FETCH")
            .trim();
        
        return cleaned;
    }
    
    public DatabaseDialect getDatabaseDialect() {
        return databaseDialect;
    }
}