package com.balsam.oasis.common.query.select;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of SelectRegistry for managing select definitions.
 */
@Component
public class SelectRegistryImpl implements SelectRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(SelectRegistryImpl.class);
    
    private final Map<String, SelectDefinition> selects = new ConcurrentHashMap<>();
    
    @Override
    public void register(SelectDefinition definition) {
        if (definition == null || definition.getName() == null) {
            throw new IllegalArgumentException("SelectDefinition and its name cannot be null");
        }
        
        String name = definition.getName();
        if (selects.containsKey(name)) {
            log.warn("Overwriting existing select definition: {}", name);
        }
        
        selects.put(name, definition);
        log.info("Registered select: {}", name);
    }
    
    @Override
    public SelectDefinition get(String name) {
        return selects.get(name);
    }
    
    @Override
    public boolean exists(String name) {
        return selects.containsKey(name);
    }
    
    @Override
    public Set<String> getSelectNames() {
        return Set.copyOf(selects.keySet());
    }
    
    @Override
    public Collection<SelectDefinition> getAllSelects() {
        return List.copyOf(selects.values());
    }
    
    @Override
    public void unregister(String name) {
        SelectDefinition removed = selects.remove(name);
        if (removed != null) {
            log.info("Unregistered select: {}", name);
        }
    }
    
    @Override
    public void clear() {
        int count = selects.size();
        selects.clear();
        log.info("Cleared {} select definitions", count);
    }
}