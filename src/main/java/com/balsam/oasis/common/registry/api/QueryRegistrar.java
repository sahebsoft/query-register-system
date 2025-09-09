package com.balsam.oasis.common.registry.api;

import com.balsam.oasis.common.registry.builder.QueryDefinition;

/**
 * Central registry for QueryDefinition instances.
 * Single responsibility: registration, validation, and lifecycle management.
 * 
 * <p>
 * This interface provides methods for registering query definitions
 * and managing their lifecycle. Implementations should be thread-safe.
 * </p>
 *
 * @author Query Registration System
 * @since 2.0
 */
public interface QueryRegistrar extends QueryRegistry {
    /**
     * Register a query definition.
     * 
     * @param definition the query definition to register
     * @throws IllegalArgumentException if definition is null or invalid
     * @throws IllegalStateException    if a query with the same name already exists
     */
    void register(QueryDefinition definition);

    /**
     * Clear all registered queries.
     */
    void clear();

}