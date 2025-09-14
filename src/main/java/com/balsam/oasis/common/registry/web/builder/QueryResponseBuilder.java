package com.balsam.oasis.common.registry.web.builder;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;
import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.balsam.oasis.common.registry.domain.select.SelectItem;
import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds JSON HTTP responses from query results.
 * Uses pre-formatted data from QueryRows (formatting is done during row processing).
 */
@Slf4j
public class QueryResponseBuilder {

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
    public ResponseEntity<QueryResponse> buildSelectResponse(QueryData queryData) {
        QueryResponse response = buildFormattedSelectResponse(queryData);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private QueryResponse buildFormattedResponse(QueryData result) {
        // Use pre-formatted data from QueryRows (formatting already done during row processing)
        List<?> formattedData = buildResponseData(result);

        return QueryResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .count(result.getCount())
                .success(result.isSuccess())
                .build();
    }

    /**
     * Build response data applying only security rules (formatting already done in row processing)
     */
    private List<Map<String, Object>> buildResponseData(QueryData result) {
        if (result.getRows().isEmpty()) {
            return result.getData();
        }

        Object securityContext = result.getContext() != null ? result.getContext().getSecurityContext() : null;
        QueryDefinitionBuilder definition = result.getContext() != null ? result.getContext().getDefinition() : null;

        // If no definition or no security rules, just return the formatted data directly
        if (definition == null || !hasSecurityRules(definition)) {
            return result.getData();
        }

        // Apply security rules to pre-formatted data
        List<Map<String, Object>> securedData = new ArrayList<>();
        for (QueryRow row : result.getRows()) {
            Map<String, Object> securedRow = applySecurityRules(row.toMap(), definition, securityContext);
            securedData.add(securedRow);
        }

        return securedData;
    }

    private boolean hasSecurityRules(QueryDefinitionBuilder definition) {
        return definition.getAttributes().values().stream()
                .anyMatch(AttributeDef::isSecured);
    }

    private Map<String, Object> applySecurityRules(Map<String, Object> rowData, QueryDefinitionBuilder definition, Object securityContext) {
        if (securityContext == null) {
            return rowData;
        }

        Map<String, Object> securedData = new HashMap<>(rowData);

        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            if (attr.isSecured()) {
                Boolean allowed = attr.securityRule().apply(securityContext);
                if (!Boolean.TRUE.equals(allowed)) {
                    securedData.put(attrName, null); // Hide secured data
                }
            }
        }

        return securedData;
    }

    private QueryResponse buildFormattedSingleResponse(QueryData result) {
        if (result == null || result.getRows().isEmpty()) {
            throw new QueryException("No data found", "NOT_FOUND", (String) null);
        }

        QueryRow firstRow = result.getRows().get(0);
        Object securityContext = result.getContext() != null ? result.getContext().getSecurityContext() : null;
        QueryDefinitionBuilder definition = result.getContext() != null ? result.getContext().getDefinition() : null;

        // Use pre-formatted data from QueryRow, apply security rules if needed
        Object formattedData;
        if (definition != null && hasSecurityRules(definition) && securityContext != null) {
            formattedData = applySecurityRules(firstRow.toMap(), definition, securityContext);
        } else {
            formattedData = firstRow.toMap();
        }

        return QueryResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .success(result.isSuccess())
                .build();
    }

    private QueryResponse buildFormattedSelectResponse(QueryData queryData) {
        QueryDefinitionBuilder definition = queryData.getContext().getDefinition();

        // Check if query has value and label attributes
        String valueAttr = definition.getValueAttribute();
        String labelAttr = definition.getLabelAttribute();

        if (valueAttr == null || labelAttr == null) {
            throw new QueryException("Select query must have value and label attributes defined",
                    "INVALID_SELECT_DEFINITION", definition.getName());
        }

        Object securityContext = queryData.getContext() != null ? queryData.getContext().getSecurityContext()
                : null;

        List<SelectItem> selectItems = new ArrayList<>();
        for (QueryRow row : queryData.getRows()) {
            // Use pre-formatted data from QueryRow, apply security rules if needed
            Map<String, Object> rowData;
            if (definition != null && hasSecurityRules(definition) && securityContext != null) {
                rowData = applySecurityRules(row.toMap(), definition, securityContext);
            } else {
                rowData = row.toMap();
            }

            // Get value and label using the configured attribute names
            String value = String.valueOf(rowData.get(valueAttr));
            String label = String.valueOf(rowData.get(labelAttr));

            // Create simple SelectItem without additions
            selectItems.add(SelectItem.of(value, label));
        }

        QueryMetadata selectMetadata = null;
        if (queryData.hasMetadata() && queryData.getMetadata().getPagination() != null) {
            selectMetadata = QueryMetadata.builder()
                    .pagination(queryData.getMetadata().getPagination())
                    .build();
        }

        return QueryResponse.builder()
                .data(selectItems)
                .metadata(selectMetadata)
                .count(queryData.getCount())
                .success(true)
                .build();
    }

}