package com.balsam.oasis.common.registry.dialect;

/**
 * Oracle 11g dialect implementation using ROWNUM for pagination.
 */
public class Oracle11gDialect implements DatabaseDialect {

    @Override
    public String buildPaginatedQuery(String sql, int offset, int limit) {
        // Oracle 11g uses ROWNUM for pagination
        int endRow = offset + limit;

        return String.format(
                "SELECT * FROM (" +
                        "  SELECT ROWNUM rn, query_result.* FROM (" +
                        "    %s" +
                        "  ) query_result" +
                        "  WHERE ROWNUM <= %d" +
                        ") WHERE rn > %d",
                sql, endRow, offset);
    }

    @Override
    public String getName() {
        return "ORACLE_11G";
    }

    @Override
    public boolean supportsFetchOffset() {
        return false;
    }
}