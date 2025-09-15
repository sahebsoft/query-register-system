package com.balsam.oasis.common.registry.web.dto.response;

import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse<T> {
    private T data;
    private Long count;
    private Long executionTime;
    private QueryMetadata metadata;
    @Builder.Default
    private boolean success = true;
    private String errorCode;
    private String message;
    private long timestamp;

    public static <E> QueryResponse<List<E>> list(List<E> data, Long count, Long executionTime,
            QueryMetadata metadata) {
        return QueryResponse.<List<E>>builder()
                .data(data)
                .count(count)
                .executionTime(executionTime)
                .metadata(metadata)
                .success(true)
                .build();
    }

    // For single object responses
    public static <T> QueryResponse<T> single(T data, Long count, Long executionTime, QueryMetadata metadata) {
        return QueryResponse.<T>builder()
                .data(data)
                .count(count)
                .executionTime(executionTime)
                .metadata(metadata)
                .success(true)
                .build();
    }

    public static <T> QueryResponse<T> error(String errorCode, String message) {
        return QueryResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}