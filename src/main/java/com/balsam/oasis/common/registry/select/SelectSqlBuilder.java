package com.balsam.oasis.common.registry.select;

import java.util.HashMap;
import java.util.Map;

import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.execution.SqlBuilder;
import com.balsam.oasis.common.registry.dialect.DatabaseDialect;
import com.balsam.oasis.common.registry.dialect.DialectFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL builder specialized for Select queries.
 * Extends SqlBuilder to provide Select-specific SQL generation.
 */
public class SelectSqlBuilder {

    private static final Logger log = LoggerFactory.getLogger(SelectSqlBuilder.class);
    private final DatabaseDialect dialect;

    public SelectSqlBuilder(String dialectName) {
        this.dialect = DialectFactory.getDialect(dialectName != null ? dialectName : "ORACLE_11G");
        log.info("SelectSqlBuilder initialized with database dialect: {}", this.dialect.getName());
    }

    public SelectSqlBuilder() {
        this("ORACLE_11G");
    }

    public static class SelectSqlResult {
        private final String sql;
        private final Map<String, Object> params;

        public SelectSqlResult(String sql, Map<String, Object> params) {
            this.sql = sql;
            this.params = params;
        }

        public String getSql() {
            return sql;
        }

        public Map<String, Object> getParams() {
            return params;
        }
    }

    public SelectSqlResult build(SelectContext context) {
        SelectDefinition definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> params = new HashMap<>(context.getParams());

        // Apply criteria
        sql = applyCriteria(sql, context, params);

        // Apply ID filtering or search
        sql = applyIdOrSearchFilter(sql, context, params);

        // Apply pagination
        sql = applyPagination(sql, context, params);

        log.debug("Built SQL for select '{}': {}", definition.getName(), sql);
        log.debug("Parameters: {}", params);

        return new SelectSqlResult(sql, params);
    }

    private String applyCriteria(String sql, SelectContext context, Map<String, Object> params) {
        SelectDefinition definition = context.getDefinition();

        if (definition.hasCriteria()) {
            for (CriteriaDef criteria : definition.getCriteria().values()) {
                String placeholder = "--" + criteria.getName();
                if (sql.contains(placeholder)) {
                    boolean shouldApply = shouldApplyCriteria(criteria, context);
                    
                    if (shouldApply) {
                        sql = sql.replace(placeholder, criteria.getSql());
                        // Track applied criteria
                        context.addAppliedCriteria(
                            criteria.getName(),
                            criteria.getSql(),
                            extractCriteriaParams(criteria, params),
                            "Criteria condition met"
                        );
                    } else {
                        sql = sql.replace(placeholder, "");
                    }
                }
            }
        }

        // Handle search criteria placeholder if no search term
        if (definition.hasSearchCriteria() && !context.hasSearchTerm()) {
            String placeholder = "--" + definition.getSearchCriteria().getName();
            sql = sql.replace(placeholder, "");
        }

        return sql;
    }

    private boolean shouldApplyCriteria(CriteriaDef criteria, SelectContext context) {
        if (criteria.getCondition() == null) {
            return true; // No condition means always apply
        }

        // Check if all bind parameters for this criteria are present
        String criteriaSql = criteria.getSql();
        if (criteriaSql != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":(\\w+)");
            java.util.regex.Matcher matcher = pattern.matcher(criteriaSql);
            while (matcher.find()) {
                String paramName = matcher.group(1);
                if (!context.hasParam(paramName)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Map<String, Object> extractCriteriaParams(CriteriaDef criteria, Map<String, Object> allParams) {
        Map<String, Object> criteriaParams = new HashMap<>();
        String criteriaSql = criteria.getSql();
        if (criteriaSql != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":(\\w+)");
            java.util.regex.Matcher matcher = pattern.matcher(criteriaSql);
            while (matcher.find()) {
                String paramName = matcher.group(1);
                if (allParams.containsKey(paramName)) {
                    criteriaParams.put(paramName, allParams.get(paramName));
                }
            }
        }
        return criteriaParams;
    }

    private String applyIdOrSearchFilter(String sql, SelectContext context, Map<String, Object> params) {
        SelectDefinition definition = context.getDefinition();

        // Handle ID filtering - wrap the entire query
        if (context.hasIds()) {
            String valueColumn = definition.getValueAttribute().getAliasName();
            if (context.getIds().size() == 1) {
                sql = "SELECT * FROM (" + sql + ") wrapped_query WHERE " +
                        valueColumn + " = :__id";
                params.put("__id", context.getIds().get(0));
            } else {
                sql = "SELECT * FROM (" + sql + ") wrapped_query WHERE " +
                        valueColumn + " IN (:__ids)";
                params.put("__ids", context.getIds());
            }
            context.addAppliedCriteria("idFilter", 
                valueColumn + (context.getIds().size() == 1 ? " = :__id" : " IN (:__ids)"),
                Map.of(context.getIds().size() == 1 ? "__id" : "__ids", 
                       context.getIds().size() == 1 ? context.getIds().get(0) : context.getIds()),
                "Filtering by specific IDs");
        }
        // Handle search
        else if (context.hasSearchTerm()) {
            if (definition.hasSearchCriteria()) {
                // Use defined search criteria
                CriteriaDef searchCriteria = definition.getSearchCriteria();
                String placeholder = "--" + searchCriteria.getName();
                if (sql.contains(placeholder)) {
                    sql = sql.replace(placeholder, searchCriteria.getSql());
                }
            } else {
                // Auto-wrap with label LIKE condition
                String labelColumn = definition.getLabelAttribute().getAliasName();
                sql = "SELECT * FROM (" + sql + ") wrapped_query WHERE LOWER(" +
                        labelColumn + ") LIKE LOWER(:search)";
            }
            params.put("search", "%" + context.getSearchTerm() + "%");
            context.addAppliedCriteria("searchFilter",
                "Label LIKE search term",
                Map.of("search", "%" + context.getSearchTerm() + "%"),
                "Search term filtering");
        }

        return sql;
    }

    private String applyPagination(String sql, SelectContext context, Map<String, Object> params) {
        if (!context.hasPagination()) {
            return sql;
        }

        SelectContext.Pagination pagination = context.getPagination();
        
        // Use dialect-specific pagination
        if (dialect.getName().startsWith("ORACLE")) {
            if (dialect.getName().equals("ORACLE_12C")) {
                // Oracle 12c+ uses OFFSET/FETCH
                sql = sql + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
                params.put("offset", pagination.getStart());
                params.put("limit", pagination.getPageSize());
            } else {
                // Oracle 11g uses ROWNUM
                sql = "SELECT * FROM (" +
                        "  SELECT inner_query.*, ROWNUM rnum FROM (" +
                        "    " + sql +
                        "  ) inner_query" +
                        "  WHERE ROWNUM <= :endRow" +
                        ") WHERE rnum > :startRow";
                params.put("startRow", pagination.getStart());
                params.put("endRow", pagination.getEnd());
            }
        } else {
            // Default to LIMIT/OFFSET (PostgreSQL, MySQL, etc.)
            sql = sql + " LIMIT :limit OFFSET :offset";
            params.put("limit", pagination.getPageSize());
            params.put("offset", pagination.getStart());
        }

        return sql;
    }

    /**
     * Build count query for total count
     */
    public SelectSqlResult buildCountQuery(SelectContext context) {
        SelectDefinition definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> params = new HashMap<>(context.getParams());

        // Apply criteria WITHOUT tracking (to avoid duplicates)
        sql = applyCriteriaWithoutTracking(sql, definition, context, params);

        // Apply ID or search filter WITHOUT tracking criteria (already tracked in main query)
        sql = applyIdOrSearchFilterWithoutTracking(sql, context, params);

        // Wrap in count query
        sql = "SELECT COUNT(*) FROM (" + sql + ") count_query";

        return new SelectSqlResult(sql, params);
    }
    
    private String applyCriteriaWithoutTracking(String sql, SelectDefinition definition, SelectContext context, Map<String, Object> params) {
        if (definition.hasCriteria()) {
            for (CriteriaDef criteria : definition.getCriteria().values()) {
                String placeholder = "--" + criteria.getName();
                if (sql.contains(placeholder)) {
                    boolean shouldApply = shouldApplyCriteria(criteria, context);
                    
                    if (shouldApply) {
                        sql = sql.replace(placeholder, criteria.getSql());
                        // Don't track - already tracked in main query
                    } else {
                        sql = sql.replace(placeholder, "");
                    }
                }
            }
        }

        // Handle search criteria placeholder if no search term
        if (definition.hasSearchCriteria() && !context.hasSearchTerm()) {
            String placeholder = "--" + definition.getSearchCriteria().getName();
            sql = sql.replace(placeholder, "");
        }

        return sql;
    }
    
    private String applyIdOrSearchFilterWithoutTracking(String sql, SelectContext context, Map<String, Object> params) {
        SelectDefinition definition = context.getDefinition();

        // Handle ID filtering - wrap the entire query
        if (context.hasIds()) {
            String valueColumn = definition.getValueAttribute().getAliasName();
            if (context.getIds().size() == 1) {
                sql = "SELECT * FROM (" + sql + ") wrapped_query WHERE " +
                        valueColumn + " = :__id";
                params.put("__id", context.getIds().get(0));
            } else {
                sql = "SELECT * FROM (" + sql + ") wrapped_query WHERE " +
                        valueColumn + " IN (:__ids)";
                params.put("__ids", context.getIds());
            }
            // Don't add to applied criteria - already tracked in main query
        }
        // Handle search
        else if (context.hasSearchTerm()) {
            if (definition.hasSearchCriteria()) {
                // Use defined search criteria
                CriteriaDef searchCriteria = definition.getSearchCriteria();
                String placeholder = "--" + searchCriteria.getName();
                if (sql.contains(placeholder)) {
                    sql = sql.replace(placeholder, searchCriteria.getSql());
                }
            } else {
                // Auto-wrap with label LIKE condition
                String labelColumn = definition.getLabelAttribute().getAliasName();
                sql = "SELECT * FROM (" + sql + ") wrapped_query WHERE LOWER(" +
                        labelColumn + ") LIKE LOWER(:search)";
            }
            params.put("search", "%" + context.getSearchTerm() + "%");
            // Don't add to applied criteria - already tracked in main query
        }

        return sql;
    }
}