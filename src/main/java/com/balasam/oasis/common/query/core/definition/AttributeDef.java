package com.balasam.oasis.common.query.core.definition;

import lombok.Builder;
import lombok.Value;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import java.util.Set;
import java.util.List;
import java.util.function.Function;

/**
 * Immutable attribute definition for query fields
 */
@Value
@Builder(toBuilder = true)
public class AttributeDef {
    String name;                    // Frontend/API name
    String dbColumn;                 // Database column name
    Class<?> type;                   // Java type
    
    @Builder.Default
    boolean filterable = false;
    
    @Builder.Default
    boolean sortable = false;
    
    @Builder.Default
    boolean calculated = false;      // Calculated from other fields
    
    @Builder.Default
    boolean virtual = false;         // Not from database
    
    @Builder.Default
    boolean primaryKey = false;
    
    @Builder.Default
    boolean required = false;
    
    @Builder.Default
    boolean masked = false;          // Should be masked in responses
    
    @Builder.Default
    Set<FilterOp> allowedOperators = ImmutableSet.of(FilterOp.EQUALS);
    
    @Builder.Default
    List<String> allowedValues = ImmutableList.of();
    
    Object defaultFilterValue;
    Object defaultValue;
    
    Function<Object, Boolean> validator;
    Function<Object, Object> converter;
    Function<Object, Object> processor;
    Function<Object, String> formatter;
    Function<Object, Object> calculator;
    Function<Object, Boolean> securityRule;
    
    @Builder.Default
    Set<String> dependencies = ImmutableSet.of();  // For virtual/calculated attributes
    
    String description;
    
    public boolean isSecured() {
        return securityRule != null;
    }
    
    public boolean hasValidator() {
        return validator != null;
    }
    
    public boolean hasConverter() {
        return converter != null;
    }
    
    public boolean hasProcessor() {
        return processor != null;
    }
    
    public boolean hasFormatter() {
        return formatter != null;
    }
    
    public boolean hasCalculator() {
        return calculator != null;
    }
    
    public boolean hasAllowedValues() {
        return allowedValues != null && !allowedValues.isEmpty();
    }
    
    public boolean supportsOperator(FilterOp op) {
        return allowedOperators != null && allowedOperators.contains(op);
    }
}