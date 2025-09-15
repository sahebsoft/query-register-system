package com.balsam.oasis.common.registry.web.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.balsam.oasis.common.registry.web.dto.response.QueryResponse;
import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.domain.exception.QueryException;

public abstract class QueryBaseController {

    private static final Logger log = LoggerFactory.getLogger(QueryBaseController.class);

    protected <T> ResponseEntity<QueryResponse<T>> execute(Supplier<T> supplier) {
        return executeWithTimer(supplier, (result, time) ->
            ResponseEntity.ok(QueryResponse.single(result, null, time, null)));
    }

    protected ResponseEntity<QueryResponse<List<Map<String, Object>>>> executeQueryList(Supplier<QueryData> supplier) {
        return executeWithTimer(supplier, (queryData, time) -> {
            List<Map<String, Object>> data = queryData.getData();
            Long count = (long) queryData.getCount();
            return ResponseEntity.ok(QueryResponse.list(data, count, time, queryData.getMetadata()));
        });
    }

    protected ResponseEntity<QueryResponse<Map<String, Object>>> executeQuerySingle(Supplier<QueryData> supplier) {
        return executeWithTimer(supplier, (queryData, time) -> {
            if (queryData.getRows().isEmpty()) {
                throw new QueryException("No data found", QueryException.ErrorCode.QUERY_NOT_FOUND, "No data found");
            }
            Map<String, Object> data = queryData.getRows().get(0).toMap();
            return ResponseEntity.ok(QueryResponse.single(data, null, time, queryData.getMetadata()));
        });
    }

    protected <T> ResponseEntity<QueryResponse<T>> executeQueryCustom(Supplier<QueryData> supplier, Function<QueryData, T> mapper) {
        return executeWithTimer(supplier, (queryData, time) -> {
            T data = mapper.apply(queryData);
            return ResponseEntity.ok(QueryResponse.single(data, null, time, queryData.getMetadata()));
        });
    }

    private <T, R> ResponseEntity<QueryResponse<R>> executeWithTimer(Supplier<T> supplier,
            java.util.function.BiFunction<T, Long, ResponseEntity<QueryResponse<R>>> responseBuilder) {
        long startTime = System.currentTimeMillis();
        try {
            T result = supplier.get();
            long executionTime = System.currentTimeMillis() - startTime;
            return responseBuilder.apply(result, executionTime);
        } catch (QueryException e) {
            log.error("Query execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(QueryResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(QueryResponse.error("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private HttpStatus determineHttpStatus(QueryException e) {
        String errorCode = e.getErrorCode();
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (errorCode) {
            case "NOT_FOUND", "QRY001" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR", "DEFINITION_ERROR", "QRY006", "QRY005", "LOV_NOT_SUPPORTED" -> HttpStatus.BAD_REQUEST;
            case "TIMEOUT_ERROR", "QRY003" -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}