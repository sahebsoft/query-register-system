package com.balsam.oasis.common.registry.engine.plsql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.balsam.oasis.common.registry.builder.PlsqlDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.definition.PlsqlParamDef;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.execution.PlsqlContext;
import com.balsam.oasis.common.registry.domain.execution.PlsqlExecution;

public class PlsqlExecutorImpl {
    private static final Logger log = LoggerFactory.getLogger(PlsqlExecutorImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final PlsqlRegistryImpl plsqlRegistry;

    public PlsqlExecutorImpl(JdbcTemplate jdbcTemplate, PlsqlRegistryImpl plsqlRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.plsqlRegistry = plsqlRegistry;
    }

    public PlsqlExecution execute(String plsqlName) {
        PlsqlDefinitionBuilder definition = plsqlRegistry.get(plsqlName);
        if (definition == null) {
            throw new QueryException(plsqlName, QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "PL/SQL block not found: " + plsqlName);
        }
        return new PlsqlExecution(definition, this);
    }

    public PlsqlExecution prepare(PlsqlDefinitionBuilder definition) {
        return new PlsqlExecution(definition, this);
    }

    @Transactional
    public Map<String, Object> doExecute(PlsqlContext context) {
        try {
            // Run pre-processors
            runPreProcessors(context);

            // Process parameters and apply defaults
            String finalPlsql = processParameters(context);

            log.debug("Executing PL/SQL '{}': {}", context.getDefinition().getName(), finalPlsql);
            log.debug("Parameters: {}", context.getParams());

            // Execute PL/SQL with proper named parameter handling
            Map<String, Object> outputs = jdbcTemplate.execute(
                    (Connection con) -> con.prepareCall(finalPlsql),
                    (CallableStatementCallback<Map<String, Object>>) cs -> {
                        // Bind parameters by parsing SQL order
                        bindParametersInSqlOrder(cs, finalPlsql, context);

                        // Register OUT parameters by parsing SQL order
                        registerOutParametersInSqlOrder(cs, finalPlsql, context);

                        // Execute
                        cs.execute();

                        // Collect outputs by parsing SQL order
                        return collectOutputsInSqlOrder(cs, finalPlsql, context);
                    });

            // Make outputs mutable for post-processors
            Map<String, Object> mutableOutputs = new HashMap<>(outputs);

            // Run post-processors (can modify/add outputs directly)
            mutableOutputs = runPostProcessors(mutableOutputs, context);

            return mutableOutputs;

        } catch (Exception e) {
            log.error("PL/SQL execution failed for '{}': {}",
                    context.getDefinition().getName(), e.getMessage(), e);

            if (e instanceof QueryException queryException) {
                throw queryException;
            } else {
                throw new QueryException(
                        context.getDefinition().getName(),
                        QueryException.ErrorCode.EXECUTION_ERROR,
                        "PL/SQL execution failed: " + e.getMessage(), e);
            }
        }
    }

    private void runPreProcessors(PlsqlContext context) {
        PlsqlDefinitionBuilder definition = context.getDefinition();
        if (definition.hasPreProcessors()) {
            definition.getPreProcessors().forEach(processor -> processor.process(context));
        }
    }

    private Map<String, Object> runPostProcessors(Map<String, Object> outputs, PlsqlContext context) {
        PlsqlDefinitionBuilder definition = context.getDefinition();
        if (!definition.hasPostProcessors()) {
            return outputs;
        }

        Map<String, Object> processedOutputs = outputs;
        for (var processor : definition.getPostProcessors()) {
            processedOutputs = processor.process(processedOutputs, context);
        }
        return processedOutputs;
    }

    private String processParameters(PlsqlContext context) {
        String plsql = context.getDefinition().getPlsql();

        for (PlsqlParamDef<?> param : context.getDefinition().getParameters().values()) {
            if (!context.hasParam(param.name())) {
                // Apply PL/SQL default if specified
                if (param.hasPlsqlDefault()) {
                    plsql = plsql.replace(":" + param.name(), param.plsqlDefault());
                }
                // Apply Java default if specified
                else if (param.hasDefaultValue()) {
                    context.addParam(param.name(), param.defaultValue());
                }
                // Check if required (only for IN and INOUT)
                else if (param.required() &&
                        (param.mode() == PlsqlParamDef.ParamMode.IN ||
                                param.mode() == PlsqlParamDef.ParamMode.INOUT)) {
                    throw new QueryException("Required parameter missing: " + param.name());
                }
            }
        }

        return plsql;
    }

    private void bindParameterValue(CallableStatement cs, int index, Object value, Class<?> targetType, int sqlType)
            throws SQLException {
        if (value == null) {
            cs.setNull(index, sqlType);
            return;
        }

        // Handle date types specifically using sqlType
        if (sqlType == Types.DATE) {
            java.util.Date dateValue = convertToDate(value);
            if (dateValue != null) {
                cs.setDate(index, new java.sql.Date(dateValue.getTime()));
            } else {
                cs.setNull(index, Types.DATE);
            }
            return;
        }

        if (sqlType == Types.TIMESTAMP) {
            java.util.Date dateValue = convertToDate(value);
            if (dateValue != null) {
                cs.setTimestamp(index, new java.sql.Timestamp(dateValue.getTime()));
            } else {
                cs.setNull(index, Types.TIMESTAMP);
            }
            return;
        }

        // Handle other types based on sqlType
        Object convertedValue = convertParameterValue(value, targetType);
        if (convertedValue != null) {
            // Use type-specific setters for better Oracle compatibility
            if (sqlType == Types.VARCHAR || sqlType == Types.CHAR) {
                cs.setString(index, (String) convertedValue);
            } else if (sqlType == Types.INTEGER) {
                cs.setInt(index, ((Number) convertedValue).intValue());
            } else if (sqlType == Types.BIGINT) {
                cs.setLong(index, ((Number) convertedValue).longValue());
            } else if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL) {
                cs.setBigDecimal(index, (java.math.BigDecimal) convertedValue);
            } else if (sqlType == Types.BOOLEAN) {
                cs.setBoolean(index, (Boolean) convertedValue);
            } else {
                cs.setObject(index, convertedValue, sqlType);
            }
        } else {
            cs.setNull(index, sqlType);
        }
    }

    private Object convertParameterValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // If value is already the correct type, return as-is
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // Handle date conversions
        if (targetType == java.util.Date.class || targetType == java.sql.Date.class) {
            return convertToDate(value);
        }

        // Handle other type conversions as needed
        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.valueOf((String) value);
                } catch (NumberFormatException e) {
                    log.warn("Could not convert '{}' to Integer", value);
                    return value;
                }
            }
        }

        // Return original value if no conversion available
        return value;
    }

    private java.util.Date convertToDate(Object value) {
        if (value instanceof java.util.Date) {
            return (java.util.Date) value;
        }

        if (value instanceof String) {
            String dateStr = (String) value;

            // Try different date formats
            String[] dateFormats = {
                    "yyyy-MM-dd",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
            };

            for (String format : dateFormats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    return sdf.parse(dateStr);
                } catch (ParseException e) {
                    // Try next format
                }
            }

            // Try ISO LocalDate
            try {
                LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                return java.sql.Date.valueOf(localDate);
            } catch (Exception e) {
                // Try ISO LocalDateTime
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return java.sql.Timestamp.valueOf(localDateTime);
                } catch (Exception ex) {
                    log.warn("Could not parse date string: {}", dateStr);
                }
            }
        }

        log.warn("Could not convert value '{}' to Date, returning null", value);
        return null;
    }

    private void bindParametersInSqlOrder(CallableStatement cs, String sql, PlsqlContext context) throws SQLException {
        List<String> paramOrder = extractParameterOrder(sql);
        int index = 1;

        log.debug("Parameter order extracted from SQL: {}", paramOrder);

        for (String paramName : paramOrder) {
            PlsqlParamDef<?> param = context.getDefinition().getParameters().get(paramName);
            if (param == null) {
                throw new QueryException("Parameter not found in definition: " + paramName);
            }

            PlsqlParamDef.ParamMode mode = param.mode();

            log.debug("Binding parameter {} at index {} with mode {}", paramName, index, mode);

            // Bind IN and INOUT parameters (for each occurrence)
            if (mode == PlsqlParamDef.ParamMode.IN || mode == PlsqlParamDef.ParamMode.INOUT) {
                Object value = context.getParam(paramName);
                if (value != null) {
                    bindParameterValue(cs, index, value, param.type(), param.sqlType());
                    log.debug("Bound parameter {} at index {} with value: {}", paramName, index, value);
                } else {
                    cs.setNull(index, param.sqlType());
                    log.debug("Bound parameter {} at index {} with NULL", paramName, index);
                }
            }
            index++;
        }
        log.debug("Total parameters bound: {}", index - 1);
    }

    private void registerOutParametersInSqlOrder(CallableStatement cs, String sql, PlsqlContext context)
            throws SQLException {
        List<String> paramOrder = extractParameterOrder(sql);
        int index = 1;

        for (String paramName : paramOrder) {
            PlsqlParamDef<?> param = context.getDefinition().getParameters().get(paramName);
            if (param == null) {
                throw new QueryException("Parameter not found in definition: " + paramName);
            }

            PlsqlParamDef.ParamMode mode = param.mode();

            // Register OUT and INOUT parameters
            if (mode == PlsqlParamDef.ParamMode.OUT || mode == PlsqlParamDef.ParamMode.INOUT) {
                cs.registerOutParameter(index, param.sqlType());
            }
            index++;
        }
    }

    private Map<String, Object> collectOutputsInSqlOrder(CallableStatement cs, String sql, PlsqlContext context)
            throws SQLException {
        Map<String, Object> outputs = new HashMap<>();
        List<String> paramOrder = extractParameterOrder(sql);
        int index = 1;

        for (String paramName : paramOrder) {
            PlsqlParamDef<?> param = context.getDefinition().getParameters().get(paramName);
            if (param == null) {
                throw new QueryException("Parameter not found in definition: " + paramName);
            }

            PlsqlParamDef.ParamMode mode = param.mode();

            // Collect OUT and INOUT parameters
            if (mode == PlsqlParamDef.ParamMode.OUT || mode == PlsqlParamDef.ParamMode.INOUT) {
                Object value = cs.getObject(index);
                outputs.put(paramName, value);
            }
            index++;
        }

        return outputs;
    }

    private List<String> extractParameterOrder(String sql) {
        List<String> paramOrder = new ArrayList<>();

        Pattern pattern = Pattern.compile(":([a-zA-Z_]\\w*)");
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            paramOrder.add(paramName); // Add every occurrence, not just unique names
        }

        return paramOrder;
    }

}