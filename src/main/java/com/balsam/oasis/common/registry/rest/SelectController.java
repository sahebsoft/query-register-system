package com.balsam.oasis.common.registry.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.balsam.oasis.common.registry.core.definition.FilterOp;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.exception.QueryException;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.balsam.oasis.common.registry.query.QueryExecution;
import com.balsam.oasis.common.registry.query.QueryExecutor;
import com.balsam.oasis.common.registry.query.QueryRegistry;
import com.balsam.oasis.common.registry.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for List of Values (LOV) / Select endpoints.
 * Provides simplified data for dropdown/select UI components.
 * Now consolidated to use the unified Query infrastructure.
 */
@RestController
@RequestMapping("/api/select")
@Tag(name = "Select API", description = "List of Values endpoints for dropdowns and select components")
public class SelectController {

    private static final Logger log = LoggerFactory.getLogger(SelectController.class);

    private final QueryExecutor queryExecutor;
    private final QueryRegistry queryRegistry;

    public SelectController(QueryExecutor queryExecutor,
            QueryRegistry queryRegistry) {
        this.queryExecutor = queryExecutor;
        this.queryRegistry = queryRegistry;
    }

    @GetMapping("/{selectName}")
    @Operation(summary = "Get list of values", description = "Execute a select query for dropdowns/selects")
    public ResponseEntity<?> getListOfValues(
            @PathVariable @Parameter(description = "Name of the registered select") String selectName,
            @RequestParam(required = false) @Parameter(description = "IDs to fetch (for default values)") List<String> id,
            @RequestParam(required = false) @Parameter(description = "Search term to filter results") String search,
            @RequestParam(defaultValue = "0") @Parameter(description = "Start index for pagination") int _start,
            @RequestParam(defaultValue = "100") @Parameter(description = "End index for pagination") int _end,
            @RequestParam MultiValueMap<String, String> allParams) {

        log.info("Executing select: {} with ids: {}, search: {}, pagination: {}-{}",
                selectName, id, search, _start, _end);

        try {
            // Get the query definition
            QueryDefinition queryDefinition = queryRegistry.get(selectName);

            if (queryDefinition == null) {
                log.error("Select query not found: {}", selectName);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse(new QueryException(
                                "Select query not found: " + selectName, "NOT_FOUND", selectName)));
            }

            // Create execution using QueryExecution
            QueryExecution execution = queryExecutor.execute(queryDefinition);

            // Handle ID fetching (for default values) - convert to IN filter on value attribute
            if (id != null && !id.isEmpty()) {
                log.debug("Fetching by IDs: {}", id);
                execution.withFilter("value", FilterOp.IN, id);
            }
            // Handle search
            else if (search != null && !search.isEmpty()) {
                log.debug("Searching with term: {}", search);
                
                // Check if query has search parameter or search criteria
                boolean hasSearchParam = queryDefinition.getParams().containsKey("search");
                boolean hasSearchCriteria = queryDefinition.getCriteria().containsKey("search") || 
                                          queryDefinition.getCriteria().containsKey("searchFilter");
                
                if (hasSearchParam || hasSearchCriteria) {
                    // Use search parameter if query supports it
                    execution.withParam("search", search);
                } else {
                    // Fallback to filtering on label column with LIKE
                    execution.withFilter("label", FilterOp.LIKE, "%" + search + "%");
                }
            }

            // Parse other parameters (excluding special ones)
            Map<String, Object> params = parseParameters(allParams, queryDefinition);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                execution.withParam(entry.getKey(), entry.getValue());
            }

            // Apply pagination
            execution.withPagination(_start, _end - _start);

            // Execute and convert to SelectResponse
            QueryResult queryResult = execution.execute();
            SelectResponse response = convertToSelectResponse(queryResult, queryDefinition);
            return ResponseEntity.ok(response);

        } catch (QueryException e) {
            log.error("Select execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error executing select: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }

    /**
     * Parse parameters from request, excluding special parameters
     */
    private Map<String, Object> parseParameters(MultiValueMap<String, String> allParams,
            QueryDefinition definition) {
        Map<String, Object> params = new HashMap<>();

        // Special parameters to exclude
        Set<String> excludedParams = Set.of("id", "search", "_start", "_end");

        for (Map.Entry<String, ParamDef<?>> entry : definition.getParams().entrySet()) {
            String paramName = entry.getKey();
            if (!excludedParams.contains(paramName) && allParams.containsKey(paramName)) {
                params.put(paramName, allParams.getFirst(paramName));
            }
        }

        return params;
    }

    /**
     * Convert QueryResult to SelectResponse format
     * Uses "value" and "label" attributes from the query definition.
     * Additional attributes are added to the additions map.
     */
    private SelectResponse convertToSelectResponse(QueryResult queryResult, QueryDefinition definition) {
        List<SelectItem> items = new ArrayList<>();
        
        // Validate that we have the required "value" and "label" attributes
        if (!definition.getAttributes().containsKey("value") || 
            !definition.getAttributes().containsKey("label")) {
            throw new QueryException("Select query must have 'value' and 'label' attributes", 
                                   "INVALID_SELECT_DEFINITION", definition.getName());
        }
        
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
            
            items.add(SelectItem.of(value, label, additions));
        }
        
        // Build metadata if available
        Map<String, Object> metadata = null;
        if (queryResult.getMetadata() != null && queryResult.getMetadata().getPagination() != null) {
            metadata = new HashMap<>();
            metadata.put("pagination", queryResult.getMetadata().getPagination());
        }
        
        return SelectResponse.of(items, metadata);
    }

    private HttpStatus determineHttpStatus(QueryException e) {
        String errorCode = e.getErrorCode();
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (errorCode) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR", "DEFINITION_ERROR", "LOV_NOT_SUPPORTED" -> HttpStatus.BAD_REQUEST;
            case "SECURITY_ERROR" -> HttpStatus.FORBIDDEN;
            case "TIMEOUT_ERROR" -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ErrorResponse buildErrorResponse(Exception e) {
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .message(e.getMessage())
                .timestamp(System.currentTimeMillis());

        if (e instanceof QueryException qe) {
            builder.code(qe.getErrorCode())
                    .queryName(qe.getQueryName());
        } else {
            builder.code("INTERNAL_ERROR");
        }

        return builder.build();
    }
}