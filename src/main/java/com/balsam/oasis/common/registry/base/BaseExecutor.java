package com.balsam.oasis.common.registry.base;

import com.balsam.oasis.common.registry.exception.QueryExecutionException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public abstract class BaseExecutor<D extends BaseDefinition, 
                                   C extends BaseContext<D>, 
                                   E extends BaseExecution<C, R>, 
                                   R> {
    
    protected final NamedParameterJdbcTemplate jdbcTemplate;
    protected final BaseRegistry<D> registry;
    protected final BaseSqlBuilder<D, C> sqlBuilder;
    
    protected BaseExecutor(NamedParameterJdbcTemplate jdbcTemplate,
                          BaseRegistry<D> registry,
                          BaseSqlBuilder<D, C> sqlBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.registry = registry;
        this.sqlBuilder = sqlBuilder;
    }
    
    public E execute(String name) {
        D definition = registry.get(name);
        if (definition == null) {
            throw new QueryExecutionException("Definition not found: " + name);
        }
        return createExecution(definition);
    }
    
    protected abstract E createExecution(D definition);
    
    @Transactional(readOnly = true)
    protected R doExecute(C context) {
        context.startExecution();
        try {
            applyDefaultParams(context);
            
            SqlResult sqlResult = sqlBuilder.build(context);
            
            if (context.hasPagination() && context.isIncludeMetadata()) {
                int totalCount = executeCountQuery(context, sqlResult);
                context.setTotalCount(totalCount);
                updatePaginationMetadata(context);
            }
            
            List<?> items = executeMainQuery(sqlResult, context);
            
            R response = buildResponse(items, context);
            
            context.endExecution();
            
            if (context.isAuditEnabled()) {
                audit(context, items.size());
            }
            
            return response;
            
        } catch (Exception e) {
            context.endExecution();
            throw new QueryExecutionException("Execution failed for: " + context.getDefinition().getName(), e);
        }
    }
    
    protected abstract R buildResponse(List<?> items, C context);
    
    protected abstract List<?> executeMainQuery(SqlResult sql, C context);
    
    protected abstract int executeCountQuery(C context, SqlResult sqlResult);
    
    protected abstract void applyDefaultParams(C context);
    
    protected abstract void updatePaginationMetadata(C context);
    
    protected abstract void audit(C context, int resultCount);
    
    public static class SqlResult {
        private final String sql;
        private final Map<String, Object> params;
        
        public SqlResult(String sql, Map<String, Object> params) {
            this.sql = sql;
            this.params = params;
        }
        
        public String getSql() {
            return sql;
        }
        
        public Map<String, Object> getParams() {
            return params;
        }
    }
}