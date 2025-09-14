package com.balsam.oasis.common.registry.domain.common;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified data container for query operations.
 * Consolidates functionality of QueryData, SqlResult, and QueryRow into a
 * single class.
 * Supports row-level data access, SQL building results, and query execution
 * results.
 */
@Value
@Builder(toBuilder = true)
public class QueryData {

    // Query Result fields
    @Builder.Default
    List<QueryRow> rows = ImmutableList.of();
    QueryMetadata metadata;
    QueryContext context;
    Long executionTimeMs;

    // SQL Building fields
    String sql;
    Map<String, Object> params;

    // Row data fields
    Map<String, Object> rowData;

    // ============= QUERY RESULT METHODS =============

    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }

    public int size() {
        return rows != null ? rows.size() : 0;
    }

    public int getCount() {
        if (metadata != null && metadata.getPagination() != null) {
            return metadata.getPagination().getTotal();
        }
        return size();
    }

    public boolean hasMetadata() {
        return metadata != null;
    }

    public QueryRow getFirstRow() {
        if (!isEmpty()) {
            return rows.get(0);
        }
        return null;
    }

    public List<Map<String, Object>> getData() {
        if (rows != null && !rows.isEmpty()) {
            return rows.stream()
                    .map(QueryRow::toMap)
                    .collect(ImmutableList.toImmutableList());
        }
        return ImmutableList.of();
    }

    public boolean isSuccess() {
        return true;
    }

    // ============= SQL RESULT METHODS =============

    public String getSql() {
        return sql;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    // ============= ROW DATA METHODS =============

    public Object get(String key) {
        if (rowData == null)
            return null;
        Object value = rowData.get(key);
        if (value != null) {
            return value;
        }
        return rowData.get(key.toUpperCase());
    }

    public Object getRaw(String columnName) {
        if (rowData == null)
            return null;
        return rowData.get(columnName.toUpperCase());
    }

    public void set(String key, Object value) {
        if (rowData == null) {
            throw new IllegalStateException("Cannot set values on QueryData without row data");
        }
        ((Map<String, Object>) rowData).put(key, value);
    }

    public Map<String, Object> toMap() {
        return rowData != null ? new HashMap<>(rowData) : new HashMap<>();
    }

    public boolean has(String key) {
        return rowData != null && (rowData.containsKey(key) || rowData.containsKey(key.toUpperCase()));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

    public Integer getInteger(String key) {
        return get(key, Integer.class);
    }

    public Long getLong(String key) {
        return get(key, Long.class);
    }

    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }

    // ============= FACTORY METHODS =============

    // Create as QueryData
    public static QueryData asResult(List<QueryRow> rows, QueryContext context, QueryMetadata metadata,
            Long executionTime) {
        return QueryData.builder()
                .rows(rows)
                .context(context)
                .metadata(metadata)
                .executionTimeMs(executionTime)
                .build();
    }

    // Create as SqlResult
    public static QueryData asSql(String sql, Map<String, Object> params) {
        return QueryData.builder()
                .sql(sql)
                .params(params)
                .build();
    }

    // Create as QueryRow
    public static QueryData asRow(Map<String, Object> data, QueryContext context) {
        return QueryData.builder()
                .rowData(new HashMap<>(data))
                .context(context)
                .build();
    }
}