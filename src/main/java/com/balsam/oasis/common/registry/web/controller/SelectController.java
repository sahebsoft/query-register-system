package com.balsam.oasis.common.registry.web.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balsam.oasis.common.registry.domain.common.Pagination;
import com.balsam.oasis.common.registry.web.dto.request.QueryRequest;
import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;
import com.balsam.oasis.common.registry.web.parser.QueryRequestParser;
import com.balsam.oasis.common.registry.service.QueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for List of Values (LOV) / Select endpoints.
 * Provides simplified data for dropdown/select UI components.
 * Now consolidated to use the unified Query infrastructure.
 */
@RestController
@RequestMapping("/api/select/v2")
@Tag(name = "Select API", description = "List of Values endpoints for dropdowns and select components")
public class SelectController extends QueryBaseController {

    private static final Logger log = LoggerFactory.getLogger(SelectController.class);

    private final QueryService queryService;
    private final QueryRequestParser requestParser;

    public SelectController(QueryService queryService, QueryRequestParser requestParser) {
        this.queryService = queryService;
        this.requestParser = requestParser;
    }

    @GetMapping("/{selectName}")
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
            QueryRequest queryRequest = requestParser.parse(allParams, _start, _end, "none",
                    queryService.getQueryDefinition(selectName));
            return queryService.executeAsSelect(selectName, _search, _id, _start, _end, queryRequest.getParams());
        });
    }
}