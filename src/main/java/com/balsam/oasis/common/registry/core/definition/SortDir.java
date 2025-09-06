package com.balsam.oasis.common.registry.core.definition;

/**
 * Sort direction for query ordering
 */
public enum SortDir {
    ASC("ASC", "asc"),
    DESC("DESC", "desc");

    private final String sql;
    private final String urlParam;

    SortDir(String sql, String urlParam) {
        this.sql = sql;
        this.urlParam = urlParam;
    }

    public String getSql() {
        return sql;
    }

    public String getUrlParam() {
        return urlParam;
    }

    public static SortDir fromUrlParam(String param) {
        for (SortDir dir : values()) {
            if (dir.urlParam.equalsIgnoreCase(param)) {
                return dir;
            }
        }
        return ASC; // Default to ASC if not recognized
    }
}