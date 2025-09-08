package com.balsam.oasis.common.registry.domain.definition;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.base.BaseDefinition;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.engine.metadata.MetadataCache;
import com.google.common.collect.ImmutableMap;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
public class QueryDefinition extends BaseDefinition {
    // Query-specific fields
    @Builder.Default
    Map<String, AttributeDef<?>> attributes = ImmutableMap.of();

    @Builder.Default
    boolean paginationEnabled = true;

    String findByKeyCriteriaName; // Name of the criteria used for findByKey

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

    /**
     * Flag to include dynamic attributes (columns not defined in AttributeDef)
     */
    @Builder.Default
    boolean includeDynamicAttributes = false;

    /**
     * Naming strategy for dynamic attributes
     */
    @Builder.Default
    NamingStrategy dynamicAttributeNamingStrategy = NamingStrategy.AS_IS;

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
}