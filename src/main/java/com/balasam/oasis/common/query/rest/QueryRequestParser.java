package com.balasam.oasis.common.query.rest;

import com.balasam.oasis.common.query.core.definition.FilterOp;
import com.balasam.oasis.common.query.core.definition.SortDir;
import com.balasam.oasis.common.query.core.execution.QueryContext;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses HTTP request parameters into query execution parameters
 */
public class QueryRequestParser {
    
    private static final Pattern PARAM_PATTERN = Pattern.compile("^param\\.(.+)$");
    private static final Pattern KEY_PATTERN = Pattern.compile("^key\\.(.+)$");
    private static final Pattern FILTER_PATTERN = Pattern.compile("^filter\\.(.+?)(?:\\.(\\w+))?$");
    private static final Pattern SORT_PATTERN = Pattern.compile("^([^.]+)\\.(asc|desc)$");
    
    public QueryRequest parse(MultiValueMap<String, String> allParams, int start, int end, String metadataLevel) {
        Map<String, Object> params = new HashMap<>();
        Map<String, QueryContext.Filter> filters = new LinkedHashMap<>();
        List<QueryContext.SortSpec> sorts = new ArrayList<>();
        
        // Parse each parameter
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            
            if (values == null || values.isEmpty()) {
                continue;
            }
            
            String value = values.get(0); // Take first value for single-valued params
            
            // Skip pagination and format parameters
            if (key.startsWith("_")) {
                continue;
            }
            
            // Check for parameter pattern: param.name
            Matcher paramMatcher = PARAM_PATTERN.matcher(key);
            if (paramMatcher.matches()) {
                String paramName = paramMatcher.group(1);
                params.put(paramName, parseValue(value));
                continue;
            }
            
            // Check for key pattern: key.name (for findByKey queries)
            Matcher keyMatcher = KEY_PATTERN.matcher(key);
            if (keyMatcher.matches()) {
                String keyParamName = keyMatcher.group(1);
                params.put(keyParamName, parseValue(value));
                continue;
            }
            
            // Check for filter pattern: filter.attribute or filter.attribute.op
            Matcher filterMatcher = FILTER_PATTERN.matcher(key);
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
                        filterValue = parseValue(allParams.getFirst(valueKey));
                    }
                    if (allParams.containsKey(valueKey2)) {
                        filterValue2 = parseValue(allParams.getFirst(valueKey2));
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
                        filters.put(attribute, QueryContext.Filter.builder()
                            .attribute(attribute)
                            .operator(op)
                            .value(parseValue(value))
                            .build());
                    } catch (IllegalArgumentException e) {
                        // Not a valid operator, treat as simple filter
                        parseSimpleFilter(attribute, value, filters);
                    }
                } else {
                    // Simple filter: filter.attribute=value
                    parseSimpleFilter(attribute, value, filters);
                }
                continue;
            }
            
            // Check for sort pattern
            if (key.equals("sort")) {
                parseSortParameter(value, sorts);
            }
        }
        
        return QueryRequest.builder()
            .params(params)
            .filters(filters)
            .sorts(sorts)
            .start(start)
            .end(end)
            .metadataLevel(metadataLevel)
            .build();
    }
    
    private void parseSimpleFilter(String attribute, String value, Map<String, QueryContext.Filter> filters) {
        // Check for comma-separated values (IN operator)
        if (value.contains(",")) {
            List<Object> values = Arrays.stream(value.split(","))
                .map(String::trim)
                .map(this::parseValue)
                .collect(Collectors.toList());
            
            filters.put(attribute, QueryContext.Filter.builder()
                .attribute(attribute)
                .operator(FilterOp.IN)
                .values(values)
                .build());
        } else {
            // Simple equals filter
            filters.put(attribute, QueryContext.Filter.builder()
                .attribute(attribute)
                .operator(FilterOp.EQUALS)
                .value(parseValue(value))
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
    
    private Object parseValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
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
}