package com.balsam.oasis.common.registry.engine.sql;

/**
 * Database dialect enumeration for SQL generation.
 */
public enum DatabaseDialect {
    ORACLE_11G("Oracle 11g"),
    ORACLE_12C("Oracle 12c"),
    ORACLE_19C("Oracle 19c"),
    ORACLE_21C("Oracle 21c"),
    MYSQL("MySQL"),
    MARIADB("MariaDB"),
    POSTGRESQL("PostgreSQL"),
    SQLSERVER("SQL Server"),
    H2("H2"),
    HSQLDB("HSQLDB");
    
    private final String displayName;
    
    DatabaseDialect(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static DatabaseDialect fromString(String dialect) {
        if (dialect == null) {
            return ORACLE_11G;
        }
        
        String upperDialect = dialect.toUpperCase().replace("-", "_").replace(" ", "_");
        
        try {
            return valueOf(upperDialect);
        } catch (IllegalArgumentException e) {
            // Try to match by partial name
            if (upperDialect.contains("ORACLE")) {
                if (upperDialect.contains("12")) return ORACLE_12C;
                if (upperDialect.contains("19")) return ORACLE_19C;
                if (upperDialect.contains("21")) return ORACLE_21C;
                return ORACLE_11G;
            }
            if (upperDialect.contains("MYSQL")) return MYSQL;
            if (upperDialect.contains("MARIA")) return MARIADB;
            if (upperDialect.contains("POSTGRES")) return POSTGRESQL;
            if (upperDialect.contains("SQL") && upperDialect.contains("SERVER")) return SQLSERVER;
            if (upperDialect.contains("H2")) return H2;
            if (upperDialect.contains("HSQL")) return HSQLDB;
            
            return ORACLE_11G; // default
        }
    }
    
    public boolean isOracle() {
        return this == ORACLE_11G || this == ORACLE_12C || this == ORACLE_19C || this == ORACLE_21C;
    }
    
    public boolean supportsOffsetFetch() {
        return this == ORACLE_12C || this == ORACLE_19C || this == ORACLE_21C || 
               this == POSTGRESQL || this == SQLSERVER;
    }
}