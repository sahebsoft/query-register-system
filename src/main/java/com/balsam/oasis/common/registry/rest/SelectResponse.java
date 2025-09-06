package com.balsam.oasis.common.registry.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified response structure for List of Values (LOV) queries.
 * Contains only the data array with no metadata or grouping.
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
     * Create a LOV response with the given items
     */
    public static SelectResponse of(List<SelectItem> items) {
        return SelectResponse.builder()
                .data(items)
                .build();
    }
}