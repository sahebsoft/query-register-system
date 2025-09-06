package com.balsam.oasis.common.registry.query;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.core.result.Row;
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

    @Builder.Default
    List<Map<String, Object>> data = ImmutableList.of();

    QueryMetadata metadata;

    @Builder.Default
    Map<String, Object> summary = ImmutableMap.of();

    QueryContext context;

    Long executionTimeMs;

    @Builder.Default
    boolean success = true;

    String errorMessage;

    @Value
    @Builder
    public static class QueryMetadata {
        PaginationMetadata pagination;
        List<AttributeMetadata> attributes;
        List<QueryContext.AppliedCriteria> appliedCriteria;
        Map<String, FilterMetadata> appliedFilters;
        List<SortMetadata> appliedSort;
        Map<String, ParameterMetadata> parameters;
        PerformanceMetadata performance;

        @Value
        @Builder
        public static class PaginationMetadata {
            int start;
            int end;
            int total;
            boolean hasNext;
            boolean hasPrevious;
            int pageSize;
            int pageCount;
            int currentPage;
        }

        @Value
        @Builder
        public static class AttributeMetadata {
            String name;
            String type;
            boolean filterable;
            boolean sortable;
            boolean virtual;
            boolean restricted;
            String restrictionReason;

            // UI metadata fields for frontend display
            String label; // Display label for the attribute
            String labelKey; // i18n key for the label
            String width; // Display width (e.g., "100px", "20%")
            String flex; // Flex value for flexible layouts (e.g., "1", "2")

            // Table context metadata
            String headerText;
            String alignment;
            Integer displayOrder;
            Boolean visible;

            // Form context metadata
            String placeholder;
            String helpText;
            String inputType;
            Boolean required;
            Integer maxLength;
            Integer minLength;
            String pattern;
            String validationMsg;
        }

        @Value
        @Builder
        public static class FilterMetadata {
            String attribute;
            String operator;
            Object value;
            Object value2;
            List<Object> values;
        }

        @Value
        @Builder
        public static class SortMetadata {
            String field;
            String direction;
            int priority;
        }

        @Value
        @Builder
        public static class ParameterMetadata {
            String name;
            Object value;
            String type;
            boolean required;
        }

        @Value
        @Builder
        public static class PerformanceMetadata {
            long executionTimeMs;
            int rowsFetched;
            int totalRowsScanned;
            boolean cacheHit;
            String queryPlan;
            Map<String, Object> additionalMetrics;
        }
    }

    // Helper methods
    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }

    public int size() {
        return rows != null ? rows.size() : 0;
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

    public List<Map<String, Object>> toListOfMaps() {
        if (data != null && !data.isEmpty()) {
            return data;
        }
        if (rows != null && !rows.isEmpty()) {
            return rows.stream()
                    .map(Row::toMap)
                    .collect(ImmutableList.toImmutableList());
        }
        return ImmutableList.of();
    }
}