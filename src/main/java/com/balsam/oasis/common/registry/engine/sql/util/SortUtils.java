package com.balsam.oasis.common.registry.engine.sql.util;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

/**
 * Utility class for applying sorting to SQL queries.
 * Handles ORDER BY clause generation based on QueryContext sorts.
 */
public class SortUtils {

    private static final Logger log = LoggerFactory.getLogger(SortUtils.class);

    /**
     * Apply sorting from QueryContext to the SQL query.
     * 
     * @param sql     The SQL query
     * @param context The query context containing sorts
     * @return The SQL with ORDER BY clause applied
     */
    public static String applySorting(String sql, QueryContext context) {
        if (context.getSorts() == null || context.getSorts().isEmpty()) {
            return sql;
        }

        List<String> orderClauses = context.getSorts().stream()
                .map(sort -> buildSortClause(sort, context))
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

    /**
     * Build a sort clause for a specific sort specification.
     * 
     * @param sort    The sort specification
     * @param context The query context
     * @return The sort clause string, or null if the attribute is not sortable
     */
    private static String buildSortClause(QueryContext.SortSpec sort, QueryContext context) {
        AttributeDef<?> attr = context.getDefinition().getAttribute(sort.getAttribute());
        if (attr == null || !attr.sortable()) {
            log.warn("Attribute {} is not sortable or does not exist", sort.getAttribute());
            return null;
        }

        String column = attr.aliasName() != null ? attr.aliasName() : attr.name();
        return column + " " + sort.getDirection().name();
    }
}