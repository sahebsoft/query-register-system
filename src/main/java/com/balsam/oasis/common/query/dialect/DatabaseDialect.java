package com.balsam.oasis.common.query.dialect;

/**
 * Database dialect interface for handling database-specific SQL generation.
 */
public interface DatabaseDialect {
    
    /**
     * Build pagination SQL for the specific database.
     * 
     * @param sql the base SQL query
     * @param offset the starting row (0-based)
     * @param limit the maximum number of rows
     * @return the paginated SQL query
     */
    String buildPaginatedQuery(String sql, int offset, int limit);
    
    /**
     * Get the dialect name.
     * 
     * @return the dialect name
     */
    String getName();
    
    /**
     * Check if this dialect supports FETCH/OFFSET pagination.
     * 
     * @return true if FETCH/OFFSET is supported
     */
    boolean supportsFetchOffset();
}