package com.balsam.oasis.common.registry.domain.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttributeMetadata {
    String name;
    String type;
    Boolean filterable;
    Boolean sortable;
    Boolean restricted;

    // UI metadata fields for frontend display
    String label; // Display label for the attribute
    String labelKey; // i18n key for the label
    String width; // Display width (e.g., "100px", "20%")
    String flex; // Flex value for flexible layouts (e.g., "1", "2")

    // Table context metadata
    String headerText;
    String alignment;
    String headerStyle;
    Boolean visible;

    // Form context metadata
    String placeholder;
    String helpText;
    String inputType;
    Boolean required;
    Integer maxLength;
    Integer minLength;
    String pattern;
    String validationMsg;
}