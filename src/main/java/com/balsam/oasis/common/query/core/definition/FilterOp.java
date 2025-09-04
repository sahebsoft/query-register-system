package com.balsam.oasis.common.query.core.definition;

/**
 * Supported filter operators for query attributes
 */
public enum FilterOp {
    EQUALS("=", "eq"),
    NOT_EQUALS("!=", "ne"),
    GREATER_THAN(">", "gt"),
    GREATER_THAN_OR_EQUAL(">=", "gte"),
    LESS_THAN("<", "lt"),
    LESS_THAN_OR_EQUAL("<=", "lte"),
    LIKE("LIKE", "like"),
    NOT_LIKE("NOT LIKE", "notlike"),
    IN("IN", "in"),
    NOT_IN("NOT IN", "notin"),
    BETWEEN("BETWEEN", "between"),
    IS_NULL("IS NULL", "null"),
    IS_NOT_NULL("IS NOT NULL", "notnull"),
    CONTAINS("CONTAINS", "contains"),
    STARTS_WITH("STARTS WITH", "startswith"),
    ENDS_WITH("ENDS WITH", "endswith");

    private final String sqlOperator;
    private final String urlShortcut;

    FilterOp(String sqlOperator, String urlShortcut) {
        this.sqlOperator = sqlOperator;
        this.urlShortcut = urlShortcut;
    }

    public String getSqlOperator() {
        return sqlOperator;
    }

    public String getUrlShortcut() {
        return urlShortcut;
    }

    public static FilterOp fromUrlShortcut(String shortcut) {
        for (FilterOp op : values()) {
            if (op.urlShortcut.equalsIgnoreCase(shortcut)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown filter operator shortcut: " + shortcut);
    }

    public boolean requiresValue() {
        return this != IS_NULL && this != IS_NOT_NULL;
    }

    public boolean requiresTwoValues() {
        return this == BETWEEN;
    }

    public boolean supportsMultipleValues() {
        return this == IN || this == NOT_IN;
    }
}