package com.balsam.oasis.common.registry.domain.definition;

import java.util.function.Function;
import java.util.function.Predicate;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.processor.ParamProcessor;
import com.google.common.base.Preconditions;

import lombok.Value;

/**
 * Immutable parameter definition for query parameters with strong typing Uses
 * staged builder pattern to enforce type specification at compile time
 *
 * @param <T> the type of the parameter value
 */
@Value
public class ParamDef<T> {

    String name;
    Class<T> type; // Java type for automatic conversion
    Class<?> genericType; // For collections/arrays
    T defaultValue;
    boolean required;

    // Single processor handles: validation, conversion, transformation
    ParamProcessor<T> processor;

    String description;

    // Private constructor - only accessible through builder
    private ParamDef(BuilderStage<T> builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.genericType = builder.genericType;
        this.defaultValue = builder.defaultValue;
        this.required = builder.required;
        this.processor = builder.processor;
        this.description = builder.description;
    }

    /**
     * Static factory method to start building a parameter Returns TypeStage to
     * force type specification
     */
    public static TypeStage name(String name) {
        return new TypeStage(name);
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

        // If processor exists, it handles validation
        if (hasProcessor()) {
            try {
                processor.process(value, context);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Stage 1: TypeStage - ONLY allows setting type This ensures type must be
     * specified immediately after param()
     */
    public static class TypeStage {

        private final String name;

        private TypeStage(String name) {
            Preconditions.checkNotNull(name, "Parameter name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "Parameter name cannot be empty");
            this.name = name;
        }

        /**
         * Specify the type of this parameter This is the ONLY method available
         * at this stage
         *
         * @param type the Java class representing the parameter type
         * @return the builder stage with all configuration methods
         */
        public <T> BuilderStage<T> type(Class<T> type) {
            Preconditions.checkNotNull(type, "Type cannot be null");
            return new BuilderStage<>(name, type);
        }
    }

    /**
     * Stage 2: BuilderStage - all configuration methods available after type is
     * set This stage is generic and knows the parameter type
     */
    public static class BuilderStage<T> {

        private final String name;
        private final Class<T> type;
        private Class<?> genericType;
        private T defaultValue;
        private boolean required = false;
        private ParamProcessor<T> processor;
        private String description;

        private BuilderStage(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public BuilderStage<T> genericType(Class<?> genericType) {
            this.genericType = genericType;
            return this;
        }

        public BuilderStage<T> defaultValue(T value) {
            this.defaultValue = value;
            return this;
        }

        public BuilderStage<T> required(boolean required) {
            this.required = required;
            return this;
        }

        public BuilderStage<T> processor(ParamProcessor<T> processor) {
            this.processor = processor;
            return this;
        }

        // Convenience method for simple processing with flexible input type
        public BuilderStage<T> processor(Function<Object, T> func) {
            this.processor = ParamProcessor.simple(func);
            return this;
        }

        // Legacy convenience method for same-type processing
        public BuilderStage<T> sameTypeProcessor(Function<T, T> func) {
            this.processor = ParamProcessor.sameType(func);
            return this;
        }

        // Convenience method for validation-only processor with flexible input type
        public BuilderStage<T> validator(Predicate<Object> validator) {
            return validator(validator, "Validation failed for parameter: " + name);
        }

        public BuilderStage<T> validator(Predicate<Object> validator, String errorMessage) {
            this.processor = ParamProcessor.validator(validator, errorMessage);
            return this;
        }

        // Legacy convenience method for typed validation
        public BuilderStage<T> typedValidator(Predicate<T> validator) {
            return typedValidator(validator, "Validation failed for parameter: " + name);
        }

        public BuilderStage<T> typedValidator(Predicate<T> validator, String errorMessage) {
            this.processor = ParamProcessor.typedValidator(validator, errorMessage);
            return this;
        }

        public BuilderStage<T> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Build the immutable ParamDef instance
         */
        public ParamDef<T> build() {
            validate();
            return new ParamDef<>(this);
        }

        private void validate() {
            if (required && defaultValue != null) {
                // This is allowed - required params can have defaults
            }
        }
    }
}
