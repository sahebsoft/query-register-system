package com.balsam.oasis.common.registry.engine.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.api.QueryRegistry;
import com.balsam.oasis.common.registry.util.QueryUtils;

/**
 * Default implementation of QueryRegistry using ConcurrentHashMap.
 * Thread-safe implementation with optimized read/write locking.
 * 
 * <p>
 * This implementation provides:
 * </p>
 * <ul>
 * <li>Thread-safe registration and lookup</li>
 * <li>Validation of query definitions</li>
 * <li>Prevention of duplicate registrations</li>
 * <li>Support for registration listeners (future enhancement)</li>
 * </ul>
 *
 * @author Query Registration System
 * @since 2.0
 */
public class QueryRegistryImpl implements QueryRegistry {

    private static final Logger log = LoggerFactory.getLogger(QueryRegistryImpl.class);

    private final ConcurrentMap<String, QueryDefinitionBuilder> registry = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    @Override
    public void register(QueryDefinitionBuilder definition) {
        validateDefinition(definition);

        String name = definition.getName();
        lock.writeLock().lock();
        try {
            if (registry.putIfAbsent(name, definition) != null) {
                throw new IllegalStateException("Query already registered: " + name);
            }
            // Build comprehensive registration message
            StringBuilder registrationLog = new StringBuilder();
            registrationLog.append("Registered query '" + name + "': ");

            // Add basic info
            registrationLog.append("attributes=").append(definition.getAttributes().size());

            // Add parameters info
            if (definition.hasParams()) {
                registrationLog.append(", has_params=true");

                // Check for unused parameters
                Set<String> unusedParams = QueryUtils.findUnusedParameters(definition);
                if (!unusedParams.isEmpty()) {
                    registrationLog.append(", unused_params=").append(unusedParams);
                }
            }

            log.info(registrationLog.toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            int count = registry.size();
            registry.clear();
            log.info("Cleared {} queries from registry", count);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public QueryDefinitionBuilder get(String name) {
        if (name == null) {
            return null;
        }

        lock.readLock().lock();
        try {
            return registry.get(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getQueryNames() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(registry.keySet()));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<QueryDefinitionBuilder> getAllQueries() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(registry.values()));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return registry.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return registry.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Validate a query definition before registration.
     * 
     * @param definition the definition to validate
     * @throws IllegalArgumentException if the definition is invalid
     */
    private void validateDefinition(QueryDefinitionBuilder definition) {
        if (definition == null) {
            throw new IllegalArgumentException("QueryDefinition cannot be null");
        }

        String name = definition.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("QueryDefinition name is required");
        }

        String sql = definition.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("QueryDefinition SQL is required for query: " + name);
        }

        // Additional validation can be added here
        // - Check for valid attribute definitions
        // - Validate parameter definitions
        // - Check criteria definitions
    }

}