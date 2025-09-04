package com.balasam.oasis.common.query.validation;

import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.ParamDef;
import com.balasam.oasis.common.query.core.definition.CriteriaDef;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates query definitions for duplicates and other issues.
 * Ensures no duplicate queries, attributes, parameters, or criteria within a query.
 * Also tracks globally registered queries to prevent duplicate query names across the application.
 */
public class QueryDefinitionValidator {
    
    // Global registry to track all query names in the application
    private static final Map<String, QueryDefinition> GLOBAL_QUERY_REGISTRY = new ConcurrentHashMap<>();
    
    /**
     * Validates a query definition for duplicates and registers it globally.
     * This method should be called during query bean creation.
     * 
     * @param queryDef the query definition to validate
     * @throws IllegalStateException if duplicates are found or query name already exists
     */
    public static void validateAndRegister(QueryDefinition queryDef) {
        String queryName = queryDef.getName();
        
        // Check for duplicate query name globally
        if (GLOBAL_QUERY_REGISTRY.containsKey(queryName)) {
            throw new IllegalStateException(String.format(
                "Duplicate query definition: Query with name '%s' is already registered. " +
                "Each query must have a unique name across the application.",
                queryName
            ));
        }
        
        // Validate internal duplicates
        validateNoDuplicates(queryDef);
        
        // Validate bind parameters
        BindParameterValidator.validate(queryDef);
        
        // Register the query globally
        GLOBAL_QUERY_REGISTRY.put(queryName, queryDef);
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
                    queryDef.getName(), attr.getName()
                ));
            }
            
            // Check alias name (database column) for non-transient attributes
            if (!attr.isTransient() && attr.getAliasName() != null) {
                String aliasName = attr.getAliasName().toUpperCase(); // Case-insensitive check
                if (!aliasNames.add(aliasName)) {
                    throw new IllegalStateException(String.format(
                        "Query '%s' has duplicate attribute alias/column name: '%s'. " +
                        "Multiple attributes are mapped to the same database column.",
                        queryDef.getName(), attr.getAliasName()
                    ));
                }
            }
        }
    }
    
    /**
     * Validates that there are no duplicate parameter names.
     */
    private static void validateParameterDuplicates(QueryDefinition queryDef) {
        Map<String, ParamDef<?>> params = queryDef.getParams();
        if (params == null || params.isEmpty()) {
            return;
        }
        
        Set<String> names = new HashSet<>();
        
        for (ParamDef<?> param : params.values()) {
            if (!names.add(param.getName())) {
                throw new IllegalStateException(String.format(
                    "Query '%s' has duplicate parameter name: '%s'. " +
                    "Each parameter must have a unique name.",
                    queryDef.getName(), param.getName()
                ));
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
            if (!names.add(criterion.getName())) {
                throw new IllegalStateException(String.format(
                    "Query '%s' has duplicate criteria name: '%s'. " +
                    "Each criteria must have a unique name.",
                    queryDef.getName(), criterion.getName()
                ));
            }
            
            // Check SQL placeholder (for non-dynamic criteria)
            if (!criterion.isDynamic()) {
                String placeholder = "--" + criterion.getName();
                if (!placeholders.add(placeholder)) {
                    throw new IllegalStateException(String.format(
                        "Query '%s' has duplicate criteria placeholder: '%s'. " +
                        "This should not happen if names are unique.",
                        queryDef.getName(), placeholder
                    ));
                }
            }
        }
    }
    
    /**
     * Validates that there are no naming conflicts between attributes, parameters, and criteria.
     * While technically allowed, having the same name can cause confusion.
     */
    private static void validateCrossDefinitionNaming(QueryDefinition queryDef) {
        Set<String> allNames = new HashSet<>();
        StringBuilder conflicts = new StringBuilder();
        
        // Add attribute names
        if (queryDef.getAttributes() != null) {
            for (String attrName : queryDef.getAttributes().keySet()) {
                if (!allNames.add(attrName)) {
                    conflicts.append(String.format("- '%s' is used as both attribute and parameter/criteria\n", attrName));
                }
            }
        }
        
        // Add parameter names
        if (queryDef.getParams() != null) {
            for (String paramName : queryDef.getParams().keySet()) {
                if (!allNames.add(paramName)) {
                    conflicts.append(String.format("- '%s' is used as both parameter and attribute/criteria\n", paramName));
                }
            }
        }
        
        // Add criteria names
        if (queryDef.getCriteria() != null) {
            for (String criteriaName : queryDef.getCriteria().keySet()) {
                if (!allNames.add(criteriaName)) {
                    conflicts.append(String.format("- '%s' is used as both criteria and attribute/parameter\n", criteriaName));
                }
            }
        }
        
        // Report conflicts as warnings (not errors, since this might be intentional in some cases)
        if (conflicts.length() > 0) {
            System.out.println("WARNING: Query '" + queryDef.getName() + "' has naming conflicts:\n" + conflicts);
        }
    }
    
    /**
     * Gets the global query registry for inspection.
     * 
     * @return unmodifiable view of registered queries
     */
    public static Map<String, QueryDefinition> getRegisteredQueries() {
        return new ConcurrentHashMap<>(GLOBAL_QUERY_REGISTRY);
    }
    
    /**
     * Clears the global query registry. 
     * Should only be used in tests or when reloading the application context.
     */
    public static void clearRegistry() {
        GLOBAL_QUERY_REGISTRY.clear();
    }
    
    /**
     * Checks if a query name is already registered.
     * 
     * @param queryName the name to check
     * @return true if the name is already registered
     */
    public static boolean isQueryNameRegistered(String queryName) {
        return GLOBAL_QUERY_REGISTRY.containsKey(queryName);
    }
}