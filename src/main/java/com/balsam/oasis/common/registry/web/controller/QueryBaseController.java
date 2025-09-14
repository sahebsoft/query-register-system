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

    protected <T> ResponseEntity<QueryResponse<T>> handleRequest(Supplier<T> callable) {
        return handleRequest(callable, (result, executionTime) ->
            ResponseEntity.ok(QueryResponse.single(result, null, executionTime, null)));
    }

    protected <T> ResponseEntity<QueryResponse<T>> handleRequest(Supplier<T> callable,
            java.util.function.BiFunction<T, Long, ResponseEntity<QueryResponse<T>>> responseBuilder) {
        try {
            long startTime = System.currentTimeMillis();
            T result = callable.get();
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

    // New methods that work with QueryData
    protected <T> ResponseEntity<QueryResponse<T>> handleQueryData(
            Supplier<QueryData> queryDataSupplier,
            Function<QueryData, T> dataExtractor) {
        try {
            long startTime = System.currentTimeMillis();
            QueryData queryData = queryDataSupplier.get();
            T data = dataExtractor.apply(queryData);
            long executionTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(QueryResponse.single(data, null, executionTime, queryData.getMetadata()));
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

    protected ResponseEntity<QueryResponse<List<Map<String, Object>>>> handleQueryDataAsList(
            Supplier<QueryData> queryDataSupplier) {
        try {
            long startTime = System.currentTimeMillis();
            QueryData queryData = queryDataSupplier.get();
            List<Map<String, Object>> data = queryData.getData();
            long executionTime = System.currentTimeMillis() - startTime;
            Long count = data != null ? (long) data.size() : 0L;

            return ResponseEntity.ok(QueryResponse.list(data, count, executionTime, queryData.getMetadata()));
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

    protected ResponseEntity<QueryResponse<Map<String, Object>>> handleQueryDataAsSingle(
            Supplier<QueryData> queryDataSupplier) {
        try {
            long startTime = System.currentTimeMillis();
            QueryData queryData = queryDataSupplier.get();
            if (queryData.getRows().isEmpty()) {
                throw new QueryException("No data found", QueryException.ErrorCode.QUERY_NOT_FOUND, "No data found");
            }
            Map<String, Object> data = queryData.getRows().get(0).toMap();
            long executionTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(QueryResponse.single(data, null, executionTime, queryData.getMetadata()));
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

    // Legacy methods for non-QueryData responses (like PlsqlController)
    protected <T> ResponseEntity<QueryResponse<List<T>>> handleListRequest(Supplier<List<T>> callable) {
        return handleRequest(callable, (result, executionTime) -> {
            Long count = result != null ? (long) result.size() : 0L;
            return ResponseEntity.ok(QueryResponse.list(result, count, executionTime, null));
        });
    }

    protected <T> ResponseEntity<QueryResponse<T>> handleSingleRequest(Supplier<T> callable) {
        return handleRequest(callable, (result, executionTime) ->
            ResponseEntity.ok(QueryResponse.single(result, null, executionTime, null)));
    }

    protected ResponseEntity<QueryResponse<String>> handleRequest(Runnable callable) {
        return handleRequest(() -> {
            callable.run();
            return "OK";
        });
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