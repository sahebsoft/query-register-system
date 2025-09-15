package com.balsam.oasis.common.registry.web.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;
import com.balsam.oasis.common.registry.web.parser.QueryRequestParser;
import com.balsam.oasis.common.registry.service.PlsqlService;
import com.balsam.oasis.common.registry.service.QueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2")
@Tag(name = "Query API", description = "Query Registration System API")
public class QueryController extends QueryBaseController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final QueryService queryService;
    private final QueryRequestParser requestParser;
    private final PlsqlService plsqlService;

    @GetMapping("/query/{queryName}")
    @Operation(summary = "Execute a query", description = "Execute a registered query with filters, sorting, and pagination")
    public ResponseEntity<QueryResponse<List<Map<String, Object>>>> executeQuery(
            @PathVariable @Parameter(description = "Name of the registered query") String queryName,
            @RequestParam(name = "_start", defaultValue = "0") @Parameter(description = "Start index for pagination") Integer start,
            @RequestParam(name = "_end", defaultValue = "50") @Parameter(description = "End index for pagination") Integer end,
            @RequestParam(name = "_meta", defaultValue = "full") @Parameter(description = "Metadata level: full, minimal, none") String meta,
            @RequestParam MultiValueMap<String, String> allParams) {

        log.info("Executing query: {} with params: {}", queryName, allParams);

        return executeQueryList(() -> {
            QueryDefinitionBuilder queryDefinition = queryService.getQueryDefinition(queryName);
            QueryContext queryContext = requestParser.parseForQuery(allParams, start, end, meta, queryDefinition);
            return queryService.executeQuery(queryContext);
        });
    }

    @GetMapping("/query/{queryName}/find-by-key")
    @Operation(summary = "Find by key", description = "Find a single record using key criteria")
    public ResponseEntity<QueryResponse<Map<String, Object>>> findByKey(
            @PathVariable @Parameter(description = "Name of the registered query") String queryName,
            @RequestParam(name = "_meta", defaultValue = "false") @Parameter(description = "Include metadata") boolean meta,
            @RequestParam Map<String, Object> params) {

        log.info("Finding by key for query: {} with params: {}", queryName, params);

        return execute(() -> {

            QueryRow result = queryService.executeSingle(queryName, params);
            if (result == null) {
                throw new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND, "No data found");
            }
            return result.toMap();
        });
    }

    @GetMapping("/select/{selectName}")
    @Operation(summary = "Get list of values", description = "Execute a select query for dropdowns/selects")
    public ResponseEntity<QueryResponse<List<Map<String, Object>>>> getListOfValues(
            @PathVariable @Parameter(description = "Name of the registered select") String selectName,
            @RequestParam(required = false) @Parameter(description = "IDs to fetch (for default values)") List<String> _id,
            @RequestParam(required = false) @Parameter(description = "Search term to filter results") String _search,
            @RequestParam(required = false) @Parameter(description = "Start index for pagination") Integer _start,
            @RequestParam(required = false) @Parameter(description = "End index for pagination") Integer _end,
            @RequestParam MultiValueMap<String, String> allParams) {

        log.info("Executing select: {} with ids: {}, search: {}, pagination: {}-{}",
                selectName, _id, _search, _start, _end);

        return executeQueryList(() -> {
            QueryContext queryContext = requestParser.parseForSelect(allParams, _id, _search, _start, _end,
                    queryService.getQueryDefinition(selectName));
            return queryService.executeQuery(queryContext);
        });
    }

    @GetMapping("/query/{queryName}/metadata")
    @Operation(summary = "Get query metadata", description = "Get metadata for a registered query without executing it")
    public ResponseEntity<QueryResponse<Map<String, Object>>> getQueryMetadata(@PathVariable String queryName) {
        return execute(() -> Map.of(
                "queryName", queryName,
                "message", "Metadata endpoint - to be implemented"));
    }

    @PostMapping("/execute/{name}")
    @Operation(summary = "Execute PL/SQL block", description = "Execute a registered PL/SQL block with parameters")
    public ResponseEntity<QueryResponse<Map<String, Object>>> execute(
            @PathVariable @Parameter(description = "Name of the registered PL/SQL block") String name,
            @RequestBody(required = false) Map<String, Object> params) {

        log.info("Executing PL/SQL: {} with params: {}", name, params);

        // Handle null params gracefully
        Map<String, Object> finalParams = params != null ? params : Map.of();
        return execute(() -> plsqlService.executePlsql(name, finalParams));
    }
}