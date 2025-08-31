package com.balasam.oasis.common.query.core.definition;

import lombok.Builder;
import lombok.Value;

import java.util.function.Predicate;

/**
 * Validation rule for query execution
 */
@Value
@Builder(toBuilder = true)
public class ValidationRule {
    String name;
    Predicate<Object> rule;
    String errorMessage;
    
    @Builder.Default
    boolean critical = true;  // If false, just warning
    
    public boolean isCritical() {
        return critical;
    }
}