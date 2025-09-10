package com.balsam.oasis.common.registry.builder;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CacheConfig;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.processor.PostProcessor;
import com.balsam.oasis.common.registry.domain.processor.PreProcessor;
import com.balsam.oasis.common.registry.domain.processor.RowProcessor;
import com.balsam.oasis.common.registry.engine.sql.MetadataCache;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;

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
 *     .attribute(AttributeDef.builder()...)
 *     .parameter("minSalary", ParamDef.builder()...)
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
@Builder(access = AccessLevel.PROTECTED)
public class QueryDefinition {
    // Base fields from BaseDefinition
    String name;
    String description;
    String sql;
    Map<String, ParamDef> parameters;
    Map<String, CriteriaDef> criteria;
    List<PreProcessor> preProcessors;
    List<RowProcessor> rowProcessors;
    List<PostProcessor> postProcessors;
    CacheConfig cacheConfig;
    Integer defaultPageSize;
    Integer maxPageSize;
    boolean auditEnabled;
    boolean metricsEnabled;
    Integer queryTimeout;

    // Query-specific fields
    Map<String, AttributeDef<?>> attributes;
    boolean paginationEnabled;

    /**
     * Fetch size for JDBC ResultSet processing.
     * Controls how many rows are fetched from database in each round trip.
     * -1 means use system default, 0 means fetch all, positive means fetch that
     * many rows.
     */
    Integer fetchSize;

    /**
     * Cached metadata for optimized row mapping.
     * This field is transient and can be set via withMetadataCache method.
     */
    transient MetadataCache metadataCache;

    /**
     * Flag to include dynamic attributes (columns not defined in AttributeDef)
     */
    boolean includeDynamicAttributes;

    /**
     * Naming strategy for dynamic attributes
     */
    NamingStrategy namingStrategy;

    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    public AttributeDef<?> getAttribute(String name) {
        // Simply return from attributes map since dynamic attributes are now
        // pre-registered
        return attributes.get(name);
    }

    public ParamDef getParam(String name) {
        return parameters.get(name);
    }

    // Methods from BaseDefinition
    public boolean hasParams() {
        return parameters != null && !parameters.isEmpty();
    }

    public boolean hasCriteria() {
        return criteria != null && !criteria.isEmpty();
    }

    public boolean hasCacheConfig() {
        return cacheConfig != null && cacheConfig.isEnabled();
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
                .parameters(this.parameters)
                .criteria(this.criteria)
                .preProcessors(this.preProcessors)
                .rowProcessors(this.rowProcessors)
                .postProcessors(this.postProcessors)
                .cacheConfig(this.cacheConfig)
                .defaultPageSize(this.defaultPageSize)
                .maxPageSize(this.maxPageSize)
                .auditEnabled(this.auditEnabled)
                .metricsEnabled(this.metricsEnabled)
                .queryTimeout(this.queryTimeout)
                .attributes(this.attributes)
                .paginationEnabled(this.paginationEnabled)
                .fetchSize(this.fetchSize)
                .metadataCache(cache)
                .includeDynamicAttributes(this.includeDynamicAttributes)
                .namingStrategy(this.namingStrategy)
                .build();
    }

    /**
     * Returns a new instance with the attributes set.
     * Since this object is immutable, we create a new instance.
     */
    public QueryDefinition withAttributes(Map<String, AttributeDef<?>> newAttributes) {
        return QueryDefinition.builder()
                .name(this.name)
                .description(this.description)
                .sql(this.sql)
                .parameters(this.parameters)
                .criteria(this.criteria)
                .preProcessors(this.preProcessors)
                .rowProcessors(this.rowProcessors)
                .postProcessors(this.postProcessors)
                .cacheConfig(this.cacheConfig)
                .defaultPageSize(this.defaultPageSize)
                .maxPageSize(this.maxPageSize)
                .auditEnabled(this.auditEnabled)
                .metricsEnabled(this.metricsEnabled)
                .queryTimeout(this.queryTimeout)
                .attributes(newAttributes)
                .paginationEnabled(this.paginationEnabled)
                .fetchSize(this.fetchSize)
                .metadataCache(this.metadataCache)
                .includeDynamicAttributes(this.includeDynamicAttributes)
                .namingStrategy(this.namingStrategy)
                .build();
    }
}