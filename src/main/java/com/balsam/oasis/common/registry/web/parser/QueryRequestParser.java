package com.balsam.oasis.common.registry.web.parser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.MultiValueMap;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.FilterOp;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.definition.SortDir;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.sql.util.TypeConverter;
import com.balsam.oasis.common.registry.exception.QueryValidationException;
import com.balsam.oasis.common.registry.web.dto.request.QueryRequest;

/**
 * Parses HTTP request parameters into query execution parameters.
 * Handles parameter parsing, filter extraction, and sort specification.
 *
 * @author Query Registration System
 * @since 1.0
 */
public class QueryRequestParser {

    private static final Pattern FILTER_PATTERN = Pattern.compile("^filter\\.(.+?)(?:\\.(\\w+))?$");
    private static final Pattern SORT_PATTERN = Pattern.compile("^([^.]+)\\.(asc|desc)$");

    public QueryRequest parse(MultiValueMap<String, String> allParams, int start, int end, String metadataLevel) {
        return parse(allParams, start, end, metadataLevel, null);
    }

    public QueryRequest parse(MultiValueMap<String, String> allParams, int start, int end, String metadataLevel,
            QueryDefinition queryDefinition) {
        Map<String, Object> params = new HashMap<>();
        Map<String, QueryContext.Filter> filters = new LinkedHashMap<>();
        List<QueryContext.SortSpec> sorts = new ArrayList<>();
        Set<String> selectedFields = null;

        // Parse each parameter
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            String paramName = entry.getKey();
            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                continue;
            }

            String value = values.get(0); // Take first value for single-valued params

            // Check for _select parameter
            if (paramName.equals("_select")) {
                // Parse comma-separated field names
                if (selectedFields == null) {
                    selectedFields = new HashSet<>();
                }
                String fieldsValue = values.get(0);
                if (fieldsValue != null && !fieldsValue.isEmpty()) {
                    selectedFields.addAll(Arrays.asList(fieldsValue.split(",")));
                }
                continue;
            }

            // Skip pagination and system parameters
            if (paramName.startsWith("_")) {
                continue;
            }

            // Check for filter pattern: filter.attribute or filter.attribute.op
            Matcher filterMatcher = FILTER_PATTERN.matcher(paramName);
            if (filterMatcher.matches()) {
                String attribute = filterMatcher.group(1);
                String opPart = filterMatcher.group(2);

                if (opPart != null && opPart.equals("op")) {
                    // This is the operator specification
                    // Look for corresponding value parameter
                    String valueKey = "filter." + attribute + ".value";
                    String valueKey2 = "filter." + attribute + ".value2";

                    FilterOp op = FilterOp.valueOf(value.toUpperCase());
                    Object filterValue = null;
                    Object filterValue2 = null;

                    if (allParams.containsKey(valueKey)) {
                        Class<?> attrType = getAttributeType(queryDefinition, attribute);
                        filterValue = parseValue(allParams.getFirst(valueKey), attrType);
                    }
                    if (allParams.containsKey(valueKey2)) {
                        Class<?> attrType = getAttributeType(queryDefinition, attribute);
                        filterValue2 = parseValue(allParams.getFirst(valueKey2), attrType);
                    }

                    filters.put(attribute, QueryContext.Filter.builder()
                            .attribute(attribute)
                            .operator(op)
                            .value(filterValue)
                            .value2(filterValue2)
                            .build());
                } else if (opPart != null) {
                    // This might be an operator shortcut like filter.name.gte
                    try {
                        FilterOp op = FilterOp.fromUrlShortcut(opPart);
                        Class<?> attrType = getAttributeType(queryDefinition, attribute);

                        // Special handling for IN and NOT_IN operators - always use values list
                        if (op == FilterOp.IN || op == FilterOp.NOT_IN) {
                            List<Object> valuesList;
                            if (value.contains(",")) {
                                // Multiple values: split by comma
                                valuesList = Arrays.stream(value.split(","))
                                        .map(String::trim)
                                        .map(v -> parseValue(v, attrType))
                                        .collect(Collectors.toList());
                            } else {
                                // Single value: wrap in a list
                                valuesList = Collections.singletonList(parseValue(value, attrType));
                            }

                            filters.put(attribute, QueryContext.Filter.builder()
                                    .attribute(attribute)
                                    .operator(op)
                                    .values(valuesList)
                                    .build());
                        } else {
                            // Check if we already have a filter for this attribute (for range queries)
                            if (filters.containsKey(attribute)) {
                                // Log warning - multiple filters on same attribute, last one wins
                                // TODO: In future, could combine into compound filter
                                System.err.println("Warning: Multiple filters on attribute '" + attribute +
                                        "'. Only the last one will be applied.");
                            }
                            filters.put(attribute, QueryContext.Filter.builder()
                                    .attribute(attribute)
                                    .operator(op)
                                    .value(parseValue(value, attrType))
                                    .build());
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid operator - throw validation error instead of silently converting
                        throw new QueryValidationException("Query '" + queryDefinition.getName() +
                                "': Invalid filter operator '" + opPart + "' for attribute '" + attribute + "'");
                    }
                } else {
                    // Simple filter: filter.attribute=value (potentially multiple values)
                    Class<?> attrType = getAttributeType(queryDefinition, attribute);
                    parseSimpleFilter(attribute, values, filters, attrType);
                }
                continue;
            }

            // Check for sort pattern
            if (paramName.equals("sort")) {
                parseSortParameter(value, sorts);
                continue;
            }

            // All other parameters are treated as query parameters
            // Only accept parameters that are defined in the QueryDefinition
            // Skip empty string parameters to let defaults apply
            if (value != null && !value.trim().isEmpty()) {
                Class<?> paramType = getParamType(queryDefinition, paramName);

                // Only process if the parameter is defined in the query
                if (paramType != null) {
                    // Handle List parameters for IN clause criteria
                    if (List.class.isAssignableFrom(paramType)) {
                        // Parse comma-separated values into a list
                        if (value.contains(",")) {
                            List<String> valueList = Arrays.stream(value.split(","))
                                    .map(String::trim)
                                    .collect(Collectors.toList());
                            params.put(paramName, valueList);
                        } else {
                            params.put(paramName, Collections.singletonList(value.trim()));
                        }
                    } else {
                        params.put(paramName, parseValue(value, paramType));
                    }
                }
                // If paramType is null, the parameter is not defined in the query - ignore it
            }
        }

        return QueryRequest.builder()
                .params(params)
                .filters(filters)
                .sorts(sorts)
                .start(start)
                .end(end)
                .metadataLevel(metadataLevel)
                .selectedFields(selectedFields)
                .build();
    }

    private void parseSimpleFilter(String attribute, List<String> values, Map<String, QueryContext.Filter> filters,
            Class<?> targetType) {
        // Skip empty filter values
        if (values == null || values.isEmpty()) {
            return;
        }

        // Filter out null and empty values
        List<String> nonEmptyValues = values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.toList());

        if (nonEmptyValues.isEmpty()) {
            return;
        }

        if (nonEmptyValues.size() == 1) {
            // Single value: use EQUALS operator
            filters.put(attribute, QueryContext.Filter.builder()
                    .attribute(attribute)
                    .operator(FilterOp.EQUALS)
                    .value(parseValue(nonEmptyValues.get(0), targetType))
                    .build());
        } else {
            // Multiple values: use IN operator
            List<Object> parsedValues = nonEmptyValues.stream()
                    .map(v -> parseValue(v, targetType))
                    .collect(Collectors.toList());

            filters.put(attribute, QueryContext.Filter.builder()
                    .attribute(attribute)
                    .operator(FilterOp.IN)
                    .values(parsedValues)
                    .build());
        }
    }

    private void parseSortParameter(String value, List<QueryContext.SortSpec> sorts) {
        // Sort can be comma-separated: sort=name.asc,age.desc
        String[] sortSpecs = value.split(",");

        for (String spec : sortSpecs) {
            spec = spec.trim();

            Matcher sortMatcher = SORT_PATTERN.matcher(spec);
            if (sortMatcher.matches()) {
                String attribute = sortMatcher.group(1);
                String direction = sortMatcher.group(2);

                sorts.add(QueryContext.SortSpec.builder()
                        .attribute(attribute)
                        .direction(SortDir.fromUrlParam(direction))
                        .build());
            } else {
                // Default to ascending if no direction specified
                sorts.add(QueryContext.SortSpec.builder()
                        .attribute(spec)
                        .direction(SortDir.ASC)
                        .build());
            }
        }
    }

    /**
     * Get the type of a parameter from the query definition
     */
    private Class<?> getParamType(QueryDefinition queryDefinition, String paramName) {
        if (queryDefinition == null) {
            return null;
        }
        ParamDef paramDef = queryDefinition.getParam(paramName);
        return paramDef != null ? paramDef.type() : null;
    }

    /**
     * Get the type of an attribute from the query definition
     */
    private Class<?> getAttributeType(QueryDefinition queryDefinition, String attributeName) {
        if (queryDefinition == null) {
            return null;
        }
        AttributeDef<?> attributeDef = queryDefinition.getAttribute(attributeName);
        return attributeDef != null ? attributeDef.getType() : null;
    }

    private Object parseValue(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // If we have a target type, try to convert to it directly
        if (targetType != null) {
            return convertToType(value, targetType);
        }

        // Try to parse as boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Try to parse as date
        try {
            if (value.length() == 10 && value.charAt(4) == '-' && value.charAt(7) == '-') {
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            // Not a date
        }

        // Try to parse as datetime
        try {
            if (value.contains("T")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            // Not a datetime
        }

        // Return as string
        return value;
    }

    /**
     * Convert a string value to the specified target type.
     * Delegates to the centralized TypeConverter utility.
     *
     * @param value      The string value to convert
     * @param targetType The target type class
     * @return The converted value
     */
    private Object convertToType(String value, Class<?> targetType) {
        if (targetType == null || value == null) {
            return value;
        }

        return TypeConverter.convertString(value, targetType);
    }
}