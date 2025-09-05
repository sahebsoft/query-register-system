package com.balsam.oasis.common.query.dialect;

/**
 * Oracle 12c+ dialect implementation using FETCH/OFFSET for pagination.
 */
public class Oracle12cDialect implements DatabaseDialect {
    
    @Override
    public String buildPaginatedQuery(String sql, int offset, int limit) {
        // Oracle 12c+ supports standard FETCH/OFFSET
        StringBuilder sb = new StringBuilder(sql);
        
        if (offset > 0) {
            sb.append(" OFFSET ").append(offset).append(" ROWS");
        }
        
        if (limit > 0) {
            sb.append(" FETCH NEXT ").append(limit).append(" ROWS ONLY");
        }
        
        return sb.toString();
    }
    
    @Override
    public String getName() {
        return "ORACLE_12C";
    }
    
    @Override
    public boolean supportsFetchOffset() {
        return true;
    }
}