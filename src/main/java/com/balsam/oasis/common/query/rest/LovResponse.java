package com.balsam.oasis.common.query.rest;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Simplified response structure for List of Values (LOV) queries.
 * Contains only the data array with no metadata or grouping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LovResponse {
    
    /**
     * List of value/label pairs with optional additions
     */
    private List<LovItem> data;
    
    /**
     * Create a LOV response with the given items
     */
    public static LovResponse of(List<LovItem> items) {
        return LovResponse.builder()
                .data(items)
                .build();
    }
}