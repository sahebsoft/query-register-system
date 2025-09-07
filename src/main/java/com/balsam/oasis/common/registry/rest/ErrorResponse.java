package com.balsam.oasis.common.registry.rest;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

/**
 * Error response for query API
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Builder.Default
    private boolean success = false;
    private String code;
    private String message;
    private String queryName;
    private Map<String, Object> details;
    private long timestamp;
    @Builder.Default
    private Integer count = 0;
}