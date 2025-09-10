package com.balsam.oasis.common.registry.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.balsam.oasis.common.registry.api.QueryRegistrar;
import com.balsam.oasis.common.registry.builder.QueryDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of QueryRegistrar using ConcurrentHashMap.
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
public class QueryRegistrarImpl implements QueryRegistrar {

    private static final Logger log = LoggerFactory.getLogger(QueryRegistrarImpl.class);

    private final ConcurrentMap<String, QueryDefinition> registry = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void register(QueryDefinition definition) {
        validateDefinition(definition);

        String name = definition.getName();
        lock.writeLock().lock();
        try {
            if (registry.putIfAbsent(name, definition) != null) {
                throw new IllegalStateException("Query already registered: " + name);
            }
            log.info("Registered query: {}", name);
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
    public QueryDefinition get(String name) {
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
    public Optional<QueryDefinition> find(String name) {
        return Optional.ofNullable(get(name));
    }

    @Override
    public boolean exists(String name) {
        if (name == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return registry.containsKey(name);
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
    public Collection<QueryDefinition> getAllQueries() {
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
     * Update an existing query definition with metadata cache.
     * This is used after metadata cache pre-warming.
     * 
     * @param name the query name
     * @param updatedDefinition the updated definition with metadata cache
     */
    public void updateWithMetadataCache(String name, QueryDefinition updatedDefinition) {
        if (name == null || updatedDefinition == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            if (registry.containsKey(name)) {
                registry.put(name, updatedDefinition);
                log.debug("Updated query '{}' with metadata cache", name);
            } else {
                log.warn("Cannot update non-existent query: {}", name);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Validate a query definition before registration.
     * 
     * @param definition the definition to validate
     * @throws IllegalArgumentException if the definition is invalid
     */
    private void validateDefinition(QueryDefinition definition) {
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