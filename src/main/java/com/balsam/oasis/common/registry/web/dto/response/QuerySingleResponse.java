package com.balsam.oasis.common.registry.web.dto.response;

import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.balsam.oasis.common.registry.exception.QueryException;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response structure for single object queries (find-by-key operations).
 * Unlike QueryListResponse, this contains a single data object without a count field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuerySingleResponse implements QuerySuccessResponse {

    /**
     * The response data (single object)
     */
    private Object data;

    /**
     * Optional metadata including query context, execution time
     */
    private QueryMetadata metadata;

    /**
     * Whether the operation was successful
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Create a successful single response with just data
     */
    public static QuerySingleResponse success(Object data) {
        return QuerySingleResponse.builder()
                .data(data)
                .success(true)
                .build();
    }

    /**
     * Create a successful single response with data and metadata
     */
    public static QuerySingleResponse success(Object data, QueryMetadata metadata) {
        return QuerySingleResponse.builder()
                .data(data)
                .metadata(metadata)
                .success(true)
                .build();
    }

    /**
     * Create a single response from QueryResult (takes first row)
     * @throws QueryException if no data found
     */
    public static QuerySingleResponse from(QueryResult queryResult) {
        if (queryResult == null || queryResult.getRows().isEmpty()) {
            throw new QueryException("No data found", "NOT_FOUND", (String) null);
        }
        
        // Get first row as single result
        Object data = queryResult.getRows().get(0).toMap();
        
        return QuerySingleResponse.builder()
                .data(data)
                .metadata(queryResult.getMetadata())
                .success(queryResult.isSuccess())
                .build();
    }
}