package com.balsam.oasis.common.registry.builder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CacheConfig;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.processor.PostProcessor;
import com.balsam.oasis.common.registry.domain.processor.PreProcessor;
import com.balsam.oasis.common.registry.domain.processor.RowProcessor;
import com.balsam.oasis.common.registry.domain.validation.BindParameterValidator;
import com.balsam.oasis.common.registry.engine.sql.MetadataCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
 * QueryDefinition query = QueryDefinition.builder("userQuery")
 *     .sql("SELECT * FROM users WHERE 1=1 --statusFilter")
 *     .attribute(AttributeDef.builder()...)
 *     .parameter(ParamDef.builder()...)
 *     .criteria(CriteriaDef.builder()...)
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryDefinitionBuilder {
    // Base fields from BaseDefinition
    private final String name;
    private final String description;
    private final String sql;
    private final Map<String, ParamDef<?>> parameters;
    private final Map<String, CriteriaDef> criteria;
    private final List<PreProcessor> preProcessors;
    private final List<RowProcessor> rowProcessors;
    private final List<PostProcessor> postProcessors;
    private final CacheConfig cacheConfig;
    private final Integer defaultPageSize;
    private final Integer maxPageSize;
    private final boolean auditEnabled;
    private final boolean metricsEnabled;
    private final Integer queryTimeout;

    // Query-specific fields
    private final Map<String, AttributeDef<?>> attributes;
    private final boolean paginationEnabled;

    /**
     * Fetch size for JDBC ResultSet processing.
     * Controls how many rows are fetched from database in each round trip.
     * -1 means use system default, 0 means fetch all, positive means fetch that
     * many rows.
     */
    private final Integer fetchSize;

    /**
     * Cached metadata for optimized row mapping.
     * This field is transient and can be set via withMetadataCache method.
     */
    private transient MetadataCache metadataCache;

    /**
     * Flag to include dynamic attributes (columns not defined in AttributeDef)
     */
    private final boolean dynamic;

    /**
     * Naming strategy for dynamic attributes
     */
    private final NamingStrategy namingStrategy;

    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    public AttributeDef<?> getAttribute(String name) {
        // Simply return from attributes map since dynamic attributes are now
        // pre-registered
        return attributes.get(name);
    }

    public ParamDef<?> getParam(String name) {
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
    public QueryDefinitionBuilder withMetadataCache(MetadataCache cache) {
        return new QueryDefinitionBuilder(
                this.name,
                this.description,
                this.sql,
                this.parameters,
                this.criteria,
                this.preProcessors,
                this.rowProcessors,
                this.postProcessors,
                this.cacheConfig,
                this.defaultPageSize,
                this.maxPageSize,
                this.auditEnabled,
                this.metricsEnabled,
                this.queryTimeout,
                this.attributes,
                this.paginationEnabled,
                this.fetchSize,
                cache,
                this.dynamic,
                this.namingStrategy);
    }

    /**
     * Returns a new instance with both metadata cache and attributes set in one
     * operation.
     * This is more efficient than calling withMetadataCache and withAttributes
     * separately.
     * Since this object is immutable, we create a new instance.
     * 
     * @param cache         The metadata cache to set (can be null)
     * @param newAttributes The attributes to set (if null, existing attributes are
     *                      used)
     * @return A new QueryDefinitionBuilder instance with both cache and attributes
     *         set
     */
    public QueryDefinitionBuilder withCacheAndAttributes(MetadataCache cache,
            Map<String, AttributeDef<?>> newAttributes) {
        return new QueryDefinitionBuilder(
                this.name,
                this.description,
                this.sql,
                this.parameters,
                this.criteria,
                this.preProcessors,
                this.rowProcessors,
                this.postProcessors,
                this.cacheConfig,
                this.defaultPageSize,
                this.maxPageSize,
                this.auditEnabled,
                this.metricsEnabled,
                this.queryTimeout,
                newAttributes != null ? newAttributes : this.attributes,
                this.paginationEnabled,
                this.fetchSize,
                cache,
                this.dynamic,
                this.namingStrategy);
    }

    /**
     * Creates a new builder for QueryDefinition
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Fluent builder for QueryDefinition with validation logic
     */
    public static class Builder {
        protected final String name;
        protected String sql;
        protected String description;
        protected final Map<String, AttributeDef<?>> attributes = new LinkedHashMap<>();
        protected final Map<String, ParamDef<?>> parameters = new LinkedHashMap<>();
        protected final Map<String, CriteriaDef> criteria = new LinkedHashMap<>();
        protected final List<PreProcessor> preProcessors = new ArrayList<>();
        protected final List<RowProcessor> rowProcessors = new ArrayList<>();
        protected final List<PostProcessor> postProcessors = new ArrayList<>();

        // Cache configuration
        protected Boolean cacheEnabled = false;
        protected Duration cacheTTL;
        protected Function<Object, String> cacheKeyGenerator;

        // Pagination configuration
        protected Integer defaultPageSize = 50;
        protected Integer maxPageSize = 1000;
        protected Boolean paginationEnabled = true;

        // Fetch size configuration
        protected Integer fetchSize = null; // null means use system default

        // Other configurations
        protected Boolean auditEnabled = true;
        protected Boolean metricsEnabled = true;
        protected Integer queryTimeout;

        // Dynamic attributes configuration
        protected Boolean dynamic = false;
        protected NamingStrategy dynamicAttributeNamingStrategy = NamingStrategy.CAMEL;

        protected Builder(String name) {
            Preconditions.checkNotNull(name, "Query name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "Query name cannot be empty");
            this.name = name;
        }

        public Builder sql(String sql) {
            Preconditions.checkNotNull(sql, "SQL cannot be null");
            this.sql = sql;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Add a fully-built attribute to the query definition
         */
        public Builder attribute(AttributeDef<?> attribute) {
            Preconditions.checkNotNull(attribute, "Attribute cannot be null");

            // Check for duplicate attribute during building
            if (this.attributes.containsKey(attribute.name())) {
                throw new IllegalStateException(String.format(
                        "Duplicate attribute definition: Attribute '%s' is already defined in this query",
                        attribute.name()));
            }

            this.attributes.put(attribute.name(), attribute);
            return this;
        }

        /**
         * Add a fully-built parameter to the query definition
         */
        public Builder parameter(ParamDef<?> param) {
            Preconditions.checkNotNull(param, "Parameter cannot be null");

            // Check for duplicate parameter during building
            if (this.parameters.containsKey(param.name())) {
                throw new IllegalStateException(String.format(
                        "Duplicate parameter definition: Parameter '%s' is already defined in this query",
                        param.name()));
            }

            this.parameters.put(param.name(), param);
            return this;
        }

        /**
         * Add a fully-built criteria to the query definition
         */
        public Builder criteria(CriteriaDef criteria) {
            Preconditions.checkNotNull(criteria, "Criteria cannot be null");

            // Check for duplicate criteria during building
            if (this.criteria.containsKey(criteria.name())) {
                throw new IllegalStateException(String.format(
                        "Duplicate criteria definition: Criteria '%s' is already defined in this query",
                        criteria.name()));
            }

            this.criteria.put(criteria.name(), criteria);
            return this;
        }

        // Processor methods
        public Builder preProcessor(PreProcessor processor) {
            Preconditions.checkNotNull(processor, "PreProcessor cannot be null");
            this.preProcessors.add(context -> {
                processor.process((com.balsam.oasis.common.registry.domain.execution.QueryContext) context);
            });
            return this;
        }

        public Builder rowProcessor(RowProcessor processor) {
            Preconditions.checkNotNull(processor, "RowProcessor cannot be null");
            this.rowProcessors.add(processor);
            return this;
        }

        public Builder postProcessor(PostProcessor processor) {
            Preconditions.checkNotNull(processor, "PostProcessor cannot be null");
            this.postProcessors.add(processor);
            return this;
        }

        // Cache configuration
        public Builder cache(Boolean enabled) {
            this.cacheEnabled = enabled;
            return this;
        }

        public Builder cacheTTL(Duration ttl) {
            this.cacheTTL = ttl;
            this.cacheEnabled = true;
            return this;
        }

        public Builder cacheKey(Function<Object, String> keyGenerator) {
            this.cacheKeyGenerator = keyGenerator;
            return this;
        }

        // Pagination configuration
        public Builder defaultPageSize(Integer size) {
            Preconditions.checkArgument(size > 0, "Page size must be positive");
            this.defaultPageSize = size;
            return this;
        }

        public Builder maxPageSize(Integer size) {
            Preconditions.checkArgument(size > 0, "Max page size must be positive");
            this.maxPageSize = size;
            return this;
        }

        public Builder paginationEnabled(Boolean enabled) {
            this.paginationEnabled = enabled;
            return this;
        }

        /**
         * Set the JDBC fetch size for this query.
         * 
         * @param size The number of rows to fetch in each round trip.
         *             Use null for system default, 0 for fetch all, positive for
         *             specific size.
         */
        public Builder fetchSize(Integer size) {
            if (size != null && size < 0 && size != -1) {
                throw new IllegalArgumentException("Fetch size must be -1, 0, or positive");
            }
            this.fetchSize = size;
            return this;
        }

        // Other configurations
        public Builder auditEnabled(Boolean enabled) {
            this.auditEnabled = enabled;
            return this;
        }

        public Builder metricsEnabled(Boolean enabled) {
            this.metricsEnabled = enabled;
            return this;
        }

        public Builder queryTimeout(Integer seconds) {
            this.queryTimeout = seconds;
            return this;
        }

        // Dynamic attributes configuration

        /**
         * Enable dynamic attributes with default naming strategy (CAMEL).
         */
        public Builder dynamic() {
            this.dynamic = true;
            this.dynamicAttributeNamingStrategy = NamingStrategy.CAMEL;
            return this;
        }

        /**
         * Enable dynamic attributes with specified naming strategy.
         */
        public Builder dynamic(NamingStrategy strategy) {
            Preconditions.checkNotNull(strategy, "NamingStrategy cannot be null");
            this.dynamic = true;
            this.dynamicAttributeNamingStrategy = strategy;
            return this;
        }

        public QueryDefinitionBuilder build() {
            validate();

            CacheConfig cacheConfig = null;
            if (cacheEnabled) {
                cacheConfig = CacheConfig.builder()
                        .enabled(true)
                        .ttl(cacheTTL != null ? cacheTTL : Duration.ofMinutes(5))
                        .keyGenerator(cacheKeyGenerator)
                        .build();
            }

            QueryDefinitionBuilder queryDef = new QueryDefinitionBuilder(
                    name,
                    description,
                    sql,
                    ImmutableMap.copyOf(parameters),
                    ImmutableMap.copyOf(criteria),
                    ImmutableList.copyOf(preProcessors),
                    ImmutableList.copyOf(rowProcessors),
                    ImmutableList.copyOf(postProcessors),
                    cacheConfig,
                    defaultPageSize,
                    maxPageSize,
                    auditEnabled,
                    metricsEnabled,
                    queryTimeout,
                    ImmutableMap.copyOf(attributes),
                    paginationEnabled,
                    fetchSize,
                    null, // metadataCache - set later if needed
                    dynamic,
                    dynamicAttributeNamingStrategy);

            // Comprehensive validation:
            // 1. Validates no duplicate definitions within the query (attributes, params,
            // criteria)
            // 2. Validates all bind parameters in SQL and criteria are defined
            // 3. Validate bind parameters
            BindParameterValidator.validate(queryDef);

            return queryDef;
        }

        private void validate() {
            Preconditions.checkNotNull(sql, "SQL is required");
            Preconditions.checkArgument(!sql.trim().isEmpty(), "SQL cannot be empty");

            // Validate criteria placeholders
            validateCriteriaPlaceholders();

            // Validate parameter references
            validateParamReferences();
        }

        private void validateCriteriaPlaceholders() {
            // Check that SQL contains placeholders for all criteria
            for (CriteriaDef criteriaDef : criteria.values()) {
                String placeholder = "--" + criteriaDef.name();
                if (!sql.contains(placeholder)) {
                    throw new IllegalArgumentException(
                            "SQL does not contain placeholder for criteria: " + placeholder);
                }
            }
        }

        private void validateParamReferences() {
            for (ParamDef<?> paramDef : parameters.values()) {
                if (paramDef.required()) {
                    Boolean referenced = sql.contains(":" + paramDef.name());

                    if (!referenced) {
                        throw new IllegalArgumentException(
                                "Required parameter not referenced in SQL: " + paramDef.name());
                    }
                }
            }
        }
    }
}