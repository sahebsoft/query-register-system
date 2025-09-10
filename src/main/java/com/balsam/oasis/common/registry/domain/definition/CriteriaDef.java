package com.balsam.oasis.common.registry.domain.definition;

import java.util.function.Predicate;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import lombok.Builder;

/**
 * Immutable criteria definition for dynamic SQL conditions.
 */
@Builder(builderClassName = "Builder", buildMethodName = "build")
public record CriteriaDef(
        String name,
        String sql,
        Predicate<QueryContext> condition) {
    public static Builder name(String name) {
        return new Builder().name(name);
    }

    public boolean hasCondition() {
        return condition != null;
    }
}
