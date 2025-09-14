package com.balsam.oasis.common.registry.web.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.balsam.oasis.common.registry.service.PlsqlService;
import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/plsql/v2")
@Tag(name = "PL/SQL API", description = "PL/SQL Block Execution System API")
public class PlsqlController extends QueryBaseController {

    private static final Logger log = LoggerFactory.getLogger(PlsqlController.class);

    private final PlsqlService plsqlService;

    public PlsqlController(PlsqlService plsqlService) {
        this.plsqlService = plsqlService;
    }

    @PostMapping("/execute/{name}")
    @Operation(summary = "Execute PL/SQL block", description = "Execute a registered PL/SQL block with parameters")
    public ResponseEntity<QueryResponse<Map<String, Object>>> execute(
            @PathVariable @Parameter(description = "Name of the registered PL/SQL block") String name,
            @RequestBody Map<String, Object> params) {

        log.info("Executing PL/SQL: {} with params: {}", name, params);

        return handleSingleRequest(() -> plsqlService.executePlsql(name, params));
    }

    @PostMapping("/execute/{name}/simple")
    @Operation(summary = "Execute PL/SQL with simple params", description = "Execute PL/SQL with parameters as map")
    public ResponseEntity<QueryResponse<Map<String, Object>>> executeSimple(
            @PathVariable @Parameter(description = "Name of the registered PL/SQL block") String name,
            @RequestBody Map<String, Object> params) {

        log.info("Executing PL/SQL (simple): {} with params: {}", name, params);

        return handleSingleRequest(() -> plsqlService.executePlsql(name, params));
    }

}