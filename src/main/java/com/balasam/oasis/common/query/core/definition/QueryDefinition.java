package com.balasam.oasis.common.query.core.definition;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.balasam.oasis.common.query.core.execution.MetadataCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Immutable query definition containing all metadata and configuration.
 * This is the central configuration object that defines a query's structure,
 * including its SQL, attributes, parameters, criteria, and processing pipeline.
 * 
 * <p>Attributes can be either regular (from database) or transient (calculated).
 * The definition is immutable after construction for thread-safety.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * QueryDefinition query = QueryDefinition.builder()
 *     .name("userQuery")
 *     .sql("SELECT * FROM users WHERE 1=1 --filters --orderBy")
 *     .attribute("id", AttributeDef.builder()...)
 *     .param("minSalary", ParamDef.builder()...)
 *     .criteria("statusFilter", CriteriaDef.builder()...)
 *     .build();
 * </pre>
 *
 * @author Query Registration System
 * @since 1.0
 * @see AttributeDef
 * @see ParamDef
 * @see CriteriaDef
 */
@Getter
@Builder(toBuilder = true)
public class QueryDefinition {
    String name;
    String sql;
    String description;
    
    @Builder.Default
    Map<String, AttributeDef<?>> attributes = ImmutableMap.of();
    
    @Builder.Default
    Map<String, ParamDef<?>> params = ImmutableMap.of();
    
    @Builder.Default
    Map<String, CriteriaDef> criteria = ImmutableMap.of();
    
    @Builder.Default
    List<Function<Object, Object>> preProcessors = ImmutableList.of();
    
    @Builder.Default
    List<Function<Object, Object>> rowProcessors = ImmutableList.of();
    
    @Builder.Default
    List<Function<Object, Object>> postProcessors = ImmutableList.of();
    
    @Builder.Default
    List<ValidationRule> validationRules = ImmutableList.of();
    
    CacheConfig cacheConfig;
    
    @Builder.Default
    int defaultPageSize = 50;
    
    @Builder.Default
    int maxPageSize = 1000;
    
    @Builder.Default
    boolean paginationEnabled = true;
    
    @Builder.Default
    boolean auditEnabled = true;
    
    @Builder.Default
    boolean metricsEnabled = true;
    
    Integer queryTimeout;  // in seconds
    
    String findByKeyCriteriaName;  // Name of the criteria used for findByKey
    
    /**
     * Cached metadata for optimized row mapping.
     * This is mutable and set after the definition is built.
     */
    @Setter
    transient MetadataCache metadataCache;
    
    /**
     * Flag to enable/disable metadata caching for this query
     */
    @Builder.Default
    boolean metadataCacheEnabled = true;
    
    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }
    
    public boolean hasParams() {
        return params != null && !params.isEmpty();
    }
    
    public boolean hasCriteria() {
        return criteria != null && !criteria.isEmpty();
    }
    
    public boolean hasPreProcessors() {
        return preProcessors != null && !preProcessors.isEmpty();
    }
    
    public boolean hasRowProcessors() {
        return rowProcessors != null && !rowProcessors.isEmpty();
    }
    
    public boolean hasPostProcessors() {
        return postProcessors != null && !postProcessors.isEmpty();
    }
    
    public boolean hasValidationRules() {
        return validationRules != null && !validationRules.isEmpty();
    }
    
    public boolean isCacheEnabled() {
        return cacheConfig != null && cacheConfig.isEnabled();
    }
    
    public AttributeDef<?> getAttribute(String name) {
        return attributes.get(name);
    }
    
    public ParamDef<?> getParam(String name) {
        return params.get(name);
    }
    
    public CriteriaDef getCriteria(String name) {
        return criteria.get(name);
    }
    
    public boolean hasFindByKey() {
        return findByKeyCriteriaName != null && criteria.containsKey(findByKeyCriteriaName);
    }
    
    public CriteriaDef getFindByKeyCriteria() {
        return findByKeyCriteriaName != null ? criteria.get(findByKeyCriteriaName) : null;
    }
    
    public boolean hasMetadataCache() {
        return metadataCache != null && metadataCache.isInitialized();
    }
    
    public boolean shouldUseMetadataCache() {
        return metadataCacheEnabled && hasMetadataCache();
    }
}