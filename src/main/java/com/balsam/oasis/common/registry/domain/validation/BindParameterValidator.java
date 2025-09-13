package com.balsam.oasis.common.registry.domain.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;

/**
 * Validates that all bind parameters in SQL queries and criteria are defined in
 * the query definition.
 * This ensures type safety and prevents runtime SQL errors.
 */
public class BindParameterValidator {

    // Pattern to match named parameters in SQL (e.g., :paramName or :param_name)
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");

    /**
     * Validates that all bind parameters used in the query are defined.
     * 
     * @param queryDef the query definition to validate
     * @throws IllegalStateException if undefined parameters are found
     */
    public static void validate(QueryDefinitionBuilder queryDef) {
        String queryName = queryDef.getName();

        // Extract all bind parameters from main SQL
        Set<String> sqlBindParams = extractBindParameters(queryDef.getSql());

        // Extract all bind parameters from criteria SQL
        Set<String> criteriaBindParams = new HashSet<>();
        if (queryDef.getCriteria() != null) {
            for (Map.Entry<String, CriteriaDef> entry : queryDef.getCriteria().entrySet()) {
                CriteriaDef criteria = entry.getValue();
                if (criteria.sql() != null) {
                    Set<String> params = extractBindParameters(criteria.sql());
                    criteriaBindParams.addAll(params);
                }
            }
        }

        // Combine all bind parameters
        Set<String> allBindParams = new HashSet<>();
        allBindParams.addAll(sqlBindParams);
        allBindParams.addAll(criteriaBindParams);

        // Get all defined parameters
        Set<String> definedParams = new HashSet<>();
        if (queryDef.getParameters() != null) {
            definedParams.addAll(queryDef.getParameters().keySet());
        }

        // Special handling for pagination parameters (these are system-provided)
        Set<String> systemParams = Set.of(
                "offset", "limit", // Standard pagination
                "_start", "_end" // REST API pagination
        );

        // Find undefined parameters
        Set<String> undefinedParams = new HashSet<>(allBindParams);
        undefinedParams.removeAll(definedParams);
        undefinedParams.removeAll(systemParams);

        // Check if undefined params might be filter-generated
        // Filters generate params like: attributeName_op (e.g., salary_gte)
        Set<String> filterGeneratedParams = new HashSet<>();
        if (queryDef.getAttributes() != null) {
            for (String attrName : queryDef.getAttributes().keySet()) {
                // Common filter operators that generate params
                filterGeneratedParams.add(attrName);
                filterGeneratedParams.add(attrName + "_eq");
                filterGeneratedParams.add(attrName + "_ne");
                filterGeneratedParams.add(attrName + "_gt");
                filterGeneratedParams.add(attrName + "_gte");
                filterGeneratedParams.add(attrName + "_lt");
                filterGeneratedParams.add(attrName + "_lte");
                filterGeneratedParams.add(attrName + "_like");
                filterGeneratedParams.add(attrName + "_in");
                filterGeneratedParams.add(attrName + "_between_1");
                filterGeneratedParams.add(attrName + "_between_2");
                filterGeneratedParams.add(attrName + "_null");
                filterGeneratedParams.add(attrName + "_notnull");
            }
        }

        // Remove filter-generated params from undefined
        undefinedParams.removeAll(filterGeneratedParams);

        // Report errors if there are still undefined parameters
        if (!undefinedParams.isEmpty()) {
            String errorMsg = String.format("""
                    Query '%s' uses undefined bind parameters: %s
                    Defined parameters: %s
                    Used in SQL: %s
                    Used in criteria: %s
                    Make sure all :paramName references have corresponding ParamDef definitions.""",
                    queryName,
                    undefinedParams,
                    definedParams,
                    sqlBindParams,
                    criteriaBindParams);
            throw new IllegalStateException(errorMsg);
        }

        // Also validate that no parameters are defined but never used (warning only)
        Set<String> unusedParams = new HashSet<>(definedParams);
        unusedParams.removeAll(allBindParams);
        unusedParams.removeAll(systemParams);

        // Unused parameters will be returned for logging in the caller
        // Not logged here to allow consolidated logging
    }

    /**
     * Finds unused parameters in the query definition.
     * 
     * @param queryDef the query definition to check
     * @return set of unused parameter names, empty if none
     */
    public static Set<String> findUnusedParameters(QueryDefinitionBuilder queryDef) {
        Set<String> sqlBindParams = extractBindParameters(queryDef.getSql());
        
        // Extract all bind parameters from criteria SQL
        Set<String> criteriaBindParams = new HashSet<>();
        if (queryDef.getCriteria() != null) {
            for (Map.Entry<String, CriteriaDef> entry : queryDef.getCriteria().entrySet()) {
                CriteriaDef criteria = entry.getValue();
                if (criteria.sql() != null) {
                    Set<String> params = extractBindParameters(criteria.sql());
                    criteriaBindParams.addAll(params);
                }
            }
        }
        
        // Combine all bind parameters
        Set<String> allBindParams = new HashSet<>();
        allBindParams.addAll(sqlBindParams);
        allBindParams.addAll(criteriaBindParams);
        
        // Get all defined parameters
        Set<String> definedParams = new HashSet<>();
        if (queryDef.getParameters() != null) {
            definedParams.addAll(queryDef.getParameters().keySet());
        }
        
        // Special handling for pagination parameters (these are system-provided)
        Set<String> systemParams = Set.of(
                "offset", "limit", // Standard pagination
                "_start", "_end" // REST API pagination
        );
        
        // Find unused parameters
        Set<String> unusedParams = new HashSet<>(definedParams);
        unusedParams.removeAll(allBindParams);
        unusedParams.removeAll(systemParams);
        
        return unusedParams;
    }
    
    /**
     * Extracts all bind parameter names from SQL text.
     * 
     * @param sql the SQL text
     * @return set of parameter names (without the : prefix)
     */
    public static Set<String> extractBindParameters(String sql) {
        Set<String> params = new HashSet<>();
        if (sql == null || sql.isEmpty()) {
            return params;
        }

        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }

        return params;
    }
}