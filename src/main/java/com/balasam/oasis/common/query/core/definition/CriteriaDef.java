package com.balasam.oasis.common.query.core.definition;

import lombok.Builder;
import lombok.Value;
import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Immutable criteria definition for dynamic SQL conditions
 */
@Value
@Builder(toBuilder = true)
public class CriteriaDef {
    String name;
    String sql;                                  // SQL fragment with named parameters
    Predicate<Object> condition;                // When to apply this criteria
    
    @Builder.Default
    Set<String> bindParams = ImmutableSet.of(); // Required bind parameters
    
    Function<Object, Object> processor;         // Process context before applying
    
    @Builder.Default
    boolean dynamic = false;                    // Generated at runtime
    
    Function<Object, String> generator;         // Generate SQL dynamically
    
    @Builder.Default
    boolean securityRelated = false;            // Is this a security filter
    
    String description;
    String appliedReason;                       // Why this criteria was applied
    
    @Builder.Default
    int priority = 0;                           // Order of application (lower = earlier)
    
    @Builder.Default
    boolean isFindByKey = false;                // Is this a findByKey criteria (returns single object)
    
    public boolean hasProcessor() {
        return processor != null;
    }
    
    public boolean hasGenerator() {
        return generator != null;
    }
    
    public boolean hasCondition() {
        return condition != null;
    }
    
    public boolean requiresParams() {
        return bindParams != null && !bindParams.isEmpty();
    }
}