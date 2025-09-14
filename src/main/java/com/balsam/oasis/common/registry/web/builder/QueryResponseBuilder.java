package com.balsam.oasis.common.registry.web.builder;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;
import com.balsam.oasis.common.registry.web.formatter.ResponseFormatter;
import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.balsam.oasis.common.registry.domain.select.SelectItem;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds JSON HTTP responses from query results
 */
@Slf4j
public class QueryResponseBuilder {

    private final ResponseFormatter formatter = new ResponseFormatter();

    /**
     * Build JSON response from query result
     */
    public ResponseEntity<QueryResponse> build(QueryData result, String queryName) {
        QueryResponse response = buildFormattedResponse(result);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for single result
     */
    public ResponseEntity<QueryResponse> buildSingle(QueryData result, String queryName) {
        QueryResponse response = buildFormattedSingleResponse(result);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for select/dropdown queries
     * Converts QueryData to select response format with SelectItem objects
     */
    public ResponseEntity<QueryResponse> buildSelectResponse(QueryData queryResult) {
        QueryResponse response = buildFormattedSelectResponse(queryResult);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private QueryResponse buildFormattedResponse(QueryData result) {
        Object securityContext = result.getContext() != null ? result.getContext().getSecurityContext() : null;
        QueryDefinitionBuilder definition = result.getContext() != null ? result.getContext().getDefinition() : null;

        List<?> formattedData;
        if (definition != null && !result.getRows().isEmpty()) {
            formattedData = formatter.formatRows(result.getRows(), definition, securityContext);
        } else {
            formattedData = result.getData();
        }

        return QueryResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .count(result.getCount())
                .success(result.isSuccess())
                .build();
    }

    private QueryResponse buildFormattedSingleResponse(QueryData result) {
        if (result == null || result.getRows().isEmpty()) {
            throw new QueryException("No data found", "NOT_FOUND", (String) null);
        }

        Object securityContext = result.getContext() != null ? result.getContext().getSecurityContext() : null;
        QueryDefinitionBuilder definition = result.getContext() != null ? result.getContext().getDefinition() : null;

        Object formattedData;
        if (definition != null) {
            formattedData = formatter.formatRow(result.getRows().get(0), definition, securityContext);
        } else {
            formattedData = result.getRows().get(0).toMap();
        }

        return QueryResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .success(result.isSuccess())
                .build();
    }

    private QueryResponse buildFormattedSelectResponse(QueryData queryResult) {
        QueryDefinitionBuilder definition = queryResult.getContext().getDefinition();

        // Check if query has value and label attributes
        String valueAttr = definition.getValueAttribute();
        String labelAttr = definition.getLabelAttribute();

        if (valueAttr == null || labelAttr == null) {
            throw new QueryException("Select query must have value and label attributes defined",
                    "INVALID_SELECT_DEFINITION", definition.getName());
        }

        Object securityContext = queryResult.getContext() != null ? queryResult.getContext().getSecurityContext()
                : null;

        List<SelectItem> selectItems = new ArrayList<>();
        for (QueryRow row : queryResult.getRows()) {
            Map<String, Object> formattedRow = formatter.formatRow(row, definition, securityContext);

            // Get value and label using the configured attribute names
            String value = String.valueOf(formattedRow.get(valueAttr));
            String label = String.valueOf(formattedRow.get(labelAttr));

            // Create simple SelectItem without additions
            selectItems.add(SelectItem.of(value, label));
        }

        QueryMetadata selectMetadata = null;
        if (queryResult.hasMetadata() && queryResult.getMetadata().getPagination() != null) {
            selectMetadata = QueryMetadata.builder()
                    .pagination(queryResult.getMetadata().getPagination())
                    .build();
        }

        return QueryResponse.builder()
                .data(selectItems)
                .metadata(selectMetadata)
                .count(queryResult.getCount())
                .success(true)
                .build();
    }

}