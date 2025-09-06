package com.balsam.oasis.common.registry.rest;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response structure for List of Values (LOV) queries.
 * Contains data array and optional metadata for pagination, applied criteria, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelectResponse {

    /**
     * List of value/label pairs with optional additions
     */
    private List<SelectItem> data;

    /**
     * Optional metadata including pagination, applied criteria, execution time
     */
    private Map<String, Object> metadata;

    /**
     * Create a LOV response with the given items
     */
    public static SelectResponse of(List<SelectItem> items) {
        return SelectResponse.builder()
                .data(items)
                .build();
    }

    /**
     * Create a LOV response with items and metadata
     */
    public static SelectResponse of(List<SelectItem> items, Map<String, Object> metadata) {
        return SelectResponse.builder()
                .data(items)
                .metadata(metadata)
                .build();
    }
}