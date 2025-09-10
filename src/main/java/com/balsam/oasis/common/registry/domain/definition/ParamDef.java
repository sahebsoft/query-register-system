package com.balsam.oasis.common.registry.domain.definition;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.processor.ParamProcessor;

import lombok.Builder;

/**
 * Immutable parameter definition for query parameters.
 */
@Builder(builderClassName = "Builder", buildMethodName = "build")
public record ParamDef(
        String name,
        Class<?> type,
        Object defaultValue,
        boolean required,
        ParamProcessor processor) {

    public static Builder name(String name) {
        return new Builder().name(name);
    }

    public boolean hasProcessor() {
        return processor != null;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Validate a value using processor and required flag.
     * Returns false if value is null and required is true,
     * or if processor returns null or throws an exception.
     */
    public boolean isValid(Object value, QueryContext context) {
        if (value == null) {
            return !required;
        }

        if (hasProcessor()) {
            try {
                Object processed = processor.process(value, context);
                return processed != null;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }
}
