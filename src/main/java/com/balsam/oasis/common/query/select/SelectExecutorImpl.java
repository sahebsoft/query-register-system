package com.balsam.oasis.common.query.select;

import com.balsam.oasis.common.query.core.definition.AttributeDef;
import com.balsam.oasis.common.query.core.definition.CriteriaDef;
import com.balsam.oasis.common.query.core.definition.ParamDef;
import com.balsam.oasis.common.query.exception.QueryExecutionException;
import com.balsam.oasis.common.query.exception.QueryValidationException;
import com.balsam.oasis.common.query.rest.LovItem;
import com.balsam.oasis.common.query.rest.LovResponse;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Implementation of SelectExecutor for executing select queries.
 */
@Component
public class SelectExecutorImpl implements SelectExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(SelectExecutorImpl.class);
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SelectRegistry selectRegistry;
    
    public SelectExecutorImpl(NamedParameterJdbcTemplate jdbcTemplate, 
                              SelectRegistry selectRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.selectRegistry = selectRegistry;
    }
    
    @Override
    public SelectExecution select(String selectName) {
        Preconditions.checkNotNull(selectName, "Select name cannot be null");
        SelectDefinition definition = selectRegistry.get(selectName);
        if (definition == null) {
            throw new QueryExecutionException("Select definition not found: " + selectName);
        }
        return new SelectExecutionImpl(definition);
    }
    
    @Override
    public SelectExecution select(SelectDefinition definition) {
        Preconditions.checkNotNull(definition, "Select definition cannot be null");
        return new SelectExecutionImpl(definition);
    }
    
    @Override
    public SelectExecution prepare(SelectDefinition definition) {
        Preconditions.checkNotNull(definition, "Select definition cannot be null");
        return new SelectExecutionImpl(definition);
    }
    
    /**
     * Implementation of SelectExecution
     */
    private class SelectExecutionImpl implements SelectExecution {
        
        private final SelectDefinition definition;
        private final Map<String, Object> params = new HashMap<>();
        private List<String> ids;
        private String searchTerm;
        private int start = 0;
        private int end = 100;
        
        public SelectExecutionImpl(SelectDefinition definition) {
            this.definition = definition;
            this.end = definition.getDefaultPageSize();
        }
        
        @Override
        public SelectExecution withIds(List<String> ids) {
            this.ids = ids;
            return this;
        }
        
        @Override
        public SelectExecution withId(String id) {
            this.ids = List.of(id);
            return this;
        }
        
        @Override
        public SelectExecution withSearch(String searchTerm) {
            this.searchTerm = searchTerm;
            return this;
        }
        
        @Override
        public SelectExecution withParam(String name, Object value) {
            params.put(name, value);
            return this;
        }
        
        @Override
        public SelectExecution withParams(Map<String, Object> params) {
            this.params.putAll(params);
            return this;
        }
        
        @Override
        public SelectExecution withPagination(int start, int end) {
            Preconditions.checkArgument(start >= 0, "Start must be non-negative");
            Preconditions.checkArgument(end > start, "End must be greater than start");
            int pageSize = end - start;
            Preconditions.checkArgument(pageSize <= definition.getMaxPageSize(), 
                    "Page size exceeds maximum: " + definition.getMaxPageSize());
            
            this.start = start;
            this.end = end;
            return this;
        }
        
        @Override
        public SelectExecution validate() {
            // Validate required parameters
            for (Map.Entry<String, ParamDef<?>> entry : definition.getParams().entrySet()) {
                ParamDef<?> paramDef = entry.getValue();
                if (paramDef.isRequired() && !params.containsKey(entry.getKey())) {
                    throw new QueryValidationException(
                            "Required parameter missing: " + entry.getKey()
                    );
                }
            }
            return this;
        }
        
        @Override
        public LovResponse execute() {
            try {
                String sql = buildSql();
                Map<String, Object> finalParams = buildParams();
                
                log.debug("Executing select '{}': {}", definition.getName(), sql);
                log.debug("Parameters: {}", finalParams);
                
                List<LovItem> items = jdbcTemplate.query(sql, finalParams, new LovRowMapper());
                
                return LovResponse.of(items);
                
            } catch (Exception e) {
                log.error("Failed to execute select '{}': {}", definition.getName(), e.getMessage());
                throw new QueryExecutionException(
                        "Select execution failed: " + e.getMessage(), e
                );
            }
        }
        
        @Override
        public SelectExecution reset() {
            params.clear();
            ids = null;
            searchTerm = null;
            start = 0;
            end = definition.getDefaultPageSize();
            return this;
        }
        
        private String buildSql() {
            String baseSql = definition.getSql();
            
            // Apply criteria
            SelectContext context = new SelectContext();
            context.setParams(params);
            
            // Apply regular criteria and search criteria
            if (definition.hasCriteria()) {
                for (CriteriaDef criteria : definition.getCriteria().values()) {
                    String placeholder = "--" + criteria.getName();
                    if (baseSql.contains(placeholder)) {
                        // Check if criteria should be applied based on its condition
                        boolean shouldApply = false;
                        if (criteria.getCondition() != null) {
                            // Create a simple evaluation context
                            // The condition usually checks if a param exists
                            // We can't use the actual predicate, so check params manually
                            // Look for bind parameters in the SQL
                            String criteriaSql = criteria.getSql();
                            if (criteriaSql != null) {
                                // Extract parameter names from SQL (e.g., :paramName)
                                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":(\\w+)");
                                java.util.regex.Matcher matcher = pattern.matcher(criteriaSql);
                                shouldApply = true;
                                while (matcher.find()) {
                                    String paramName = matcher.group(1);
                                    if (!params.containsKey(paramName) || params.get(paramName) == null) {
                                        shouldApply = false;
                                        break;
                                    }
                                }
                            }
                        } else {
                            // No condition means always apply
                            shouldApply = true;
                        }
                        if (shouldApply) {
                            baseSql = baseSql.replace(placeholder, criteria.getSql());
                        } else {
                            baseSql = baseSql.replace(placeholder, "");
                        }
                    }
                }
            }
            
            // Handle search criteria placeholder
            if (definition.hasSearchCriteria() && searchTerm == null) {
                String placeholder = "--" + definition.getSearchCriteria().getName();
                if (baseSql.contains(placeholder)) {
                    baseSql = baseSql.replace(placeholder, "");
                }
            }
            
            // Handle ID filtering - wrap the entire query
            if (ids != null && !ids.isEmpty()) {
                String valueColumn = definition.getValueAttribute().getAliasName();
                if (ids.size() == 1) {
                    baseSql = "SELECT * FROM (" + baseSql + ") wrapped_query WHERE " + 
                             valueColumn + " = :__id";
                } else {
                    baseSql = "SELECT * FROM (" + baseSql + ") wrapped_query WHERE " + 
                             valueColumn + " IN (:__ids)";
                }
            }
            // Handle search
            else if (searchTerm != null && !searchTerm.isEmpty()) {
                if (definition.hasSearchCriteria()) {
                    // Use defined search criteria
                    CriteriaDef searchCriteria = definition.getSearchCriteria();
                    String placeholder = "--" + searchCriteria.getName();
                    if (baseSql.contains(placeholder)) {
                        baseSql = baseSql.replace(placeholder, searchCriteria.getSql());
                    }
                } else {
                    // Auto-wrap with label LIKE condition
                    String labelColumn = definition.getLabelAttribute().getAliasName();
                    baseSql = "SELECT * FROM (" + baseSql + ") wrapped_query WHERE LOWER(" + 
                             labelColumn + ") LIKE LOWER(:search)";
                }
            }
            
            // Apply pagination using Oracle ROWNUM (Oracle 11g style)
            // TODO: Add database dialect support for proper pagination
            int pageSize = end - start;
            baseSql = "SELECT * FROM (" +
                     "  SELECT inner_query.*, ROWNUM rnum FROM (" +
                     "    " + baseSql +
                     "  ) inner_query" +
                     "  WHERE ROWNUM <= :endRow" +
                     ") WHERE rnum > :startRow";
            return baseSql;
        }
        
        private Map<String, Object> buildParams() {
            Map<String, Object> finalParams = new HashMap<>(params);
            
            // Add default values for parameters
            for (Map.Entry<String, ParamDef<?>> entry : definition.getParams().entrySet()) {
                String paramName = entry.getKey();
                ParamDef<?> paramDef = entry.getValue();
                if (!finalParams.containsKey(paramName) && paramDef.getDefaultValue() != null) {
                    finalParams.put(paramName, paramDef.getDefaultValue());
                }
            }
            
            // Add ID parameter(s)
            if (ids != null && !ids.isEmpty()) {
                if (ids.size() == 1) {
                    finalParams.put("__id", ids.get(0));
                } else {
                    finalParams.put("__ids", ids);
                }
            }
            
            // Add search parameter with wildcards
            if (searchTerm != null && !searchTerm.isEmpty()) {
                finalParams.put("search", "%" + searchTerm + "%");
            }
            
            // Add pagination parameters for Oracle
            finalParams.put("startRow", start);
            finalParams.put("endRow", end);
            
            return finalParams;
        }
        
        /**
         * Row mapper for LOV items
         */
        private class LovRowMapper implements RowMapper<LovItem> {
            @Override
            public LovItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                // Get value and label
                String value = extractValue(rs, definition.getValueAttribute());
                String label = extractValue(rs, definition.getLabelAttribute());
                
                // Get additions if configured
                Map<String, Object> additions = null;
                if (definition.hasAdditions()) {
                    additions = new HashMap<>();
                    for (AttributeDef<?> attr : definition.getAdditionAttributes()) {
                        Object val = extractAttributeValue(rs, attr);
                        if (val != null) {
                            additions.put(attr.getName(), val);
                        }
                    }
                }
                
                return LovItem.of(value, label, additions);
            }
            
            private String extractValue(ResultSet rs, AttributeDef<?> attr) throws SQLException {
                Object value = rs.getObject(attr.getAliasName());
                return value != null ? String.valueOf(value) : null;
            }
            
            private Object extractAttributeValue(ResultSet rs, AttributeDef<?> attr) throws SQLException {
                try {
                    return rs.getObject(attr.getAliasName());
                } catch (SQLException e) {
                    log.debug("Could not extract attribute '{}': {}", attr.getName(), e.getMessage());
                    return null;
                }
            }
        }
    }
}