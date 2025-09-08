package com.balsam.oasis.common.registry.domain.definition;

import java.util.function.Predicate;

import lombok.Builder;
import lombok.Value;

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
    boolean critical = true; // If false, just warning

    public boolean isCritical() {
        return critical;
    }
}