package com.balsam.oasis.common.query.rest;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single item in a List of Values (LOV) response.
 * Used for dropdown/select components in the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LovItem {
    
    /**
     * The value to be submitted (typically an ID)
     */
    private String value;
    
    /**
     * The display label shown to the user
     */
    private String label;
    
    /**
     * Additional data fields (email, phone, etc.)
     */
    private Map<String, Object> additions;
    
    /**
     * Create a simple LOV item without additions
     */
    public static LovItem of(String value, String label) {
        return LovItem.builder()
                .value(value)
                .label(label)
                .build();
    }
    
    /**
     * Create a LOV item with additional data
     */
    public static LovItem of(String value, String label, Map<String, Object> additions) {
        return LovItem.builder()
                .value(value)
                .label(label)
                .additions(additions)
                .build();
    }
}