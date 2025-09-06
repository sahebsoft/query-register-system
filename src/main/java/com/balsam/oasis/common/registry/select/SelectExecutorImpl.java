package com.balsam.oasis.common.registry.select;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.core.execution.DynamicRowMapper;
import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.processor.ParamProcessor;
import com.balsam.oasis.common.registry.exception.QueryExecutionException;
import com.balsam.oasis.common.registry.exception.QueryValidationException;
import com.balsam.oasis.common.registry.rest.SelectItem;
import com.balsam.oasis.common.registry.rest.SelectResponse;
import com.balsam.oasis.common.registry.util.TypeConverter;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Implementation of SelectExecutor for executing select queries.
 * Now uses SelectSqlBuilder and enhanced SelectContext for consistency with QueryExecutor.
 */
@Component
public class SelectExecutorImpl implements SelectExecutor {

    private static final Logger log = LoggerFactory.getLogger(SelectExecutorImpl.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SelectRegistry selectRegistry;
    private final SelectSqlBuilder sqlBuilder;

    public SelectExecutorImpl(NamedParameterJdbcTemplate jdbcTemplate,
            SelectRegistry selectRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.selectRegistry = selectRegistry;
        this.sqlBuilder = new SelectSqlBuilder();
    }

    @Override
    public SelectExecution select(String selectName) {
        Preconditions.checkNotNull(selectName, "Select name cannot be null");
        SelectDefinition definition = selectRegistry.get(selectName);
        if (definition == null) {
            throw new QueryExecutionException("Select definition not found: " + selectName);
        }
        return new SelectExecutionImpl(definition, this);
    }

    @Override
    public SelectExecution select(SelectDefinition definition) {
        Preconditions.checkNotNull(definition, "Select definition cannot be null");
        return new SelectExecutionImpl(definition, this);
    }

    @Override
    public SelectExecution prepare(SelectDefinition definition) {
        Preconditions.checkNotNull(definition, "Select definition cannot be null");
        return new SelectExecutionImpl(definition, this);
    }

    /**
     * Internal execution method
     */
    @Transactional(readOnly = true)
    SelectResponse doExecute(SelectContext context) {
        context.startExecution();

        try {
            // Add default parameter values
            applyDefaultParams(context);

            // Build SQL using SelectSqlBuilder
            SelectSqlBuilder.SelectSqlResult sqlResult = sqlBuilder.build(context);
            String finalSql = sqlResult.getSql();
            Map<String, Object> params = sqlResult.getParams();

            log.debug("Executing select '{}': {}", context.getDefinition().getName(), finalSql);
            log.debug("Parameters: {}", params);

            // Calculate total count if needed for pagination
            if (context.hasPagination() && context.isIncludeMetadata()) {
                int totalCount = executeTotalCountQuery(context);
                context.setTotalCount(totalCount);
                updatePaginationMetadata(context);
            }

            // Execute query
            List<SelectItem> items = jdbcTemplate.query(finalSql, params, 
                new SelectRowMapper(context.getDefinition()));

            // Build response with metadata if requested
            SelectResponse response = buildResponse(items, context);

            context.endExecution();

            if (context.isAuditEnabled()) {
                log.info("Select '{}' executed in {}ms, returned {} items",
                        context.getDefinition().getName(),
                        context.getExecutionTime(),
                        items.size());
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to execute select '{}': {}", 
                context.getDefinition().getName(), e.getMessage());
            throw new QueryExecutionException(
                    "Select execution failed: " + e.getMessage(), e);
        } finally {
            context.endExecution();
        }
    }

    private void applyDefaultParams(SelectContext context) {
        SelectDefinition definition = context.getDefinition();
        for (Map.Entry<String, ParamDef<?>> entry : definition.getParams().entrySet()) {
            String paramName = entry.getKey();
            ParamDef<?> paramDef = entry.getValue();
            if (!context.hasParam(paramName) && paramDef.getDefaultValue() != null) {
                context.setParam(paramName, paramDef.getDefaultValue());
            }
        }
    }

    private int executeTotalCountQuery(SelectContext context) {
        SelectSqlBuilder.SelectSqlResult countResult = sqlBuilder.buildCountQuery(context);
        Integer count = jdbcTemplate.queryForObject(
            countResult.getSql(),
            countResult.getParams(),
            Integer.class
        );
        return count != null ? count : 0;
    }

    private void updatePaginationMetadata(SelectContext context) {
        if (!context.hasPagination()) {
            return;
        }

        SelectContext.Pagination pagination = context.getPagination();
        if (context.getTotalCount() != null) {
            pagination.setTotal(context.getTotalCount());
            pagination.setHasNext(pagination.getEnd() < context.getTotalCount());
            pagination.setHasPrevious(pagination.getStart() > 0);
        }
    }

    private SelectResponse buildResponse(List<SelectItem> items, SelectContext context) {
        SelectResponse response = SelectResponse.of(items);

        // Add metadata if requested
        if (context.isIncludeMetadata()) {
            Map<String, Object> metadata = new HashMap<>();
            
            // Add pagination metadata
            if (context.hasPagination()) {
                metadata.put("pagination", context.getPagination());
            }

            // Add applied criteria
            if (!context.getAppliedCriteria().isEmpty()) {
                metadata.put("appliedCriteria", context.getAppliedCriteria());
            }

            // Add execution time
            metadata.put("executionTimeMs", context.getExecutionTime());

            // Add custom metadata from context
            metadata.putAll(context.getMetadata());

            response.setMetadata(metadata);
        }

        return response;
    }

    /**
     * Implementation of SelectExecution
     */
    private static class SelectExecutionImpl implements SelectExecution {

        private final SelectDefinition definition;
        private final SelectContext context;
        private final SelectExecutorImpl executor;

        public SelectExecutionImpl(SelectDefinition definition, SelectExecutorImpl executor) {
            this.definition = definition;
            this.executor = executor;
            this.context = SelectContext.builder()
                    .definition(definition)
                    .build();
            // Set default pagination
            this.context.setPagination(SelectContext.Pagination.builder()
                    .start(0)
                    .end(definition.getDefaultPageSize())
                    .build());
        }

        @Override
        public SelectExecution withIds(List<String> ids) {
            context.setIds(ids);
            return this;
        }

        @Override
        public SelectExecution withId(String id) {
            context.setIds(List.of(id));
            return this;
        }

        @Override
        public SelectExecution withSearch(String searchTerm) {
            context.setSearchTerm(searchTerm);
            return this;
        }

        @Override
        public SelectExecution withParam(String name, Object value) {
            context.setParam(name, value);
            return this;
        }

        @Override
        public SelectExecution withParams(Map<String, Object> params) {
            context.getParams().putAll(params);
            return this;
        }

        @Override
        public SelectExecution withPagination(int start, int end) {
            Preconditions.checkArgument(start >= 0, "Start must be non-negative");
            Preconditions.checkArgument(end > start, "End must be greater than start");
            int pageSize = end - start;
            Preconditions.checkArgument(pageSize <= definition.getMaxPageSize(),
                    "Page size (%s) exceeds maximum allowed (%s)", pageSize, definition.getMaxPageSize());
            context.setPagination(SelectContext.Pagination.builder()
                    .start(start)
                    .end(end)
                    .build());
            return this;
        }

        @Override
        public SelectExecution validate() {
            // Validate required parameters
            for (Map.Entry<String, ParamDef<?>> entry : definition.getParams().entrySet()) {
                ParamDef<?> paramDef = entry.getValue();
                if (paramDef.isRequired() && !context.hasParam(entry.getKey())) {
                    throw new QueryValidationException("Required parameter '" + entry.getKey() + "' is missing");
                }

                // Validate parameter value if processor is present
                if (paramDef.getProcessor() != null && context.hasParam(entry.getKey())) {
                    Object value = context.getParam(entry.getKey());
                    // Use processor to validate and process the value
                    try {
                        // ParamProcessor is generic, we need to handle it carefully
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        ParamProcessor processor = paramDef.getProcessor();
                        @SuppressWarnings("unchecked")
                        Object processed = processor.process(value, null);
                        context.setParam(entry.getKey(), processed);
                    } catch (Exception e) {
                        throw new QueryValidationException(
                            "Parameter '" + entry.getKey() + "' validation failed: " + e.getMessage());
                    }
                }
            }
            return this;
        }

        @Override
        public SelectResponse execute() {
            // Validate before execution
            validate();
            
            // Delegate to executor
            return executor.doExecute(context);
        }

        @Override
        public SelectExecution reset() {
            context.getParams().clear();
            context.setIds(null);
            context.setSearchTerm(null);
            context.setPagination(SelectContext.Pagination.builder()
                    .start(0)
                    .end(definition.getDefaultPageSize())
                    .build());
            context.getAppliedCriteria().clear();
            context.getMetadata().clear();
            return this;
        }
    }

    /**
     * Row mapper for Select items using TypeConverter for consistent type handling
     */
    private class SelectRowMapper implements RowMapper<SelectItem> {
        private final SelectDefinition definition;

        public SelectRowMapper(SelectDefinition definition) {
            this.definition = definition;
        }

        @Override
        public SelectItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            // Get value and label with type conversion
            String value = extractValue(rs, definition.getValueAttribute());
            String label = extractValue(rs, definition.getLabelAttribute());

            // Get additions if configured
            Map<String, Object> additions = null;
            if (definition.hasAdditions()) {
                additions = new HashMap<>();
                for (AttributeDef<?> attr : definition.getAdditionAttributes()) {
                    Object val = extractAttributeValue(rs, attr);
                    if (val != null) {
                        // Apply type conversion
                        val = TypeConverter.convert(val, attr.getType());
                        additions.put(attr.getName(), val);
                    }
                }
            }

            return SelectItem.of(value, label, additions);
        }

        private String extractValue(ResultSet rs, AttributeDef<?> attr) throws SQLException {
            Object value = rs.getObject(attr.getAliasName());
            if (value != null) {
                // Apply type conversion before converting to string
                value = TypeConverter.convert(value, attr.getType());
            }
            return value != null ? String.valueOf(value) : null;
        }

        private Object extractAttributeValue(ResultSet rs, AttributeDef<?> attr) throws SQLException {
            try {
                Object value = rs.getObject(attr.getAliasName());
                if (value != null) {
                    // Apply type conversion
                    value = TypeConverter.convert(value, attr.getType());
                }
                return value;
            } catch (SQLException e) {
                log.debug("Could not extract attribute '{}': {}", attr.getName(), e.getMessage());
                return null;
            }
        }
    }
}