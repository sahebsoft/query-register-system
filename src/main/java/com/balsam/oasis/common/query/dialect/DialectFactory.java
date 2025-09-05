package com.balsam.oasis.common.query.dialect;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating database dialect instances.
 */
public class DialectFactory {

    private static final Map<String, DatabaseDialect> DIALECTS = new HashMap<>();

    static {
        DIALECTS.put("ORACLE_11G", new Oracle11gDialect());
        DIALECTS.put("ORACLE_12C", new Oracle12cDialect());
    }

    /**
     * Get a dialect by name.
     * 
     * @param dialectName the name of the dialect
     * @return the dialect instance, or StandardSqlDialect if not found
     */
    public static DatabaseDialect getDialect(String dialectName) {
        return DIALECTS.getOrDefault(dialectName, DIALECTS.get("ORACLE_11G"));
    }

    /**
     * Register a custom dialect.
     * 
     * @param name    the dialect name
     * @param dialect the dialect implementation
     */
    public static void registerDialect(String name, DatabaseDialect dialect) {
        DIALECTS.put(name, dialect);
    }
}