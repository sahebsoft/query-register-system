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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Fluent builder for QueryDefinition
 * Accepts fully-built isolated objects for attributes, params, and criteria
 */
public class QueryDefinitionBuilder {

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

    protected QueryDefinitionBuilder(String name) {
        Preconditions.checkNotNull(name, "Query name cannot be null");
        Preconditions.checkArgument(!name.trim().isEmpty(), "Query name cannot be empty");
        this.name = name;
    }

    public static QueryDefinitionBuilder builder(String name) {
        return new QueryDefinitionBuilder(name);
    }

    public QueryDefinitionBuilder sql(String sql) {
        Preconditions.checkNotNull(sql, "SQL cannot be null");
        this.sql = sql;
        return this;
    }

    public QueryDefinitionBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Add a fully-built attribute to the query definition
     */
    public QueryDefinitionBuilder attribute(AttributeDef<?> attribute) {
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
    public QueryDefinitionBuilder parameter(ParamDef<?> param) {
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
    public QueryDefinitionBuilder criteria(CriteriaDef criteria) {
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
    public QueryDefinitionBuilder preProcessor(PreProcessor processor) {
        Preconditions.checkNotNull(processor, "PreProcessor cannot be null");
        this.preProcessors.add(context -> {
            processor.process((com.balsam.oasis.common.registry.domain.execution.QueryContext) context);
        });
        return this;
    }

    public QueryDefinitionBuilder rowProcessor(RowProcessor processor) {
        Preconditions.checkNotNull(processor, "RowProcessor cannot be null");
        this.rowProcessors.add(processor);
        return this;
    }

    public QueryDefinitionBuilder postProcessor(PostProcessor processor) {
        Preconditions.checkNotNull(processor, "PostProcessor cannot be null");
        this.postProcessors.add(processor);
        return this;
    }

    // Cache configuration
    public QueryDefinitionBuilder cache(Boolean enabled) {
        this.cacheEnabled = enabled;
        return this;
    }

    public QueryDefinitionBuilder cacheTTL(Duration ttl) {
        this.cacheTTL = ttl;
        this.cacheEnabled = true;
        return this;
    }

    public QueryDefinitionBuilder cacheKey(Function<Object, String> keyGenerator) {
        this.cacheKeyGenerator = keyGenerator;
        return this;
    }

    // Pagination configuration
    public QueryDefinitionBuilder defaultPageSize(Integer size) {
        Preconditions.checkArgument(size > 0, "Page size must be positive");
        this.defaultPageSize = size;
        return this;
    }

    public QueryDefinitionBuilder maxPageSize(Integer size) {
        Preconditions.checkArgument(size > 0, "Max page size must be positive");
        this.maxPageSize = size;
        return this;
    }

    public QueryDefinitionBuilder paginationEnabled(Boolean enabled) {
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
    public QueryDefinitionBuilder fetchSize(Integer size) {
        if (size != null && size < 0 && size != -1) {
            throw new IllegalArgumentException("Fetch size must be -1, 0, or positive");
        }
        this.fetchSize = size;
        return this;
    }

    // Other configurations
    public QueryDefinitionBuilder auditEnabled(Boolean enabled) {
        this.auditEnabled = enabled;
        return this;
    }

    public QueryDefinitionBuilder metricsEnabled(Boolean enabled) {
        this.metricsEnabled = enabled;
        return this;
    }

    public QueryDefinitionBuilder queryTimeout(Integer seconds) {
        this.queryTimeout = seconds;
        return this;
    }

    // Dynamic attributes configuration

    /**
     * Enable dynamic attributes with default naming strategy (CAMEL).
     */
    public QueryDefinitionBuilder dynamic() {
        this.dynamic = true;
        this.dynamicAttributeNamingStrategy = NamingStrategy.CAMEL;
        return this;
    }

    /**
     * Enable dynamic attributes with specified naming strategy.
     */
    public QueryDefinitionBuilder dynamic(NamingStrategy strategy) {
        Preconditions.checkNotNull(strategy, "NamingStrategy cannot be null");
        this.dynamic = true;
        this.dynamicAttributeNamingStrategy = strategy;
        return this;
    }

    public QueryDefinition build() {
        validate();

        CacheConfig cacheConfig = null;
        if (cacheEnabled) {
            cacheConfig = CacheConfig.builder()
                    .enabled(true)
                    .ttl(cacheTTL != null ? cacheTTL : Duration.ofMinutes(5))
                    .keyGenerator(cacheKeyGenerator)
                    .build();
        }

        QueryDefinition queryDef = QueryDefinition.builder()
                .name(name)
                .sql(sql)
                .description(description)
                .attributes(ImmutableMap.copyOf(attributes))
                .parameters(ImmutableMap.copyOf(parameters))
                .criteria(ImmutableMap.copyOf(criteria))
                .preProcessors(ImmutableList.copyOf(preProcessors))
                .rowProcessors(ImmutableList.copyOf(rowProcessors))
                .postProcessors(ImmutableList.copyOf(postProcessors))
                .cacheConfig(cacheConfig)
                .defaultPageSize(defaultPageSize)
                .maxPageSize(maxPageSize)
                .paginationEnabled(paginationEnabled)
                .fetchSize(fetchSize)
                .auditEnabled(auditEnabled)
                .metricsEnabled(metricsEnabled)
                .queryTimeout(queryTimeout)
                .metadataCache(null) // set later if needed
                .dynamic(dynamic)
                .namingStrategy(dynamicAttributeNamingStrategy)
                .build();

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