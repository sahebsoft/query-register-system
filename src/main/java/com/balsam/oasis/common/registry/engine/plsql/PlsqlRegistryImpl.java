package com.balsam.oasis.common.registry.engine.plsql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.builder.PlsqlDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.definition.PlsqlParamDef;
import com.balsam.oasis.common.registry.domain.exception.QueryException;

public class PlsqlRegistryImpl {
    private static final Logger log = LoggerFactory.getLogger(PlsqlRegistryImpl.class);

    private final ConcurrentMap<String, List<PlsqlDefinitionBuilder>> registry = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void register(PlsqlDefinitionBuilder definition) {
        validateDefinition(definition);

        String name = definition.getName();
        lock.writeLock().lock();
        try {
            registry.computeIfAbsent(name, k -> new ArrayList<>()).add(definition);

            StringBuilder registrationLog = new StringBuilder();
            registrationLog.append("Registered PL/SQL '" + name + "': ");
            registrationLog.append("parameters=").append(definition.getParameters().size());

            if (definition.hasParameters()) {
                long inParams = definition.getParameters().values().stream()
                        .filter(p -> p.mode() == PlsqlParamDef.ParamMode.IN
                                || p.mode() == PlsqlParamDef.ParamMode.INOUT)
                        .count();
                long outParams = definition.getParameters().values().stream()
                        .filter(p -> p.mode() == PlsqlParamDef.ParamMode.OUT
                                || p.mode() == PlsqlParamDef.ParamMode.INOUT)
                        .count();
                registrationLog.append(" (IN=").append(inParams)
                        .append(", OUT=").append(outParams).append(")");
            }

            // Check for overloads
            List<PlsqlDefinitionBuilder> overloads = registry.get(name);
            if (overloads.size() > 1) {
                registrationLog.append(", overloads=").append(overloads.size());
            }

            log.info(registrationLog.toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PlsqlDefinitionBuilder get(String name) {
        if (name == null) {
            return null;
        }
        lock.readLock().lock();
        try {
            List<PlsqlDefinitionBuilder> candidates = registry.get(name);
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            // If multiple overloads exist without parameters, return the first one
            return candidates.get(0);
        } finally {
            lock.readLock().unlock();
        }
    }

    public PlsqlDefinitionBuilder resolve(String name, Map<String, Object> params) {
        if (name == null) {
            return null;
        }
        lock.readLock().lock();
        try {
            List<PlsqlDefinitionBuilder> candidates = registry.get(name);
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }

            if (candidates.size() == 1) {
                return candidates.get(0);
            }

            // Find best match based on provided parameters
            return candidates.stream()
                    .filter(def -> matchesParameters(def, params))
                    .findFirst()
                    .orElseThrow(() -> new QueryException(name, QueryException.ErrorCode.VALIDATION_ERROR,
                            "No matching PL/SQL parameters found for: " + name + " with params: " + params.keySet()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return registry.values().stream().mapToInt(List::size).sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return registry.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void validateDefinition(PlsqlDefinitionBuilder definition) {
        if (definition == null) {
            throw new IllegalArgumentException("PlsqlDefinition cannot be null");
        }
        String name = definition.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("PlsqlDefinition name is required");
        }
        String plsql = definition.getPlsql();
        if (plsql == null || plsql.trim().isEmpty()) {
            throw new IllegalArgumentException("PlsqlDefinition PL/SQL is required for: " + name);
        }
    }

    private boolean matchesParameters(PlsqlDefinitionBuilder def, Map<String, Object> params) {
        // Check if all required IN/INOUT parameters are provided or have defaults
        return def.getParameters().values().stream()
                .filter(p -> p.mode() == PlsqlParamDef.ParamMode.IN || p.mode() == PlsqlParamDef.ParamMode.INOUT)
                .allMatch(p -> !p.required() || p.hasDefaultValue() || p.hasPlsqlDefault()
                        || params.containsKey(p.name()));
    }
}