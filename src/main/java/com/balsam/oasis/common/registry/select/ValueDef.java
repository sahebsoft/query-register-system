package com.balsam.oasis.common.registry.select;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.google.common.base.Preconditions;

/**
 * Specialized definition for select value attributes.
 * Only requires aliasName and type - all other properties are predefined internally.
 * 
 * Value attributes are automatically configured with:
 * - name: "value" (fixed)
 * - filterable: true (for ID lookups)
 * - sortable: false (values typically don't need sorting)
 * - primaryKey: true (values represent the primary key)
 */
public class ValueDef {
    
    private final String aliasName;
    private final Class<?> type;
    
    private ValueDef(String aliasName, Class<?> type) {
        this.aliasName = aliasName;
        this.type = type;
    }
    
    /**
     * Create a value definition with database column name and Java type
     * 
     * @param aliasName the database column name
     * @param type the Java type
     * @return ValueDef instance
     */
    public static ValueDef of(String aliasName, Class<?> type) {
        Preconditions.checkNotNull(aliasName, "Alias name cannot be null");
        Preconditions.checkArgument(!aliasName.trim().isEmpty(), "Alias name cannot be empty");
        Preconditions.checkNotNull(type, "Type cannot be null");
        
        return new ValueDef(aliasName, type);
    }
    
    /**
     * Convert to AttributeDef with predefined value-specific settings
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttributeDef<?> toAttributeDef() {
        return AttributeDef.name("value")
                .type(type)
                .aliasName(aliasName)
                .filterable(true)    // Always filterable for ID lookups
                .sortable(false)     // Values typically don't need sorting
                .primaryKey(true)    // Values represent the primary key
                .description("Value field for select component")
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