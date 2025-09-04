package com.balasam.oasis.common.query.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.balasam.oasis.common.query.core.definition.CriteriaDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;

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
    public static void validate(QueryDefinition queryDef) {
        String queryName = queryDef.getName();

        // Extract all bind parameters from main SQL
        Set<String> sqlBindParams = extractBindParameters(queryDef.getSql());

        // Extract all bind parameters from criteria SQL
        Set<String> criteriaBindParams = new HashSet<>();
        if (queryDef.getCriteria() != null) {
            for (Map.Entry<String, CriteriaDef> entry : queryDef.getCriteria().entrySet()) {
                CriteriaDef criteria = entry.getValue();
                if (criteria.getSql() != null) {
                    Set<String> params = extractBindParameters(criteria.getSql());
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
        if (queryDef.getParams() != null) {
            definedParams.addAll(queryDef.getParams().keySet());
        }

        // Special handling for pagination parameters (these are system-provided)
        Set<String> systemParams = Set.of(
                "offset", "limit", // Standard pagination
                "startRow", "endRow", // Oracle pagination
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
            String errorMsg = String.format(
                    "Query '%s' uses undefined bind parameters: %s\n" +
                            "Defined parameters: %s\n" +
                            "Used in SQL: %s\n" +
                            "Used in criteria: %s\n" +
                            "Make sure all :paramName references have corresponding ParamDef definitions.",
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

        if (!unusedParams.isEmpty()) {
            System.out.println("WARNING: Query '" + queryName + "' has defined but unused parameters: " + unusedParams);
        }
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

    /**
     * Validates all bind parameters for a query and returns detailed information.
     * 
     * @param queryDef the query definition
     * @return validation result with details
     */
    public static ValidationResult validateWithDetails(QueryDefinition queryDef) {
        try {
            validate(queryDef);
            return ValidationResult.success(queryDef.getName());
        } catch (IllegalStateException e) {
            return ValidationResult.failure(queryDef.getName(), e.getMessage());
        }
    }

    /**
     * Result of bind parameter validation.
     */
    public static class ValidationResult {
        private final String queryName;
        private final boolean valid;
        private final String errorMessage;
        private final Set<String> undefinedParams;
        private final Set<String> unusedParams;

        private ValidationResult(String queryName, boolean valid, String errorMessage,
                Set<String> undefinedParams, Set<String> unusedParams) {
            this.queryName = queryName;
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.undefinedParams = undefinedParams;
            this.unusedParams = unusedParams;
        }

        public static ValidationResult success(String queryName) {
            return new ValidationResult(queryName, true, null, Set.of(), Set.of());
        }

        public static ValidationResult failure(String queryName, String errorMessage) {
            return new ValidationResult(queryName, false, errorMessage, Set.of(), Set.of());
        }

        public String getQueryName() {
            return queryName;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Set<String> getUndefinedParams() {
            return undefinedParams;
        }

        public Set<String> getUnusedParams() {
            return unusedParams;
        }
    }
}