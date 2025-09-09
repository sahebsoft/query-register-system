package com.balsam.oasis.common.registry.domain.common;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.balsam.oasis.common.registry.domain.result.Row;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable query result container with data and metadata
 */
@Value
@Builder(toBuilder = true)
public class QueryResult {

    @Builder.Default
    List<Row> rows = ImmutableList.of();

    QueryMetadata metadata;

    @Builder.Default
    Map<String, Object> summary = ImmutableMap.of();

    QueryContext context;

    Long executionTimeMs;

    @Builder.Default
    boolean success = true;

    @Builder.Default
    Integer count = 0;

    String errorMessage;

    // Helper methods
    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }

    public int size() {
        return rows != null ? rows.size() : 0;
    }

    public int getCount() {
        // Use pagination metadata total if available, otherwise fall back to count or
        // data size
        if (metadata != null && metadata.getPagination() != null) {
            return metadata.getPagination().getTotal();
        }
        return count != null ? count : size();
    }

    public boolean hasMetadata() {
        return metadata != null;
    }

    public boolean hasErrors() {
        return !success || errorMessage != null;
    }

    public Row getFirstRow() {
        if (!isEmpty()) {
            return rows.get(0);
        }
        return null;
    }

    public List<Map<String, Object>> getData() {
        if (rows != null && !rows.isEmpty()) {
            return rows.stream()
                    .map(Row::toMap)
                    .collect(ImmutableList.toImmutableList());
        }
        return ImmutableList.of();
    }
}