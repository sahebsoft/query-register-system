package com.balsam.oasis.common.registry.util;

import java.util.Map;

import com.balsam.oasis.common.registry.domain.common.Pagination;

/**
 * Centralized pagination utility for all SQL dialects.
 * This is the single source of truth for pagination logic.
 */
public class PaginationUtils {

    public static final String ORACLE_11G = "ORACLE_11G";
    public static final String ORACLE_12C = "ORACLE_12C";
    public static final String POSTGRESQL = "POSTGRESQL";
    public static final String MYSQL = "MYSQL";
    public static final String STANDARD = "STANDARD";

    /**
     * Apply pagination to SQL based on database dialect.
     * 
     * @param sql        The SQL query to paginate
     * @param pagination The pagination parameters
     * @param dialect    The database dialect (ORACLE_11G, ORACLE_12C, POSTGRESQL,
     *                   MYSQL, STANDARD)
     * @param params     The parameter map to add pagination parameters to
     * @return The paginated SQL query
     */
    public static String applyPagination(String sql, Pagination pagination,
            String dialect, Map<String, Object> params) {
        if (pagination == null) {
            return sql;
        }

        // Choose pagination strategy based on dialect
        switch (dialect != null ? dialect.toUpperCase() : STANDARD) {
            case ORACLE_11G:
                return applyOracle11gPagination(sql, pagination, params);
            case ORACLE_12C:
                return applyOracle12cPagination(sql, pagination, params);
            case POSTGRESQL:
                return applyPostgresPagination(sql, pagination, params);
            case MYSQL:
                return applyMySqlPagination(sql, pagination, params);
            default:
                return applyStandardPagination(sql, pagination, params);
        }
    }

    /**
     * Oracle 11g pagination using ROWNUM.
     */
    private static String applyOracle11gPagination(String sql, Pagination pagination,
            Map<String, Object> params) {
        sql = "SELECT * FROM (" +
                "  SELECT inner_query.*, ROWNUM rnum FROM (" +
                "    " + sql +
                "  ) inner_query" +
                "  WHERE ROWNUM <= :endRow" +
                ") WHERE rnum > :startRow";
        params.put("startRow", pagination.getStart());
        params.put("endRow", pagination.getEnd());
        return sql;
    }

    /**
     * Oracle 12c+ pagination using OFFSET/FETCH.
     */
    private static String applyOracle12cPagination(String sql, Pagination pagination,
            Map<String, Object> params) {
        sql = sql + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        params.put("offset", pagination.getOffset());
        params.put("limit", pagination.getLimit());
        return sql;
    }

    /**
     * PostgreSQL pagination using LIMIT/OFFSET.
     */
    private static String applyPostgresPagination(String sql, Pagination pagination,
            Map<String, Object> params) {
        sql = sql + " LIMIT :limit OFFSET :offset";
        params.put("limit", pagination.getLimit());
        params.put("offset", pagination.getOffset());
        return sql;
    }

    /**
     * MySQL pagination using LIMIT with offset.
     */
    private static String applyMySqlPagination(String sql, Pagination pagination,
            Map<String, Object> params) {
        sql = sql + " LIMIT :offset, :limit";
        params.put("offset", pagination.getOffset());
        params.put("limit", pagination.getLimit());
        return sql;
    }

    /**
     * Standard SQL pagination (most databases support this).
     */
    private static String applyStandardPagination(String sql, Pagination pagination,
            Map<String, Object> params) {
        sql = sql + " LIMIT :limit OFFSET :offset";
        params.put("limit", pagination.getLimit());
        params.put("offset", pagination.getOffset());
        return sql;
    }

    /**
     * Update pagination metadata after query execution.
     */
    public static void updatePaginationMetadata(Pagination pagination, int totalCount) {
        if (pagination != null) {
            pagination.setTotal(totalCount);
            pagination.setHasNext(pagination.getEnd() < totalCount);
            pagination.setHasPrevious(pagination.getStart() > 0);
        }
    }
}