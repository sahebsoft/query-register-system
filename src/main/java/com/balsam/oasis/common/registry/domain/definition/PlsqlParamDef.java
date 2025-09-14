package com.balsam.oasis.common.registry.domain.definition;

import com.balsam.oasis.common.registry.domain.execution.PlsqlContext;
import com.balsam.oasis.common.registry.domain.processor.ParamProcessor;
import lombok.Builder;

@Builder
public record PlsqlParamDef<T>(
        String name,
        Class<T> type,
        T defaultValue,
        boolean required,
        ParamProcessor<T> processor,
        ParamMode mode,
        String plsqlDefault,
        int sqlType) {

    public enum ParamMode {
        IN,      // Input only (default)
        OUT,     // Output only
        INOUT    // Input and output
    }

    public ParamMode mode() {
        return mode != null ? mode : ParamMode.IN;
    }

    public boolean hasProcessor() {
        return processor != null;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public boolean hasPlsqlDefault() {
        return plsqlDefault != null && !plsqlDefault.trim().isEmpty();
    }


    public boolean isValid(T value, PlsqlContext context) {
        if (value == null) {
            return !required;
        }

        if (hasProcessor()) {
            try {
                // Create a temporary QueryContext to maintain compatibility with existing processors
                var queryContext = com.balsam.oasis.common.registry.domain.execution.QueryContext.builder()
                        .params(context.getParams())
                        .build();
                T processed = processor.process((String) value, queryContext);
                return processed != null;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    // Builder methods for PL/SQL parameter modes
    public static <T> PlsqlParamDefBuilder<T> in(String name, Class<T> type) {
        return PlsqlParamDef.<T>builder()
                .name(name)
                .type(type)
                .mode(ParamMode.IN);
    }

    public static <T> PlsqlParamDefBuilder<T> out(String name, Class<T> type) {
        return PlsqlParamDef.<T>builder()
                .name(name)
                .type(type)
                .mode(ParamMode.OUT)
                .required(false);  // OUT params are never required as input
    }

    public static <T> PlsqlParamDefBuilder<T> inout(String name, Class<T> type) {
        return PlsqlParamDef.<T>builder()
                .name(name)
                .type(type)
                .mode(ParamMode.INOUT);
    }
}