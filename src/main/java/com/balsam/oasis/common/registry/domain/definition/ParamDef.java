package com.balsam.oasis.common.registry.domain.definition;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.processor.ParamProcessor;

/**
 * Immutable parameter definition for query parameters.
 */
public record ParamDef<T>(
        String name,
        Class<T> type,
        T defaultValue,
        boolean required,
        ParamProcessor<T> processor) {

    public static <T> Builder<T> name(String name) {
        return new Builder<T>().name(name);
    }

    public static <T> Builder<T> name(String name, Class<T> type) {
        return new Builder<T>().name(name).type(type);
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
    public boolean isValid(T value, QueryContext context) {
        if (value == null) {
            return !required;
        }

        if (hasProcessor()) {
            try {
                T processed = processor.process(value, context);
                return processed != null;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Manual builder implementation for generic record
     */
    public static class Builder<T> {
        private String name;
        private Class<T> type;
        private T defaultValue;
        private boolean required;
        private ParamProcessor<T> processor;

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> type(Class<T> type) {
            this.type = type;
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder<T> processor(ParamProcessor<T> processor) {
            this.processor = processor;
            return this;
        }

        public ParamDef<T> build() {
            return new ParamDef<>(name, type, defaultValue, required, processor);
        }
    }
}
