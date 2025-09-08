package com.balsam.oasis.common.registry.domain.select;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single item in a List of Values (LOV) response.
 * Used for dropdown/select components in the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelectItem {

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
    public static SelectItem of(String value, String label) {
        return SelectItem.builder()
                .value(value)
                .label(label)
                .build();
    }

    /**
     * Create a LOV item with additional data
     */
    public static SelectItem of(String value, String label, Map<String, Object> additions) {
        return SelectItem.builder()
                .value(value)
                .label(label)
                .additions(additions)
                .build();
    }
}