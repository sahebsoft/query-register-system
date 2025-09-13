package com.balsam.oasis.common.registry.domain.select;

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
     * Create a simple LOV item
     */
    public static SelectItem of(String value, String label) {
        return SelectItem.builder()
                .value(value)
                .label(label)
                .build();
    }
}