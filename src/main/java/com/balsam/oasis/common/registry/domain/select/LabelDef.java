package com.balsam.oasis.common.registry.domain.select;

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.google.common.base.Preconditions;

/**
 * Specialized definition for select label attributes.
 * Only requires aliasName and type - all other properties are predefined
 * internally.
 * 
 * Label attributes are automatically configured with:
 * - name: "label" (fixed)
 * - primaryKey: false (labels are not primary keys)
 */
public class LabelDef {

    private final String aliasName;
    private final Class<?> type;

    private LabelDef(String aliasName, Class<?> type) {
        this.aliasName = aliasName;
        this.type = type;
    }

    /**
     * Create a label definition with database column name and Java type
     * 
     * @param aliasName the database column name
     * @param type      the Java type (typically String.class)
     * @return LabelDef instance
     */
    public static LabelDef of(String aliasName, Class<?> type) {
        Preconditions.checkNotNull(aliasName, "Alias name cannot be null");
        Preconditions.checkArgument(!aliasName.trim().isEmpty(), "Alias name cannot be empty");
        Preconditions.checkNotNull(type, "Type cannot be null");

        return new LabelDef(aliasName, type);
    }

    /**
     * Convenience method for String labels (most common case)
     */
    public static LabelDef of(String aliasName) {
        return of(aliasName, String.class);
    }

    /**
     * Convert to AttributeDef with predefined label-specific settings
     */
    public AttributeDef<?> toAttributeDef() {
        return AttributeDef.name("label", type)
                .aliasName(aliasName)
                .primaryKey(false) // Labels are not primary keys
                .description("Label field for select component")
                .build();
    }

    // Getters for internal use
    public String getAliasName() {
        return aliasName;
    }

    public Class<?> getType() {
        return type;
    }
}