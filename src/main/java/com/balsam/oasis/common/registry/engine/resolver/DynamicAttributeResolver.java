package com.balsam.oasis.common.registry.engine.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.engine.metadata.MetadataCache;

/**
 * Resolves dynamic attributes from database metadata.
 * 
 * <p>This resolver handles queries configured with .dynamic() by creating
 * AttributeDef objects on-demand for columns discovered at runtime.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Uses MetadataCache for efficient column discovery</li>
 *   <li>Applies NamingStrategy for consistent attribute naming</li>
 *   <li>Caches resolved attributes for performance</li>
 *   <li>Makes dynamic attributes filterable and sortable by default</li>
 * </ul>
 */
public class DynamicAttributeResolver {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicAttributeResolver.class);
    
    // Cache resolved dynamic attributes per query
    private final Map<String, Map<String, AttributeDef<?>>> resolvedAttributesCache = new ConcurrentHashMap<>();
    
    /**
     * Resolve a dynamic attribute by name for a given query definition.
     * 
     * @param queryDef The query definition
     * @param attributeName The attribute name to resolve (after naming strategy conversion)
     * @return The resolved AttributeDef, or null if not found
     */
    public AttributeDef<?> resolveDynamicAttribute(QueryDefinition queryDef, String attributeName) {
        if (queryDef == null || attributeName == null) {
            return null;
        }
        
        // Only resolve for queries with dynamic attributes enabled
        if (!queryDef.isIncludeDynamicAttributes()) {
            return null;
        }
        
        // Check if already resolved and cached
        Map<String, AttributeDef<?>> cachedAttributes = resolvedAttributesCache.get(queryDef.getName());
        if (cachedAttributes != null && cachedAttributes.containsKey(attributeName)) {
            return cachedAttributes.get(attributeName);
        }
        
        // Try to resolve from metadata cache
        MetadataCache metadataCache = queryDef.getMetadataCache();
        if (metadataCache == null || !metadataCache.isInitialized()) {
            log.debug("No metadata cache available for query: {}", queryDef.getName());
            return null;
        }
        
        // Get naming strategy
        NamingStrategy namingStrategy = queryDef.getDynamicAttributeNamingStrategy();
        if (namingStrategy == null) {
            namingStrategy = NamingStrategy.CAMEL; // Default to camel case
        }
        
        // Try to find matching column
        String matchingColumn = findMatchingColumn(metadataCache, attributeName, namingStrategy);
        if (matchingColumn == null) {
            log.debug("No matching column found for attribute: {} in query: {}", 
                     attributeName, queryDef.getName());
            return null;
        }
        
        // Create dynamic AttributeDef
        AttributeDef<?> dynamicAttr = createDynamicAttribute(attributeName, matchingColumn);
        
        // Cache the resolved attribute
        cacheResolvedAttribute(queryDef.getName(), attributeName, dynamicAttr);
        
        log.debug("Resolved dynamic attribute: {} -> column: {} for query: {}", 
                 attributeName, matchingColumn, queryDef.getName());
        
        return dynamicAttr;
    }
    
    /**
     * Resolve all dynamic attributes for a query.
     * This pre-populates the cache with all available dynamic attributes.
     * 
     * @param queryDef The query definition
     * @return Map of attribute name to AttributeDef
     */
    public Map<String, AttributeDef<?>> resolveAllDynamicAttributes(QueryDefinition queryDef) {
        if (queryDef == null || !queryDef.isIncludeDynamicAttributes()) {
            return new HashMap<>();
        }
        
        // Check cache first
        Map<String, AttributeDef<?>> cached = resolvedAttributesCache.get(queryDef.getName());
        if (cached != null) {
            return new HashMap<>(cached);
        }
        
        Map<String, AttributeDef<?>> dynamicAttributes = new HashMap<>();
        
        MetadataCache metadataCache = queryDef.getMetadataCache();
        if (metadataCache == null || !metadataCache.isInitialized()) {
            return dynamicAttributes;
        }
        
        NamingStrategy namingStrategy = queryDef.getDynamicAttributeNamingStrategy();
        if (namingStrategy == null) {
            namingStrategy = NamingStrategy.CAMEL;
        }
        
        // Get all columns from metadata
        String[] columnNames = metadataCache.getColumnNames();
        if (columnNames != null) {
            for (String columnName : columnNames) {
                // Skip if already defined as static attribute
                if (isStaticallyDefined(queryDef, columnName)) {
                    continue;
                }
                
                // Apply naming strategy
                String attributeName = namingStrategy.convert(columnName);
                
                // Create dynamic attribute
                AttributeDef<?> dynamicAttr = createDynamicAttribute(attributeName, columnName);
                dynamicAttributes.put(attributeName, dynamicAttr);
            }
        }
        
        // Cache all resolved attributes
        if (!dynamicAttributes.isEmpty()) {
            resolvedAttributesCache.put(queryDef.getName(), new HashMap<>(dynamicAttributes));
        }
        
        return dynamicAttributes;
    }
    
    /**
     * Clear cache for a specific query.
     */
    public void clearCache(String queryName) {
        resolvedAttributesCache.remove(queryName);
    }
    
    /**
     * Clear entire cache.
     */
    public void clearAllCache() {
        resolvedAttributesCache.clear();
    }
    
    /**
     * Find a column that matches the given attribute name after naming strategy conversion.
     */
    private String findMatchingColumn(MetadataCache metadataCache, String attributeName, 
                                     NamingStrategy namingStrategy) {
        String[] columnNames = metadataCache.getColumnNames();
        if (columnNames == null) {
            return null;
        }
        
        // Try exact match first (case-insensitive)
        for (String columnName : columnNames) {
            String convertedName = namingStrategy.convert(columnName);
            if (convertedName.equalsIgnoreCase(attributeName)) {
                return columnName;
            }
        }
        
        // Try partial match for flexibility
        String lowerAttrName = attributeName.toLowerCase();
        for (String columnName : columnNames) {
            String convertedName = namingStrategy.convert(columnName);
            if (convertedName.toLowerCase().equals(lowerAttrName)) {
                return columnName;
            }
        }
        
        return null;
    }
    
    /**
     * Check if a column is already defined as a static attribute.
     */
    private boolean isStaticallyDefined(QueryDefinition queryDef, String columnName) {
        if (queryDef.getAttributes() == null) {
            return false;
        }
        
        String lowerColumnName = columnName.toLowerCase();
        
        for (AttributeDef<?> attr : queryDef.getAttributes().values()) {
            // Check alias name (database column)
            if (attr.getAliasName() != null && 
                attr.getAliasName().toLowerCase().equals(lowerColumnName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Create a dynamic AttributeDef for a discovered column.
     * Dynamic attributes are filterable and sortable by default.
     * We use String type as a safe default that works with most conversions.
     */
    private AttributeDef<?> createDynamicAttribute(String attributeName, String columnName) {
        // Determine type based on column name patterns (simple heuristic)
        Class<?> type = inferTypeFromColumnName(columnName);
        
        return AttributeDef.name(attributeName)
            .aliasName(columnName)
            .type(type)          // Use inferred type
            .filterable(true)    // Allow filtering by default
            .sortable(true)      // Allow sorting by default
            .selected(true)      // Include in results by default
            .build();
    }
    
    /**
     * Infer type from column name using common patterns.
     * This is a simple heuristic - a more robust solution would use
     * actual database metadata.
     */
    private Class<?> inferTypeFromColumnName(String columnName) {
        String lowerName = columnName.toLowerCase();
        
        // Common ID patterns - usually integers
        if (lowerName.endsWith("_id") || lowerName.equals("id") || 
            lowerName.contains("employee_id") || lowerName.contains("department_id") ||
            lowerName.contains("manager_id")) {
            return Integer.class;
        }
        
        // Date patterns
        if (lowerName.contains("date") || lowerName.contains("_date") ||
            lowerName.contains("hire_date") || lowerName.contains("created") ||
            lowerName.contains("updated")) {
            return java.time.LocalDate.class;
        }
        
        // Numeric patterns
        if (lowerName.contains("salary") || lowerName.contains("amount") ||
            lowerName.contains("price") || lowerName.contains("commission") ||
            lowerName.contains("pct") || lowerName.contains("rate")) {
            return java.math.BigDecimal.class;
        }
        
        if (lowerName.contains("count") || lowerName.contains("qty") ||
            lowerName.contains("quantity")) {
            return Integer.class;
        }
        
        // Boolean patterns
        if (lowerName.startsWith("is_") || lowerName.startsWith("has_") ||
            lowerName.contains("active") || lowerName.contains("enabled")) {
            return Boolean.class;
        }
        
        // Default to String for everything else
        return String.class;
    }
    
    /**
     * Cache a resolved attribute.
     */
    private void cacheResolvedAttribute(String queryName, String attributeName, 
                                       AttributeDef<?> attribute) {
        resolvedAttributesCache.computeIfAbsent(queryName, k -> new ConcurrentHashMap<>())
                              .put(attributeName, attribute);
    }
}