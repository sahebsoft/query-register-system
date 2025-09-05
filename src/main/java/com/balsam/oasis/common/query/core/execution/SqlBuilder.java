package com.balsam.oasis.common.query.core.execution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.query.core.definition.AttributeDef;
import com.balsam.oasis.common.query.core.definition.CriteriaDef;
import com.balsam.oasis.common.query.core.definition.FilterOp;
import com.balsam.oasis.common.query.core.definition.QueryDefinition;
import com.balsam.oasis.common.query.dialect.DatabaseDialect;
import com.balsam.oasis.common.query.dialect.DialectFactory;
import com.balsam.oasis.common.query.exception.QueryExecutionException;

/**
 * Builds dynamic SQL from query definition and context.
 * Supports multiple database dialects including Oracle 11g and 12c.
 * Implements caching for filter and order clauses to improve performance.
 *
 * @author Query Registration System
 * @since 1.0
 */
public class SqlBuilder {

    private static final Logger log = LoggerFactory.getLogger(SqlBuilder.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("--(\\w+)");

    private final DatabaseDialect dialect;

    // Cache for filter and order clauses per query execution
    private String cachedFilterClause;
    private String cachedOrderClause;
    private QueryContext cachedContext;

    public SqlBuilder(String dialectName) {
        this.dialect = DialectFactory.getDialect(dialectName != null ? dialectName : "ORACLE_11G");
        log.info("SqlBuilder initialized with database dialect: {}", this.dialect.getName());
    }

    public SqlBuilder() {
        this("ORACLE_11G");
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
        sql = applyCriteria(sql, context);

        // Clean up any remaining placeholders from criteria
        sql = removeUnusedPlaceholders(sql);

        // Store base SQL for count query
        String baseSql = sql;
        Map<String, Object> baseParams = new HashMap<>(params);

        // Wrap the query to apply filters, sorting, and pagination
        sql = wrapQueryForFiltersAndPagination(sql, context, params);

        log.debug("Built SQL for {}: {}", dialect.getName(), sql);
        log.debug("Parameters: {}", params);

        SqlResult result = new SqlResult(sql, params);
        result.baseSql = baseSql;
        result.paramsWithoutPagination = baseParams;
        return result;
    }

    private String applyCriteria(String sql, QueryContext context) {
        QueryDefinition definition = context.getDefinition();

        if (!definition.hasCriteria()) {
            return sql;
        }

        // Sort criteria by priority
        List<CriteriaDef> sortedCriteria = definition.getCriteria().values().stream()
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
                // Generate SQL
                String criteriaSql = criteria.getSql();
                // Replace placeholder
                sql = sql.replace(placeholder, criteriaSql);

                // Record applied criteria
                context.recordAppliedCriteria(
                        criteria.getName(),
                        criteriaSql);
            } else {
                // Remove placeholder if criteria not applied
                sql = sql.replace(placeholder, "");
            }
        }

        return sql;
    }

    private String wrapQueryForFiltersAndPagination(String sql, QueryContext context, Map<String, Object> params) {
        // Check if we need to wrap the query
        boolean hasFilters = !context.getFilters().isEmpty();
        boolean hasSorting = context.hasSorts() && !context.getSorts().isEmpty();
        boolean hasPagination = context.hasPagination() && context.getDefinition().isPaginationEnabled();

        // Check if query is complex (has JOINs) - if yes, always wrap when
        // filtering/sorting
        boolean isComplexQuery = sql.toUpperCase().contains(" JOIN ");

        // If complex query with filters or sorting, we MUST wrap to avoid ambiguous
        // columns
        boolean needsWrapper = (isComplexQuery && (hasFilters || hasSorting)) || hasPagination;

        if (!needsWrapper && !hasFilters && !hasSorting && !hasPagination) {
            return sql;
        }

        // For Oracle 11g with pagination, we need to use ROWNUM approach
        if ("ORACLE_11G".equals(dialect.getName()) && (hasPagination || needsWrapper)) {
            return buildOracle11gQuery(sql, context, params, hasFilters, hasSorting);
        }

        // For complex queries or when we have filters/sorting/pagination, wrap with CTE
        if (isComplexQuery || hasFilters || hasSorting || hasPagination) {
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

        return sql;
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

        // Check if query is complex (has JOINs)
        boolean isComplexQuery = sql.toUpperCase().contains(" JOIN ");

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

        // For complex queries with filters/sorting, we need to wrap even without
        // pagination
        if (isComplexQuery && (hasFilters || hasSorting)) {
            if (needsPagination) {
                int endRow = offset + limit;

                // Three-level wrapper for pagination with complex queries
                // Outer query - filters by rnum > offset
                oracle11gSql.append("SELECT * FROM (\n");

                // Middle query - adds ROWNUM as rnum and limits to endRow
                oracle11gSql.append("  SELECT filtered_query.*, ROWNUM rnum FROM (\n");

                // First wrap the complex query to avoid ambiguous columns
                oracle11gSql.append("    SELECT * FROM (\n");
                oracle11gSql.append("      ").append(sql.replace("\n", "\n      "));
                oracle11gSql.append("\n    ) base_query");

                // Add filters on the wrapped query
                if (hasFilters) {
                    String filterClause = buildFilterClause(context, params);
                    if (!filterClause.isEmpty()) {
                        oracle11gSql.append("\n    WHERE ").append(filterClause);
                    }
                }

                // Add sorting on the wrapped query
                if (hasSorting) {
                    String orderByClause = buildOrderByClause(context);
                    if (!orderByClause.isEmpty()) {
                        oracle11gSql.append("\n    ").append(orderByClause);
                    }
                }

                // Close filtered_query
                oracle11gSql.append("\n  ) filtered_query\n");

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
                // Complex query with filters/sorting but no pagination
                // Wrap the query to avoid ambiguous columns
                oracle11gSql.append("SELECT * FROM (\n");
                oracle11gSql.append("  ").append(sql.replace("\n", "\n  "));
                oracle11gSql.append("\n) base_query");

                // Add filters
                if (hasFilters) {
                    String filterClause = buildFilterClause(context, params);
                    if (!filterClause.isEmpty()) {
                        oracle11gSql.append("\nWHERE ").append(filterClause);
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
        } else if (needsPagination) {
            // Simple query with pagination (no JOINs)
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
            // Simple query without pagination, just apply filters and sorting directly
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

    /**
     * Builds filter clause from query context.
     * Caches the result for repeated calls within the same query execution.
     *
     * @param context The query context containing filters
     * @param params  The parameter map to populate
     * @return The filter clause SQL string
     */
    private String buildFilterClause(QueryContext context, Map<String, Object> params) {
        // Check if we can use cached filter clause
        if (cachedContext == context && cachedFilterClause != null) {
            return cachedFilterClause;
        }

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

        // Cache the result
        cachedContext = context;
        cachedFilterClause = filterSql.toString();

        return cachedFilterClause;
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
        if ("ORACLE_12C".equals(dialect.getName())) {
            // Oracle 12c+ (SQL:2008 standard)
            params.put("offset", offset);
            params.put("limit", limit);
            return "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        } else if ("ORACLE_11G".equals(dialect.getName())) {
            // Oracle 11g - handled separately in buildOracle11gQuery
            // This shouldn't be reached, but return empty as a safety
            return "";
        } else {
            // Default to Oracle 11g behavior
            return "";
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

    public String getDatabaseDialect() {
        return dialect.getName();
    }
}