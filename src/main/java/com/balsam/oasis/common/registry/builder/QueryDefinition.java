package com.balsam.oasis.common.registry.builder;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CacheConfig;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.definition.ValidationRule;
import com.balsam.oasis.common.registry.engine.metadata.MetadataCache;
import com.balsam.oasis.common.registry.processor.PostProcessor;
import com.balsam.oasis.common.registry.processor.PreProcessor;
import com.balsam.oasis.common.registry.processor.RowProcessor;
import com.google.common.collect.ImmutableMap;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.Builder;

/**
 * Immutable query definition containing all metadata and configuration.
 * This is the central configuration object that defines a query's structure,
 * including its SQL, attributes, parameters, criteria, and processing pipeline.
 * 
 * <p>
 * Attributes can be either regular (from database) or transient (calculated).
 * The definition is immutable after construction for thread-safety.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * QueryDefinition query = QueryDefinition.builder()
 *     .name("userQuery")
 *     .sql("SELECT * FROM users WHERE 1=1 --statusFilter")
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
@Value
@Builder
public class QueryDefinition {
    // Base fields from BaseDefinition
    String name;
    String description;
    String sql;
    Map<String, ParamDef<?>> params;
    Map<String, CriteriaDef> criteria;
    List<PreProcessor> preProcessors;
    List<RowProcessor> rowProcessors;
    List<PostProcessor> postProcessors;
    List<ValidationRule> validationRules;
    CacheConfig cacheConfig;
    int defaultPageSize;
    int maxPageSize;
    boolean auditEnabled;
    boolean metricsEnabled;
    Integer queryTimeout;

    // Query-specific fields
    Map<String, AttributeDef<?>> attributes;
    boolean paginationEnabled;

    String findByKeyCriteriaName; // Name of the criteria used for findByKey

    /**
     * Cached metadata for optimized row mapping.
     * This field is transient and can be set via withMetadataCache method.
     */
    transient MetadataCache metadataCache;

    /**
     * Flag to enable/disable metadata caching for this query
     */
    boolean metadataCacheEnabled;

    /**
     * Flag to include dynamic attributes (columns not defined in AttributeDef)
     */
    boolean includeDynamicAttributes;

    /**
     * Naming strategy for dynamic attributes
     */
    NamingStrategy dynamicAttributeNamingStrategy;

    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
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

    // Methods from BaseDefinition
    public boolean hasParams() {
        return params != null && !params.isEmpty();
    }

    public boolean hasCriteria() {
        return criteria != null && !criteria.isEmpty();
    }

    public boolean hasCacheConfig() {
        return cacheConfig != null && cacheConfig.isEnabled();
    }

    public boolean hasValidationRules() {
        return validationRules != null && !validationRules.isEmpty();
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
    
    /**
     * Returns a new instance with the metadata cache set.
     * Since this object is immutable, we create a new instance.
     */
    public QueryDefinition withMetadataCache(MetadataCache cache) {
        return QueryDefinition.builder()
                .name(this.name)
                .description(this.description)
                .sql(this.sql)
                .params(this.params)
                .criteria(this.criteria)
                .preProcessors(this.preProcessors)
                .rowProcessors(this.rowProcessors)
                .postProcessors(this.postProcessors)
                .validationRules(this.validationRules)
                .cacheConfig(this.cacheConfig)
                .defaultPageSize(this.defaultPageSize)
                .maxPageSize(this.maxPageSize)
                .auditEnabled(this.auditEnabled)
                .metricsEnabled(this.metricsEnabled)
                .queryTimeout(this.queryTimeout)
                .attributes(this.attributes)
                .paginationEnabled(this.paginationEnabled)
                .findByKeyCriteriaName(this.findByKeyCriteriaName)
                .metadataCache(cache)
                .metadataCacheEnabled(this.metadataCacheEnabled)
                .includeDynamicAttributes(this.includeDynamicAttributes)
                .dynamicAttributeNamingStrategy(this.dynamicAttributeNamingStrategy)
                .build();
    }
}