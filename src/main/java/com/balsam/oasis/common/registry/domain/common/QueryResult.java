package com.balsam.oasis.common.registry.domain.common;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.google.common.collect.ImmutableList;

import lombok.Builder;
import lombok.Value;

/**
 * Simple container for query execution results
 */
@Value
@Builder(toBuilder = true)
public class QueryResult {

    @Builder.Default
    List<QueryRow> rows = ImmutableList.of();

    QueryMetadata metadata;

    QueryContext context;

    Long executionTimeMs;

    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }

    public int size() {
        return rows != null ? rows.size() : 0;
    }

    public int getCount() {
        // If we have pagination metadata with total, use that
        if (metadata != null && metadata.getPagination() != null) {
            return metadata.getPagination().getTotal();
        }
        // Otherwise return actual row count
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

    // For backward compatibility
    public boolean isSuccess() {
        return true; // If we got here, it was successful
    }
}