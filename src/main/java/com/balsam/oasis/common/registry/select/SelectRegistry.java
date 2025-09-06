package com.balsam.oasis.common.registry.select;

import java.util.Collection;
import java.util.Set;

/**
 * Registry for managing SelectDefinition instances.
 * Completely separate from QueryRegistry to maintain clean separation.
 */
public interface SelectRegistry {

    /**
     * Register a select definition
     * 
     * @param definition the select definition to register
     */
    void register(SelectDefinition definition);

    /**
     * Get a select definition by name
     * 
     * @param name the name of the select
     * @return the select definition, or null if not found
     */
    SelectDefinition get(String name);

    /**
     * Check if a select is registered
     * 
     * @param name the name of the select
     * @return true if registered, false otherwise
     */
    boolean exists(String name);

    /**
     * Get all registered select names
     * 
     * @return set of select names
     */
    Set<String> getSelectNames();

    /**
     * Get all registered select definitions
     * 
     * @return collection of select definitions
     */
    Collection<SelectDefinition> getAllSelects();

    /**
     * Unregister a select
     * 
     * @param name the name of the select to unregister
     */
    void unregister(String name);

    /**
     * Clear all registered selects
     */
    void clear();
}