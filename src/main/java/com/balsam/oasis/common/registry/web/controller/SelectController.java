package com.balsam.oasis.common.registry.web.controller;

import java.util.List;

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

import com.balsam.oasis.common.registry.api.QueryExecutor;
import com.balsam.oasis.common.registry.api.QueryRegistry;
import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.definition.FilterOp;
import com.balsam.oasis.common.registry.domain.execution.QueryExecution;
import com.balsam.oasis.common.registry.exception.QueryException;
import com.balsam.oasis.common.registry.web.builder.QueryResponseBuilder;
import com.balsam.oasis.common.registry.web.dto.request.QueryRequest;
import com.balsam.oasis.common.registry.web.dto.response.ErrorResponse;
import com.balsam.oasis.common.registry.web.parser.QueryRequestParser;

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
    private final QueryResponseBuilder responseBuilder;
    private final QueryRequestParser requestParser;

    public SelectController(QueryExecutor queryExecutor,
            QueryRegistry queryRegistry,
            QueryResponseBuilder responseBuilder,
            QueryRequestParser requestParser) {
        this.queryExecutor = queryExecutor;
        this.queryRegistry = queryRegistry;
        this.responseBuilder = responseBuilder;
        this.requestParser = requestParser;
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

            // Parse request parameters with type information (including special select
            // params)
            QueryRequest queryRequest = requestParser.parse(allParams, _start, _end, "none", queryDefinition);

            // Create execution using QueryExecution
            QueryExecution execution = queryExecutor.execute(queryDefinition);

            // Handle ID fetching (for default values) - convert to IN filter on value
            // attribute
            if (id != null && !id.isEmpty()) {
                log.debug("Fetching by IDs: {}", id);
                execution.withFilter("value", FilterOp.IN, id);
            }
            // Handle search
            else if (search != null && !search.isEmpty()) {
                log.debug("Searching with term: {}", search);

                // Check if query has search parameter or search criteria
                boolean hasSearchParam = queryDefinition.getParameters().containsKey("search");
                boolean hasSearchCriteria = queryDefinition.getCriteria().containsKey("search") ||
                        queryDefinition.getCriteria().containsKey("searchFilter");

                if (hasSearchParam || hasSearchCriteria) {
                    // Use search parameter if query supports it
                    execution.withParam("search", "%" + search + "%");
                } else {
                    // Fallback to filtering on label column with case insensitive LIKE
                    // (case insensitivity is now handled by QuerySqlBuilder using UPPER functions)
                    execution.withFilter("label", FilterOp.LIKE, "%" + search + "%");
                }
            }

            // Apply parsed parameters, filters, and sorting
            execution.withParams(queryRequest.getParams())
                    .withFilters(queryRequest.getFilters())
                    .withSort(queryRequest.getSorts())
                    .withPagination(_start, _end)
                    .includeMetadata(true);

            // Execute and build select response
            QueryResult queryResult = execution.execute();
            return responseBuilder.buildSelectResponse(queryResult);

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