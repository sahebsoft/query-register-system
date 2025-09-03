package com.balasam.oasis.common.query.core.definition;

import java.util.function.Function;

import com.balasam.oasis.common.query.processor.AttributeFormatter;
import com.balasam.oasis.common.query.processor.AttributeProcessor;
import com.google.common.base.Preconditions;

import lombok.Value;

/**
 * Immutable attribute definition for query fields with strong typing Uses
 * staged builder pattern to enforce type specification at compile time
 *
 * @param <T> the type of the attribute value
 */
@Value
public class AttributeDef<T> {

    String name; // Frontend/API name
    Class<T> type; // Java type for automatic conversion
    String aliasName; // Database column name
    boolean filterable;
    boolean sortable;
    boolean virtual; // Not from database
    boolean primaryKey;

    // Single processor handles: conversion, formatting, masking, calculation
    AttributeProcessor<T> processor;

    // Formatter for string representation
    AttributeFormatter<T> formatter;

    // Security rule determines if user can see this attribute
    Function<Object, Boolean> securityRule;

    String description;

    // Private constructor - only accessible through builder
    private AttributeDef(BuilderStage<T> builder) {
        this.name = builder.name;
        this.aliasName = builder.aliasName;
        this.type = builder.type;
        this.filterable = builder.filterable;
        this.sortable = builder.sortable;
        this.virtual = builder.virtual;
        this.primaryKey = builder.primaryKey;
        this.processor = builder.processor;
        this.formatter = builder.formatter;
        this.securityRule = builder.securityRule;
        this.description = builder.description;
    }

    /**
     * Static factory method to start building an attribute Returns TypeStage to
     * force type specification
     */
    public static TypeStage name(String name) {
        return new TypeStage(name);
    }

    public boolean isSecured() {
        return securityRule != null;
    }

    public boolean hasProcessor() {
        return processor != null;
    }

    public boolean hasFormatter() {
        return formatter != null;
    }


    /**
     * Stage 1: TypeStage - ONLY allows setting type This ensures type must be
     * specified immediately after attr()
     */
    public static class TypeStage {

        private final String name;

        private TypeStage(String name) {
            Preconditions.checkNotNull(name, "Attribute name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "Attribute name cannot be empty");
            this.name = name;
        }

        /**
         * Specify the type of this attribute This is the ONLY method available
         * at this stage
         *
         * @param type the Java class representing the attribute type
         * @return the builder stage with all configuration methods
         */
        public <T> BuilderStage<T> type(Class<T> type) {
            Preconditions.checkNotNull(type, "Type cannot be null");
            return new BuilderStage<>(name, type);
        }
    }

    /**
     * Stage 2: BuilderStage - all configuration methods available after type is
     * set This stage is generic and knows the attribute type
     */
    public static class BuilderStage<T> {

        private final String name;
        private final Class<T> type;
        private String aliasName;
        private boolean filterable = false;
        private boolean sortable = false;
        private boolean virtual = false;
        private boolean primaryKey = false;
        private AttributeProcessor<T> processor;
        private AttributeFormatter<T> formatter;
        private Function<Object, Boolean> securityRule;
        private String description;

        private BuilderStage(String name, Class<T> type) {
            this.name = name;
            this.type = type;
            this.aliasName = name; // Default to same as name
        }

        // sql column/alias name
        public BuilderStage<T> aliasName(String column) {
            this.aliasName = column;
            return this;
        }

        public BuilderStage<T> filterable(boolean filterable) {
            this.filterable = filterable;
            return this;
        }

        public BuilderStage<T> sortable(boolean sortable) {
            this.sortable = sortable;
            return this;
        }

        public BuilderStage<T> virtual(boolean virtual) {
            this.virtual = virtual;
            if (virtual) {
                this.aliasName = null; // Virtual attributes don't have DB columns
                this.sortable = false; // Can't sort virtual attributes at DB level
            }
            return this;
        }

        public BuilderStage<T> primaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
            return this;
        }

        public BuilderStage<T> processor(AttributeProcessor<T> processor) {
            this.processor = processor;
            return this;
        }

        // Convenience method for simple processing
        public BuilderStage<T> processor(Function<T, Object> func) {
            this.processor = AttributeProcessor.simple(func);
            return this;
        }

        public BuilderStage<T> formatter(AttributeFormatter<T> formatter) {
            this.formatter = formatter;
            return this;
        }

        // Convenience method for simple formatting
        public BuilderStage<T> formatter(Function<T, String> func) {
            this.formatter = func::apply;
            return this;
        }

        public BuilderStage<T> secure(Function<Object, Boolean> rule) {
            this.securityRule = rule;
            return this;
        }

        public BuilderStage<T> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Build the immutable AttributeDef instance
         */
        public AttributeDef<T> build() {
            validate();
            return new AttributeDef<>(this);
        }

        private void validate() {
            if (virtual && aliasName != null) {
                aliasName = null; // Ensure virtual attributes have no DB column
            }
            if (virtual && sortable) {
                sortable = false; // Virtual attributes can't be sorted at DB level
            }
            if (primaryKey && virtual) {
                throw new IllegalStateException("Virtual attributes cannot be primary keys");
            }
        }
    }

    /**
     * Create a non-generic AttributeDef for backward compatibility This is used
     * when type information is not needed at compile time
     */
    public AttributeDef<?> toNonGeneric() {
        return (AttributeDef<?>) this;
    }
}
