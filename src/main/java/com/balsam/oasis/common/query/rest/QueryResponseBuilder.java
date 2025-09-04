package com.balsam.oasis.common.query.rest;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.balsam.oasis.common.query.core.result.QueryResult;
import com.balsam.oasis.common.query.core.result.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

/**
 * Builds JSON HTTP responses from query results
 */
public class QueryResponseBuilder {

    private final ObjectMapper objectMapper;

    public QueryResponseBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Build JSON response from query result
     */
    public ResponseEntity<?> build(QueryResult result, String queryName) {
        Map<String, Object> response = ImmutableMap.of(
                "data", result.toListOfMaps(),
                "metadata", result.getMetadata() != null ? result.getMetadata() : ImmutableMap.of());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for single result
     */
    public ResponseEntity<?> buildSingle(Object singleResult, String queryName) {
        if (singleResult == null) {
            return ResponseEntity.notFound().build();
        }

        // Convert Row to Map if needed
        Map<?, ?> data;
        switch (singleResult) {
            case Row row -> data = row.toMap();
            case Map<?, ?> mapResult -> data = mapResult;
            default -> data = ImmutableMap.of("result", singleResult);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

}