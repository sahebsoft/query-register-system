package com.balsam.oasis.common.registry.web.dto.response;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.execution.QueryResult;
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
public class QueryResponse {

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
    public static QueryResponse success(List<?> data, Object metadata, Integer count) {
        return QueryResponse.builder()
                .data(data)
                .metadata(metadata)
                .count(count)
                .success(true)
                .build();
    }

    /**
     * Create a successful query response from QueryResult
     */
    public static QueryResponse from(QueryResult queryResult) {
        return QueryResponse.builder()
                .data(queryResult.toListOfMaps())
                .metadata(queryResult.getMetadata())
                .count(queryResult.getCount())
                .success(queryResult.isSuccess())
                .build();
    }

    /**
     * Create a successful response with custom data and metadata
     */
    public static QueryResponse success(List<?> data, Object metadata) {
        return QueryResponse.builder()
                .data(data)
                .metadata(metadata)
                .count(data != null ? data.size() : 0)
                .success(true)
                .build();
    }

    /**
     * Create a successful response with just data
     */
    public static QueryResponse success(List<?> data) {
        return QueryResponse.builder()
                .data(data)
                .count(data != null ? data.size() : 0)
                .success(true)
                .build();
    }
}