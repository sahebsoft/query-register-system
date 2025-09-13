package com.balsam.oasis.common.registry.web.builder;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.web.dto.response.QueryListResponse;
import com.balsam.oasis.common.registry.web.dto.response.QuerySingleResponse;
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
    public ResponseEntity<?> build(QueryResult result, String queryName) {
        QueryListResponse response = buildFormattedResponse(result);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for single result
     */
    public ResponseEntity<QuerySingleResponse> buildSingle(QueryResult result, String queryName) {
        QuerySingleResponse response = buildFormattedSingleResponse(result);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Build JSON response for select/dropdown queries
     * Converts QueryResult to select response format with SelectItem objects
     */
    public ResponseEntity<?> buildSelectResponse(QueryResult queryResult) {
        QueryListResponse response = buildFormattedSelectResponse(queryResult);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private QueryListResponse buildFormattedResponse(QueryResult result) {
        Object securityContext = result.getContext() != null ? result.getContext().getSecurityContext() : null;
        QueryDefinitionBuilder definition = result.getContext() != null ? result.getContext().getDefinition() : null;

        List<?> formattedData;
        if (definition != null && !result.getRows().isEmpty()) {
            formattedData = formatter.formatRows(result.getRows(), definition, securityContext);
        } else {
            formattedData = result.getData();
        }

        return QueryListResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .count(result.getCount())
                .success(result.isSuccess())
                .build();
    }

    private QuerySingleResponse buildFormattedSingleResponse(QueryResult result) {
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

        return QuerySingleResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .success(result.isSuccess())
                .build();
    }

    private QueryListResponse buildFormattedSelectResponse(QueryResult queryResult) {
        QueryDefinitionBuilder definition = queryResult.getContext().getDefinition();
        if (!definition.getAttributes().containsKey("value") ||
                !definition.getAttributes().containsKey("label")) {
            throw new QueryException("Select query must have 'value' and 'label' attributes",
                    "INVALID_SELECT_DEFINITION", definition.getName());
        }

        Object securityContext = queryResult.getContext() != null ? queryResult.getContext().getSecurityContext() : null;

        List<SelectItem> selectItems = new ArrayList<>();
        for (QueryRow row : queryResult.getRows()) {
            Map<String, Object> formattedRow = formatter.formatRow(row, definition, securityContext);

            String value = String.valueOf(formattedRow.get("value"));
            String label = String.valueOf(formattedRow.get("label"));

            Map<String, Object> additions = null;
            List<String> additionAttrNames = definition.getAttributes().keySet().stream()
                    .filter(name -> !"value".equals(name) && !"label".equals(name))
                    .toList();

            if (!additionAttrNames.isEmpty()) {
                additions = new HashMap<>();
                for (String attrName : additionAttrNames) {
                    additions.put(attrName, formattedRow.get(attrName));
                }
            }

            selectItems.add(SelectItem.of(value, label, additions));
        }

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