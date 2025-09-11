package com.balsam.oasis.common.registry.domain.definition;

import java.util.function.Function;
import com.balsam.oasis.common.registry.domain.processor.AttributeFormatter;
import com.balsam.oasis.common.registry.domain.processor.Calculator;
import lombok.Builder;
import lombok.With;
import lombok.experimental.Accessors;

/**
 * Immutable attribute definition for query result fields.
 * Supports both regular (database) and virtual (calculated) attributes.
 */
@With
@Builder(toBuilder = true)
@Accessors(fluent = true)
public record AttributeDef<T>(
        String name,
        Class<T> type,
        String aliasName,
        boolean filterable,
        boolean sortable,
        boolean primaryKey,
        boolean virtual,
        boolean selected,
        AttributeFormatter<T> formatter,
        Calculator<T> calculator,
        Function<Object, Boolean> securityRule,
        String description,
        // Display metadata
        String label,
        String labelKey,
        String width,
        String flex,
        String alignment,
        String headerStyle,
        boolean visible) {

    /**
     * Compact constructor for validation and defaults
     */
    @SuppressWarnings("unchecked")
    public AttributeDef {
        // Validation
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Attribute name is required");
        }

        // Apply defaults
        type = type != null ? type : (Class<T>) Object.class;
        alignment = alignment != null ? alignment : "left";
        aliasName = (!virtual && aliasName == null) ? name : aliasName;

        // Virtual attribute rules
        if (virtual) {
            if (calculator == null) {
                throw new IllegalStateException("Virtual attribute '" + name + "' requires calculator");
            }
            if (primaryKey) {
                throw new IllegalStateException("Virtual attribute '" + name + "' cannot be primary key");
            }
            aliasName = null;
            filterable = false;
            sortable = false;
        } else if (calculator != null) {
            throw new IllegalStateException("Regular attribute '" + name + "' should not have calculator");
        }
    }

    /**
     * Static factory to start building an attribute
     */
    @SuppressWarnings("unchecked")
    public static <T> AttributeDefBuilder<T> of(String name) {
        return (AttributeDefBuilder<T>) builder()
                .name(name)
                .type(Object.class)
                .aliasName(name)
                .filterable(true)
                .sortable(true)
                .selected(true)
                .visible(true)
                .alignment("left");
    }

    public static <T> AttributeDefBuilder<T> of(String name, Class<T> type) {
        return AttributeDef.<T>builder()
                .name(name).type(type).aliasName(name)
                .filterable(true).sortable(true)
                .selected(true).visible(true);
    }

    // Compatibility methods
    public static <T> AttributeDefBuilder<T> name(String name) {
        return of(name);
    }

    public static <T> AttributeDefBuilder<T> name(String name, Class<T> type) {
        return of(name, type);
    }

    // Helper methods
    public boolean isSecured() {
        return securityRule != null;
    }

    public boolean hasFormatter() {
        return formatter != null;
    }

    public boolean hasCalculator() {
        return calculator != null;
    }

    public boolean isVirtual() {
        return virtual;
    }

    /**
     * Custom builder extensions
     */
    public static class AttributeDefBuilder<T> {
        public AttributeDefBuilder<T> calculated(Calculator<T> calc) {
            return calculator(calc).virtual(true).filterable(false).sortable(false).aliasName(null);
        }

        public AttributeDefBuilder<T> display(String label, String width) {
            return label(label).width(width);
        }

        public AttributeDefBuilder<T> secure(Function<Object, Boolean> rule) {
            return securityRule(rule);
        }
    }
}