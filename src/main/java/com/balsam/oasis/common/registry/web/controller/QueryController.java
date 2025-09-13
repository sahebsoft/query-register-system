package com.balsam.oasis.common.registry.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.web.builder.QueryResponseBuilder;
import com.balsam.oasis.common.registry.web.dto.request.QueryRequest;
import com.balsam.oasis.common.registry.web.dto.request.QueryRequestBody;
import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;
import com.balsam.oasis.common.registry.web.parser.QueryRequestParser;
import com.balsam.oasis.common.registry.service.QueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for query execution via HTTP endpoints.
 * Provides GET and POST endpoints for executing registered queries
 * with support for parameters, filters, sorting, and pagination.
 *
 * <p>
 * The controller handles:
 * </p>
 * <ul>
 * <li>Query parameter parsing from HTTP requests</li>
 * <li>Query execution through the QueryExecutor</li>
 * <li>Response formatting with optional metadata</li>
 * <li>Error handling and validation</li>
 * </ul>
 *
 * <p>
 * GET endpoint format:
 * </p>
 * 
 * <pre>
 * GET /api/query/{queryName}?
 *     minSalary=50000&
 *     filter.department.in=IT,HR&
 *     filter.status=ACTIVE&
 *     sort=salary.desc,name.asc&
 *     _start=0&_end=100&
 *     _meta=full
 * </pre>
 *
 * <p>
 * POST endpoint accepts JSON body with query configuration.
 * </p>
 *
 * @author Query Registration System
 * @since 1.0
 * @see QueryRequestParser
 * @see QueryResponseBuilder
 */
@RestController
@RequestMapping("/api/query/v2")
@Tag(name = "Query API", description = "Query Registration System API")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final QueryService queryService;
    private final QueryRequestParser requestParser;
    private final QueryResponseBuilder responseBuilder;

    public QueryController(QueryService queryService,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        this.queryService = queryService;
        this.requestParser = requestParser;
        this.responseBuilder = responseBuilder;
    }

    @GetMapping("/{queryName}")
    @Operation(summary = "Execute a query", description = "Execute a registered query with filters, sorting, and pagination")
    public ResponseEntity<?> executeQuery(
            @PathVariable @Parameter(description = "Name of the registered query") String queryName,
            @RequestParam(defaultValue = "0") @Parameter(description = "Start index for pagination") int _start,
            @RequestParam(defaultValue = "50") @Parameter(description = "End index for pagination") int _end,
            @RequestParam(defaultValue = "full") @Parameter(description = "Metadata level: full, minimal, none") String _meta,
            @RequestParam MultiValueMap<String, String> allParams) {

        log.info("Executing query: {} with params: {}", queryName, allParams);

        try {
            // Get the query definition for type-aware parsing
            QueryDefinitionBuilder queryDefinition = queryService.getQueryDefinition(queryName);

            if (queryDefinition == null) {
                log.error("Query not found: {}", queryName);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse(
                                new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND, "Query not found: " + queryName)));
            }

            // Parse request parameters with type information
            QueryRequest queryRequest = requestParser.parse(allParams, _start, _end, _meta, queryDefinition);

            // Execute query through service
            QueryResult result = queryService.executeQuery(queryName, queryRequest);

            // Build JSON response
            return responseBuilder.build(result, queryName);

        } catch (QueryException e) {
            log.error("Query execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error executing query: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }

    @PostMapping("/{queryName}")
    @Operation(summary = "Execute a query with body", description = "Execute a query with complex parameters in request body")
    public ResponseEntity<?> executeQueryWithBody(
            @PathVariable String queryName,
            @RequestBody QueryRequestBody body) {

        log.info("Executing query with body: {}", queryName);

        try {
            // Convert body to QueryRequest
            QueryRequest queryRequest = QueryRequest.builder()
                    .params(body.getParams())
                    .filters(body.getFilters())
                    .sorts(body.getSorts())
                    .pagination(body.getStart(), body.getEnd())
                    .build();
            
            // Execute query through service
            QueryResult result = queryService.executeQuery(queryName, queryRequest);

            // Build JSON response
            return responseBuilder.build(result, queryName);

        } catch (QueryException e) {
            log.error("Query execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error executing query: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }

    @GetMapping("/{queryName}/find-by-key")
    @Operation(summary = "Find by key", description = "Find a single record using key criteria")
    public ResponseEntity<?> findByKey(
            @PathVariable @Parameter(description = "Name of the registered query") String queryName,
            @RequestParam(defaultValue = "false") @Parameter(description = "Include metadata") boolean _meta,
            @RequestParam MultiValueMap<String, String> keyParams) {

        log.info("Finding by key for query: {} with params: {}", queryName, keyParams);

        try {
            // Get the query definition
            QueryDefinitionBuilder queryDefinition = queryService.getQueryDefinition(queryName);

            if (queryDefinition == null) {
                log.error("Query not found: {}", queryName);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse(
                                new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND, "Query not found: " + queryName)));
            }

            // Extract key parameters
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : keyParams.entrySet()) {
                if (!entry.getKey().startsWith("_")) { // Skip meta parameters
                    params.put(entry.getKey(), entry.getValue().get(0));
                }
            }

            // Create request with single result pagination
            QueryRequest queryRequest = QueryRequest.builder()
                    .params(params)
                    .pagination(0, 1) // Limit to single result
                    .build();
            
            // Execute through service
            QueryResult result = queryService.executeQuery(queryName, queryRequest);

            // Build single object response
            return responseBuilder.buildSingle(result, queryName);

        } catch (QueryException e) {
            log.error("Find-by-key execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error in find-by-key: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }

    @GetMapping("/{queryName}/metadata")
    @Operation(summary = "Get query metadata", description = "Get metadata for a registered query without executing it")
    public ResponseEntity<?> getQueryMetadata(@PathVariable String queryName) {
        try {
            // This would return query definition metadata
            // Implementation would depend on QueryExecutor providing metadata access
            return ResponseEntity.ok(Map.of(
                    "queryName", queryName,
                    "message", "Metadata endpoint - to be implemented"));
        } catch (Exception e) {
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
            case "VALIDATION_ERROR", "DEFINITION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "SECURITY_ERROR" -> HttpStatus.FORBIDDEN;
            case "TIMEOUT_ERROR" -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private QueryResponse buildErrorResponse(Exception e) {
        String errorCode = "INTERNAL_ERROR";
        String errorMessage = e.getMessage();

        if (e instanceof QueryException qe) {
            errorCode = qe.getErrorCode();
        }

        return QueryResponse.error(errorCode, errorMessage);
    }
}