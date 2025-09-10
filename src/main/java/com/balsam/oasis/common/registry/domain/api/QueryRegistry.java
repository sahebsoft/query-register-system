package com.balsam.oasis.common.registry.domain.api;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.balsam.oasis.common.registry.builder.QueryDefinition;

/**
 * Read-only view of registered queries.
 * Provides lookup and discovery methods without modification capabilities.
 * 
 * <p>
 * This interface is used by components that need to access registered
 * queries but should not be able to modify the registry.
 * </p>
 *
 * @author Query Registration System
 * @since 2.0
 */
public interface QueryRegistry {

    /**
     * Get a query definition by name.
     * 
     * @param name the name of the query
     * @return the query definition, or null if not found
     */
    QueryDefinition get(String name);

    /**
     * Get a query definition by name as an Optional.
     * 
     * @param name the name of the query
     * @return an Optional containing the query definition, or empty if not found
     */
    Optional<QueryDefinition> find(String name);

    /**
     * Check if a query exists by name.
     * 
     * @param name the name of the query
     * @return true if the query exists, false otherwise
     */
    boolean exists(String name);

    /**
     * Get all registered query names.
     * 
     * @return an unmodifiable set of query names
     */
    Set<String> getQueryNames();

    /**
     * Get all registered query definitions.
     * 
     * @return an unmodifiable collection of query definitions
     */
    Collection<QueryDefinition> getAllQueries();

    /**
     * Get the count of registered queries.
     * 
     * @return the number of registered queries
     */
    int size();

    /**
     * Check if the registry is empty.
     * 
     * @return true if no queries are registered, false otherwise
     */
    boolean isEmpty();
}