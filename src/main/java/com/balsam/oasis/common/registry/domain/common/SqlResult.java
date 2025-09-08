package com.balsam.oasis.common.registry.domain.common;

import java.util.Map;

public class SqlResult {
    private final String sql;
    private final Map<String, Object> params;

    public SqlResult(String sql, Map<String, Object> params) {
        this.sql = sql;
        this.params = params;
    }

    public String getSql() {
        return sql;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}