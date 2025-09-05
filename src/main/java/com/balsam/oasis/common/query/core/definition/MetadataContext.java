package com.balsam.oasis.common.query.core.definition;

/**
 * Defines the context in which metadata will be used.
 * Different contexts require different metadata structures.
 */
public enum MetadataContext {
    
    /**
     * Table/Grid display context.
     * Uses headerText, width, alignment for column display.
     */
    TABLE,
    
    /**
     * Form display context.
     * Uses label, placeholder, helpText, validation for form fields.
     */
    FORM,
    
    /**
     * List of Values context.
     * Minimal metadata for dropdown/select components.
     */
    LOV,
    
    /**
     * Detail view context.
     * Uses label and full descriptions for read-only display.
     */
    DETAIL,
    
    /**
     * Export context.
     * Uses headerText and formatting for data export.
     */
    EXPORT;
    
    /**
     * Get default context based on common usage patterns
     */
    public static MetadataContext getDefault() {
        return TABLE;
    }
    
    /**
     * Check if this context requires full metadata
     */
    public boolean requiresFullMetadata() {
        return this == TABLE || this == FORM || this == DETAIL;
    }
    
    /**
     * Check if this context is for single record display
     */
    public boolean isSingleRecord() {
        return this == FORM || this == DETAIL;
    }
    
    /**
     * Check if this context is for multiple records display
     */
    public boolean isMultipleRecords() {
        return this == TABLE || this == LOV || this == EXPORT;
    }
}