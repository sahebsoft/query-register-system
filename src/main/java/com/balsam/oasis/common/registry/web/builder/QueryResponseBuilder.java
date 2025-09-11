package com.balsam.oasis.common.registry.web.builder;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.web.dto.response.QueryListResponse;
import com.balsam.oasis.common.registry.web.dto.response.QuerySingleResponse;

/**
 * Builds JSON HTTP responses from query results
 */
public class QueryResponseBuilder {

    /**
     * Build JSON response from query result
     */
    public ResponseEntity<?> build(QueryResult result, String queryName) {
        QueryListResponse response = QueryListResponse.from(result);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for single result
     */
    public ResponseEntity<QuerySingleResponse> buildSingle(QueryResult result, String queryName) {
        // QuerySingleResponse.from() will throw exception if no data found
        QuerySingleResponse response = QuerySingleResponse.from(result);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for select/dropdown queries
     * Converts QueryResult to select response format with SelectItem objects
     */
    public ResponseEntity<?> buildSelectResponse(QueryResult queryResult) {
        // QueryListResponse.fromSelect() handles validation and transformation
        QueryListResponse response = QueryListResponse.fromSelect(queryResult);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

}