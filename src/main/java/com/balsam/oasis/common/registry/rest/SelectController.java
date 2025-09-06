package com.balsam.oasis.common.registry.rest;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.exception.QueryException;
import com.balsam.oasis.common.registry.select.SelectDefinition;
import com.balsam.oasis.common.registry.select.SelectExecution;
import com.balsam.oasis.common.registry.select.SelectExecutor;
import com.balsam.oasis.common.registry.select.SelectRegistry;

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
 * Completely separate from Query API to maintain clean separation.
 */
@RestController
@RequestMapping("/api/select")
@Tag(name = "Select API", description = "List of Values endpoints for dropdowns and select components")
public class SelectController {

    private static final Logger log = LoggerFactory.getLogger(SelectController.class);

    private final SelectExecutor selectExecutor;
    private final SelectRegistry selectRegistry;

    public SelectController(SelectExecutor selectExecutor,
            SelectRegistry selectRegistry) {
        this.selectExecutor = selectExecutor;
        this.selectRegistry = selectRegistry;
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
            // Get the select definition
            SelectDefinition selectDefinition = selectRegistry.get(selectName);

            if (selectDefinition == null) {
                log.error("Select not found: {}", selectName);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse(new QueryException(
                                "Select not found: " + selectName, "NOT_FOUND", selectName)));
            }

            // Create execution
            SelectExecution execution = selectExecutor.select(selectDefinition);

            // Handle ID fetching (for default values)
            if (id != null && !id.isEmpty()) {
                log.debug("Fetching by IDs: {}", id);
                execution.withIds(id);
            }
            // Handle search
            else if (search != null && !search.isEmpty()) {
                log.debug("Searching with term: {}", search);
                execution.withSearch(search);
            }

            // Parse other parameters (excluding special ones)
            Map<String, Object> params = parseParameters(allParams, selectDefinition);
            if (!params.isEmpty()) {
                execution.withParams(params);
            }

            // Apply pagination
            execution.withPagination(_start, _end);

            // Execute and return
            SelectResponse response = execution.execute();
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
            SelectDefinition definition) {
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