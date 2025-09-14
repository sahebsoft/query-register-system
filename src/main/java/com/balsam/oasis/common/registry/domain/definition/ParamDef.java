package com.balsam.oasis.common.registry.domain.definition;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.processor.ParamProcessor;
import lombok.Builder;

/**
 * Immutable parameter definition for query parameters.
 */
@Builder
public record ParamDef<T>(
        String name,
        Class<T> type,
        T defaultValue,
        boolean required,
        ParamProcessor<T> processor) {

    public static <T> ParamDefBuilder<T> name(String name) {
        return ParamDef.<T>builder().name(name);
    }

    public static <T> ParamDefBuilder<T> name(String name, Class<T> type) {
        return ParamDef.<T>builder().name(name).type(type);
    }

    public boolean hasProcessor() {
        return processor != null;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public boolean isValid(T value, QueryContext context) {
        if (value == null) {
            return !required;
        }

        if (hasProcessor()) {
            try {
                T processed = processor.process((String) value, context);
                return processed != null;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }
}