package com.balsam.oasis.common.registry.domain.definition;

import lombok.Builder;

/**
 * Immutable parameter definition for query parameters.
 */
@Builder
public record ParamDef<T>(
        String name,
        Class<T> type,
        T defaultValue,
        boolean required) {

    public static <T> ParamDefBuilder<T> name(String name) {
        return ParamDef.<T>builder().name(name);
    }

    public static <T> ParamDefBuilder<T> name(String name, Class<T> type) {
        return ParamDef.<T>builder().name(name).type(type);
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public boolean isValid(T value) {
        if (value == null) {
            return !required;
        }
        return true;
    }
}