package com.balsam.oasis.common.registry.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.balsam.oasis.common.registry.builder.PlsqlDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.execution.PlsqlExecution;
import com.balsam.oasis.common.registry.engine.plsql.PlsqlExecutorImpl;
import com.balsam.oasis.common.registry.engine.plsql.PlsqlRegistryImpl;

@Service
public class PlsqlService {
    private static final Logger log = LoggerFactory.getLogger(PlsqlService.class);

    private final PlsqlExecutorImpl plsqlExecutor;
    private final PlsqlRegistryImpl plsqlRegistry;

    public PlsqlService(PlsqlExecutorImpl plsqlExecutor, PlsqlRegistryImpl plsqlRegistry) {
        this.plsqlExecutor = plsqlExecutor;
        this.plsqlRegistry = plsqlRegistry;
    }

    public Map<String, Object> executePlsql(String plsqlName, Map<String, Object> params) {
        log.info("Executing PL/SQL: {} with params: {}", plsqlName, params);

        PlsqlDefinitionBuilder plsqlDefinition = plsqlRegistry.resolve(plsqlName, params != null ? params : Map.of());
        if (plsqlDefinition == null) {
            throw new QueryException(plsqlName, QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "PL/SQL block not found: " + plsqlName);
        }

        PlsqlExecution execution = plsqlExecutor.prepare(plsqlDefinition);
        if (params != null) {
            params.forEach(execution::withParam);
        }

        return execution.execute();
    }


    public PlsqlExecution preparePlsql(String plsqlName) {
        return plsqlExecutor.execute(plsqlName);
    }

    public PlsqlDefinitionBuilder getPlsqlDefinition(String plsqlName) {
        return plsqlRegistry.get(plsqlName);
    }


    public boolean exists(String plsqlName) {
        return plsqlRegistry.get(plsqlName) != null;
    }

    public int getRegisteredCount() {
        return plsqlRegistry.size();
    }
}