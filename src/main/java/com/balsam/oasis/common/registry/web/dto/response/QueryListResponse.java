package com.balsam.oasis.common.registry.web.dto.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.balsam.oasis.common.registry.domain.select.SelectItem;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.balsam.oasis.common.registry.exception.QueryException;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response structure for all query and select APIs.
 * Provides consistent shape with data, metadata, count, and success fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryListResponse implements QuerySuccessResponse {

    /**
     * The response data (list of results)
     */
    private List<?> data;

    /**
     * Optional metadata including pagination, applied criteria, execution time
     */
    private Object metadata;

    /**
     * Number of items in the response (uses pagination total if available)
     */
    private Integer count;

    /**
     * Whether the operation was successful
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Create a successful query response from QueryResult
     */
    public static QueryListResponse success(List<?> data, QueryMetadata metadata, Integer count) {
        return QueryListResponse.builder()
                .data(data)
                .metadata(metadata)
                .count(count)
                .success(true)
                .build();
    }

    public static QueryListResponse success(List<?> data, Integer count) {
        return QueryListResponse.builder()
                .data(data)
                .count(count)
                .success(true)
                .build();
    }

    /**
     * Create a successful query response from QueryResult
     */
    public static QueryListResponse from(QueryResult queryResult) {
        return QueryListResponse.builder()
                .data(queryResult.getData())
                .metadata(queryResult.getMetadata())
                .count(queryResult.getCount())
                .success(queryResult.isSuccess())
                .build();
    }

    /**
     * Create a successful response with custom data and metadata
     */
    public static QueryListResponse success(List<?> data, Object metadata) {
        return QueryListResponse.builder()
                .data(data)
                .metadata(metadata)
                .count(data != null ? data.size() : 0)
                .success(true)
                .build();
    }

    /**
     * Create a successful response with just data
     */
    public static QueryListResponse success(List<?> data) {
        return QueryListResponse.builder()
                .data(data)
                .count(data != null ? data.size() : 0)
                .success(true)
                .build();
    }

    /**
     * Create a select response from QueryResult for dropdown/select queries
     * Transforms rows into SelectItem objects
     */
    public static QueryListResponse fromSelect(QueryResult queryResult) {
        QueryDefinition definition = queryResult.getContext().getDefinition();
        // Validate that we have the required "value" and "label" attributes
        if (!definition.getAttributes().containsKey("value") ||
                !definition.getAttributes().containsKey("label")) {
            throw new QueryException("Select query must have 'value' and 'label' attributes",
                    "INVALID_SELECT_DEFINITION", definition.getName());
        }

        List<SelectItem> selectItems = new ArrayList<>();

        for (QueryRow row : queryResult.getRows()) {
            String value = String.valueOf(row.get("value"));
            String label = String.valueOf(row.get("label"));

            // Build additions from attributes other than value and label
            Map<String, Object> additions = null;
            List<String> additionAttrNames = definition.getAttributes().keySet().stream()
                    .filter(name -> !"value".equals(name) && !"label".equals(name))
                    .toList();

            if (!additionAttrNames.isEmpty()) {
                additions = new HashMap<>();
                for (String attrName : additionAttrNames) {
                    additions.put(attrName, row.get(attrName));
                }
            }

            selectItems.add(SelectItem.of(value, label, additions));
        }

        // Build the response with minimal metadata (only pagination, no attributes)
        QueryMetadata selectMetadata = null;
        if (queryResult.hasMetadata() && queryResult.getMetadata().getPagination() != null) {
            selectMetadata = QueryMetadata.builder()
                    .pagination(queryResult.getMetadata().getPagination())
                    .build();
        }

        return selectMetadata != null
                ? QueryListResponse.success(selectItems, selectMetadata, queryResult.getCount())
                : QueryListResponse.success(selectItems, queryResult.getCount());
    }
}