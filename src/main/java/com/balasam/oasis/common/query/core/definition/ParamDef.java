package com.balasam.oasis.common.query.core.definition;

import lombok.Builder;
import lombok.Value;

import java.util.function.Function;

/**
 * Immutable parameter definition for query parameters
 */
@Value
@Builder(toBuilder = true)
public class ParamDef {
    String name;
    Class<?> type;
    Class<?> genericType;  // For collections/arrays
    Object defaultValue;
    
    @Builder.Default
    boolean required = false;
    
    Function<Object, Boolean> validator;
    Function<Object, Object> processor;
    Function<Object, Object> converter;
    
    String description;
    
    Integer minValue;
    Integer maxValue;
    Integer minLength;
    Integer maxLength;
    String pattern;      // Regex pattern for validation
    
    public boolean hasValidator() {
        return validator != null;
    }
    
    public boolean hasProcessor() {
        return processor != null;
    }
    
    public boolean hasConverter() {
        return converter != null;
    }
    
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }
    
    public boolean hasConstraints() {
        return minValue != null || maxValue != null || 
               minLength != null || maxLength != null || 
               pattern != null;
    }
}