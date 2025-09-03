package com.balasam.oasis.common.query.core.definition;

/**
 * Supported Oracle database dialects for SQL generation
 */
public enum DatabaseDialect {
    
    /**
     * Oracle 11g (uses ROWNUM for pagination)
     */
    ORACLE_11G("Oracle", "11g"),
    
    /**
     * Oracle 12c and above (supports FETCH/OFFSET)
     */
    ORACLE_12C("Oracle", "12c");
    
    private final String displayName;
    private final String version;
    
    DatabaseDialect(String displayName, String version) {
        this.displayName = displayName;
        this.version = version;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getVersion() {
        return version;
    }
    
    /**
     * Check if this is an Oracle database (always true for this enum)
     */
    public boolean isOracle() {
        return true;
    }
    
    /**
     * Check if database supports FETCH/OFFSET syntax (SQL:2008 standard)
     */
    public boolean supportsFetchOffset() {
        return this == ORACLE_12C;
    }
    
    /**
     * Check if database requires ROWNUM for pagination (Oracle 11g)
     */
    public boolean requiresRownum() {
        return this == ORACLE_11G;
    }
    
    /**
     * Parse dialect from string configuration
     */
    public static DatabaseDialect fromString(String dialect) {
        if (dialect == null || dialect.trim().isEmpty()) {
            return ORACLE_12C; // Default to 12c
        }
        
        String normalized = dialect.toUpperCase().replace(" ", "_").replace(".", "");
        
        // Handle specific Oracle versions
        if (normalized.contains("11")) {
            return ORACLE_11G;
        } else if (normalized.contains("12") || normalized.contains("18") || normalized.contains("19") || normalized.contains("21") || normalized.contains("23")) {
            return ORACLE_12C; // 12c and above use FETCH/OFFSET
        }
        
        // Try exact match
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Default to 12c for unrecognized input
            return ORACLE_12C;
        }
    }
}