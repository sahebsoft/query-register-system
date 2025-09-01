package com.balasam.oasis.common.query.rest;

import com.balasam.oasis.common.query.core.execution.QueryExecutor;
import com.balasam.oasis.common.query.core.result.QueryResult;
import com.balasam.oasis.common.query.exception.QueryException;
import com.balasam.oasis.common.query.exception.QueryValidationException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for query execution
 */
@RestController
@RequestMapping("/api/query")
@Tag(name = "Query API", description = "Query Registration System API")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final QueryExecutor queryExecutor;
    private final QueryRequestParser requestParser;
    private final QueryResponseBuilder responseBuilder;

    public QueryController(QueryExecutor queryExecutor,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        this.queryExecutor = queryExecutor;
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

            @RequestParam(defaultValue = "json") @Parameter(description = "Response format: json, csv, excel") String _format,

            @RequestParam MultiValueMap<String, String> allParams) {

        log.info("Executing query: {} with params: {}", queryName, allParams);

        try {
            // Parse request parameters
            QueryRequest queryRequest = requestParser.parse(allParams, _start, _end, _meta);

            // Check if this is a findByKey query (has key.* parameters)
            boolean hasKeyParams = allParams.keySet().stream()
                    .anyMatch(key -> key.startsWith("key."));

            if (hasKeyParams) {
                // Execute as single object query
                Object singleResult = queryExecutor.execute(queryName)
                        .withParams(queryRequest.getParams())
                        .withFilters(queryRequest.getFilters())
                        .includeMetadata(!"none".equals(_meta))
                        .executeSingle();

                // Build single object response
                return responseBuilder.buildSingle(singleResult, _format, queryName);
            } else {
                // Execute as list query
                QueryResult result = queryExecutor.execute(queryName)
                        .withParams(queryRequest.getParams())
                        .withFilters(queryRequest.getFilters())
                        .withSort(queryRequest.getSorts())
                        .withPagination(_start, _end)
                        .includeMetadata(!"none".equals(_meta))
                        .execute();

                // Build response based on format
                return responseBuilder.build(result, _format, queryName);
            }

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
            // Execute query
            QueryResult result = queryExecutor.execute(queryName)
                    .withParams(body.getParams())
                    .withFilters(body.getFilters())
                    .withSort(body.getSorts())
                    .withPagination(body.getStart(), body.getEnd())
                    .includeMetadata(body.isIncludeMetadata())
                    .execute();

            // Build response
            return responseBuilder.build(result, body.getFormat(), queryName);

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

    @GetMapping("/{queryName}/export/{format}")
    @Operation(summary = "Export query results", description = "Export query results in specified format")
    public ResponseEntity<?> exportQuery(
            @PathVariable String queryName,
            @PathVariable String format,
            @RequestParam MultiValueMap<String, String> allParams) {

        try {
            // Parse request
            QueryRequest queryRequest = requestParser.parse(allParams, 0, Integer.MAX_VALUE, "none");

            // Execute query without pagination for export
            QueryResult result = queryExecutor.execute(queryName)
                    .withParams(queryRequest.getParams())
                    .withFilters(queryRequest.getFilters())
                    .withSort(queryRequest.getSorts())
                    .includeMetadata(false)
                    .execute();

            // Build export response
            return responseBuilder.buildExport(result, format, queryName);

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

        switch (errorCode) {
            case "NOT_FOUND":
                return HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR":
            case "DEFINITION_ERROR":
                return HttpStatus.BAD_REQUEST;
            case "SECURITY_ERROR":
                return HttpStatus.FORBIDDEN;
            case "TIMEOUT_ERROR":
                return HttpStatus.REQUEST_TIMEOUT;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private ErrorResponse buildErrorResponse(Exception e) {
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .message(e.getMessage())
                .timestamp(System.currentTimeMillis());

        if (e instanceof QueryException) {
            QueryException qe = (QueryException) e;
            builder.code(qe.getErrorCode())
                    .queryName(qe.getQueryName());

            if (qe instanceof QueryValidationException) {
                builder.details(Map.of("violations",
                        ((QueryValidationException) qe).getViolations()));
            }
        } else {
            builder.code("INTERNAL_ERROR");
        }

        return builder.build();
    }
}