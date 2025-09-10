package com.balsam.oasis.common.registry.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;

/**
 * Validates query definitions for duplicates and other issues.
 * Ensures no duplicate attributes, parameters, or criteria within a query.
 * This is a stateless utility class for validation only.
 */
public class QueryDefinitionValidator {

    /**
     * Private constructor to prevent instantiation
     */
    private QueryDefinitionValidator() {
        // Utility class
    }

    /**
     * Validates that there are no duplicate definitions within the query.
     * 
     * @param queryDef the query definition to validate
     * @throws IllegalStateException if duplicates are found
     */
    public static void validateNoDuplicates(QueryDefinition queryDef) {
        String queryName = queryDef.getName();

        // Check for duplicate attribute names
        validateAttributeDuplicates(queryDef);

        // Check for duplicate parameter names
        validateParameterDuplicates(queryDef);

        // Check for duplicate criteria names
        validateCriteriaDuplicates(queryDef);

        // Check for naming conflicts between attributes, params, and criteria
        validateCrossDefinitionNaming(queryDef);
    }

    /**
     * Validates that there are no duplicate attribute names.
     */
    private static void validateAttributeDuplicates(QueryDefinition queryDef) {
        Map<String, AttributeDef<?>> attributes = queryDef.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        Set<String> names = new HashSet<>();
        Set<String> aliasNames = new HashSet<>();

        for (AttributeDef<?> attr : attributes.values()) {
            // Check attribute name
            if (!names.add(attr.getName())) {
                throw new IllegalStateException(String.format(
                        "Query '%s' has duplicate attribute name: '%s'. " +
                                "Each attribute must have a unique name.",
                        queryDef.getName(), attr.getName()));
            }

            // Check alias name (database column) for non-transient attributes
            if (!attr.isVirual() && attr.getAliasName() != null) {
                String aliasName = attr.getAliasName().toUpperCase(); // Case-insensitive check
                if (!aliasNames.add(aliasName)) {
                    throw new IllegalStateException(String.format(
                            "Query '%s' has duplicate attribute alias/column name: '%s'. " +
                                    "Multiple attributes are mapped to the same database column.",
                            queryDef.getName(), attr.getAliasName()));
                }
            }
        }
    }

    /**
     * Validates that there are no duplicate parameter names.
     */
    private static void validateParameterDuplicates(QueryDefinition queryDef) {
        Map<String, ParamDef> params = queryDef.getParameters();
        if (params == null || params.isEmpty()) {
            return;
        }

        Set<String> names = new HashSet<>();

        for (ParamDef param : params.values()) {
            if (!names.add(param.name())) {
                throw new IllegalStateException(String.format(
                        "Query '%s' has duplicate parameter name: '%s'. " +
                                "Each parameter must have a unique name.",
                        queryDef.getName(), param.name()));
            }
        }
    }

    /**
     * Validates that there are no duplicate criteria names.
     */
    private static void validateCriteriaDuplicates(QueryDefinition queryDef) {
        Map<String, CriteriaDef> criteria = queryDef.getCriteria();
        if (criteria == null || criteria.isEmpty()) {
            return;
        }

        Set<String> names = new HashSet<>();
        Set<String> placeholders = new HashSet<>();

        for (CriteriaDef criterion : criteria.values()) {
            // Check criteria name
            if (!names.add(criterion.name())) {
                throw new IllegalStateException(String.format(
                        "Query '%s' has duplicate criteria name: '%s'. " +
                                "Each criteria must have a unique name.",
                        queryDef.getName(), criterion.name()));
            }

            // Check SQL placeholder (for non-dynamic criteria)
            String placeholder = "--" + criterion.name();
            if (!placeholders.add(placeholder)) {
                throw new IllegalStateException(String.format(
                        "Query '%s' has duplicate criteria placeholder: '%s'. " +
                                "This should not happen if names are unique.",
                        queryDef.getName(), placeholder));
            }
        }
    }

    /**
     * Validates that there are no naming conflicts between attributes, parameters,
     * and criteria.
     */
    private static void validateCrossDefinitionNaming(QueryDefinition queryDef) {
        Set<String> allNames = new HashSet<>();

        // Add all attribute names
        if (queryDef.getAttributes() != null) {
            for (String attrName : queryDef.getAttributes().keySet()) {
                if (!allNames.add(attrName)) {
                    throw new IllegalStateException(String.format(
                            "Query '%s' has naming conflict: '%s' is used in multiple definitions.",
                            queryDef.getName(), attrName));
                }
            }
        }

        // Check against parameter names
        if (queryDef.getParameters() != null) {
            for (String paramName : queryDef.getParameters().keySet()) {
                if (!allNames.add(paramName)) {
                    throw new IllegalStateException(String.format(
                            "Query '%s' has naming conflict: " +
                                    "parameter '%s' has the same name as an attribute.",
                            queryDef.getName(), paramName));
                }
            }
        }

        // Check against criteria names
        if (queryDef.getCriteria() != null) {
            for (String criteriaName : queryDef.getCriteria().keySet()) {
                if (!allNames.add(criteriaName)) {
                    throw new IllegalStateException(String.format(
                            "Query '%s' has naming conflict: " +
                                    "criteria '%s' has the same name as an attribute or parameter.",
                            queryDef.getName(), criteriaName));
                }
            }
        }
    }
}