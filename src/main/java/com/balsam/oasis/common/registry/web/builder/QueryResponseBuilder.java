package com.balsam.oasis.common.registry.web.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.balsam.oasis.common.registry.domain.definition.MetadataContext;
import com.balsam.oasis.common.registry.domain.definition.QueryDefinition;
import com.balsam.oasis.common.registry.domain.execution.QueryResult;
import com.balsam.oasis.common.registry.domain.result.Row;
import com.balsam.oasis.common.registry.domain.select.SelectItem;
import com.balsam.oasis.common.registry.exception.QueryException;
import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;
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
        QueryResponse response = QueryResponse.from(result);

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

    /**
     * Build JSON response for single result with metadata
     */
    public ResponseEntity<?> buildSingleWithMetadata(Object singleResult, String queryName, MetadataContext context) {
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

        // TODO: Build context-aware metadata
        Map<String, Object> metadata = ImmutableMap.of(
                "context", context.name(),
                "queryName", queryName);

        QueryResponse response = QueryResponse.success(List.of(data), metadata, 1);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for select/dropdown queries
     * Converts QueryResult to select response format with SelectItem objects
     */
    public ResponseEntity<?> buildSelectResponse(QueryResult queryResult, QueryDefinition definition) {
        // Validate that we have the required "value" and "label" attributes
        if (!definition.getAttributes().containsKey("value") ||
                !definition.getAttributes().containsKey("label")) {
            throw new QueryException("Select query must have 'value' and 'label' attributes",
                    "INVALID_SELECT_DEFINITION", definition.getName());
        }

        List<SelectItem> selectItems = new ArrayList<>();

        for (Row row : queryResult.getRows()) {
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
        Map<String, Object> selectMetadata = new HashMap<>();
        if (queryResult.hasMetadata() && queryResult.getMetadata().getPagination() != null) {
            selectMetadata.put("pagination", queryResult.getMetadata().getPagination());
        }

        QueryResponse response = QueryResponse.success(
                selectItems,
                selectMetadata.isEmpty() ? null : selectMetadata,
                queryResult.getCount());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

}