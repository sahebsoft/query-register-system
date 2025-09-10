package com.balsam.oasis.common.registry.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.balsam.oasis.common.registry.api.QueryRegistrar;
import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.engine.sql.MetadataCache;
import com.balsam.oasis.common.registry.engine.sql.MetadataCacheBuilder;

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

    @Autowired(required = false)
    private MetadataCacheBuilder metadataCacheBuilder;

    @Override
    public void register(QueryDefinition definition) {
        validateDefinition(definition);

        // Automatically pre-warm metadata cache for dynamic queries
        if (definition.isIncludeDynamicAttributes() && metadataCacheBuilder != null) {
            try {
                log.debug("Pre-warming metadata cache for dynamic query: {}", definition.getName());
                MetadataCache cache = metadataCacheBuilder.buildCache(definition);
                definition = definition.withMetadataCache(cache);
                log.info("Successfully pre-warmed metadata cache for query '{}' with {} columns",
                        definition.getName(), cache.getColumnCount());

                // Register dynamic attributes from metadata cache
                Map<String, AttributeDef<?>> combinedAttributes = new LinkedHashMap<>(definition.getAttributes());
                String[] columnNames = cache.getColumnNames();
                NamingStrategy strategy = definition.getDynamicAttributeNamingStrategy();

                for (String columnName : columnNames) {
                    String attributeName = strategy.convert(columnName);

                    // Skip if this attribute is already defined statically
                    if (!combinedAttributes.containsKey(attributeName)) {
                        Class<?> javaType = cache.getJavaTypeForColumn(columnName);
                        if (javaType != null) {
                            AttributeDef<?> dynamicAttr = AttributeDef.name(attributeName)
                                    .type(javaType)
                                    .aliasName(columnName)
                                    .filterable(true)
                                    .sortable(true)
                                    .selected(true)
                                    .build();
                            combinedAttributes.put(attributeName, dynamicAttr);
                            log.trace("Registered dynamic attribute '{}' for column '{}'", attributeName, columnName);
                        }
                    }
                }

                // Create new definition with combined attributes
                int originalSize = definition.getAttributes().size();
                definition = definition.withAttributes(combinedAttributes);
                log.info("Registered {} dynamic attributes for query '{}'",
                        combinedAttributes.size() - originalSize, definition.getName());

            } catch (Exception e) {
                log.warn(
                        "Failed to pre-warm metadata cache for query '{}': {}. Dynamic attributes may not work properly.",
                        definition.getName(), e.getMessage());
                // Continue with registration even if metadata pre-warming fails
            }
        }

        String name = definition.getName();
        lock.writeLock().lock();
        try {
            if (registry.putIfAbsent(name, definition) != null) {
                throw new IllegalStateException("Query already registered: " + name);
            }
            log.info("Registered query: {} (dynamic: {}, metadata cached: {})",
                    name, definition.isIncludeDynamicAttributes(),
                    definition.getMetadataCache() != null);
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