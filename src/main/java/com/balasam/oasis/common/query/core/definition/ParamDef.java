package com.balasam.oasis.common.query.core.definition;

import lombok.Builder;
import lombok.Value;
import com.balasam.oasis.common.query.processor.ParamProcessor;
import com.balasam.oasis.common.query.core.execution.QueryContext;

/**
 * Immutable parameter definition for query parameters
 * Simplified - all validation and processing handled by processor
 */
@Value
@Builder(toBuilder = true)
public class ParamDef {
    String name;
    Class<?> type;                    // Java type for automatic conversion
    Class<?> genericType;              // For collections/arrays
    Object defaultValue;
    
    @Builder.Default
    boolean required = false;
    
    // Single processor handles: validation, conversion, transformation
    ParamProcessor processor;
    
    String description;
    
    public boolean hasProcessor() {
        return processor != null;
    }
    
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }
    
    public boolean isValid(Object value, QueryContext context) {
        if (value == null) {
            return !required;
        }
        
        // If processor exists, it handles validation
        if (hasProcessor()) {
            try {
                processor.process(value, context);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        return true;
    }
}