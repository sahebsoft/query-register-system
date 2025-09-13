package com.balsam.oasis.common.registry.web.dto.response;

import com.balsam.oasis.common.registry.domain.metadata.QueryMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse {
    private Object data;
    private Integer count;
    private QueryMetadata metadata;
    @Builder.Default
    private boolean success = true;
    private String errorCode;
    private String errorMessage;
    private long timestamp;

    // Static factory methods for convenience
    public static QueryResponse success(List<?> data, QueryMetadata metadata, Integer count) {
        return QueryResponse.builder()
                .data(data)
                .metadata(metadata)
                .count(count)
                .success(true)
                .build();
    }

    public static QueryResponse success(List<?> data, Integer count) {
        return QueryResponse.builder()
                .data(data)
                .count(count)
                .success(true)
                .build();
    }

    public static QueryResponse success(Object singleData) {
        return QueryResponse.builder()
                .data(singleData)
                .count(1)
                .success(true)
                .build();
    }

    public static QueryResponse error(String errorCode, String errorMessage) {
        return QueryResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}