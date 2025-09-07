package com.balsam.oasis.common.registry.select;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.CacheConfig;
import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.core.definition.ValidationRule;
import com.balsam.oasis.common.registry.processor.PostProcessor;
import com.balsam.oasis.common.registry.processor.PreProcessor;
import com.balsam.oasis.common.registry.processor.RowProcessor;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Builder for creating select-type QueryDefinition instances with fluent API.
 * This builder creates QueryDefinition objects that work with the Select API
 * by ensuring the first attribute is the value field and second is the label
 * field.
 * Leverages the full Query infrastructure while maintaining the Select API
 * pattern.
 */
public class SelectDefinitionBuilder {

    private final String name;
    private String description;
    private String sql;
    private ValueDef valueDef;
    private LabelDef labelDef;
    private final List<AttributeDef<?>> additionAttributes = new ArrayList<>();
    private final Map<String, ParamDef<?>> params = new LinkedHashMap<>();
    private final Map<String, CriteriaDef> criteria = new LinkedHashMap<>();
    private CriteriaDef searchCriteria;

    // Processing pipeline
    private final List<PreProcessor> preProcessors = new ArrayList<>();
    private final List<RowProcessor> rowProcessors = new ArrayList<>();
    private final List<PostProcessor> postProcessors = new ArrayList<>();
    private final List<ValidationRule> validationRules = new ArrayList<>();

    // Cache configuration
    private boolean cacheEnabled = false;
    private Duration cacheTTL;
    private Function<Object, String> cacheKeyGenerator;

    // Pagination configuration
    private int defaultPageSize = 100;
    private int maxPageSize = 1000;

    // Other configurations
    private boolean auditEnabled = true;
    private boolean metricsEnabled = true;
    private Integer queryTimeout;

    private SelectDefinitionBuilder(String name) {
        Preconditions.checkNotNull(name, "Select name cannot be null");
        Preconditions.checkArgument(!name.trim().isEmpty(), "Select name cannot be empty");
        this.name = name;
    }

    /**
     * Create a new builder with the given select name
     */
    public static SelectDefinitionBuilder builder(String name) {
        return new SelectDefinitionBuilder(name);
    }

    /**
     * Set the description
     */
    public SelectDefinitionBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the SQL query
     */
    public SelectDefinitionBuilder sql(String sql) {
        Preconditions.checkNotNull(sql, "SQL cannot be null");
        Preconditions.checkArgument(!sql.trim().isEmpty(), "SQL cannot be empty");
        this.sql = sql;
        return this;
    }

    /**
     * Set the value definition (only requires aliasName and type)
     */
    public SelectDefinitionBuilder value(ValueDef valueDef) {
        Preconditions.checkNotNull(valueDef, "Value definition cannot be null");
        this.valueDef = valueDef;
        return this;
    }

    /**
     * Create value definition with database column name and Java type
     */
    public SelectDefinitionBuilder value(String aliasName, Class<?> type) {
        this.valueDef = ValueDef.of(aliasName, type);
        return this;
    }

    /**
     * Set the label definition (only requires aliasName and type)
     */
    public SelectDefinitionBuilder label(LabelDef labelDef) {
        Preconditions.checkNotNull(labelDef, "Label definition cannot be null");
        this.labelDef = labelDef;
        return this;
    }

    /**
     * Create label definition with database column name and Java type
     */
    public SelectDefinitionBuilder label(String aliasName, Class<?> type) {
        this.labelDef = LabelDef.of(aliasName, type);
        return this;
    }

    /**
     * Create label definition with database column name (defaults to String type)
     */
    public SelectDefinitionBuilder label(String aliasName) {
        this.labelDef = LabelDef.of(aliasName);
        return this;
    }

    /**
     * Add an additional attribute to include in the additions field
     */
    public SelectDefinitionBuilder addition(AttributeDef<?> attribute) {
        Preconditions.checkNotNull(attribute, "Addition attribute cannot be null");
        this.additionAttributes.add(attribute);
        return this;
    }

    /**
     * Create addition attribute inline with lambda customizer
     */
    public <T> SelectDefinitionBuilder addition(String name, Class<T> type,
            Function<AttributeDef.BuilderStage<T>, AttributeDef.BuilderStage<T>> customizer) {
        AttributeDef.BuilderStage<T> builder = AttributeDef.name(name)
                .type(type)
                .aliasName(name);

        if (customizer != null) {
            builder = customizer.apply(builder);
        }

        this.additionAttributes.add(builder.build());
        return this;
    }

    /**
     * Add multiple additional attributes
     */
    public SelectDefinitionBuilder additions(AttributeDef<?>... attributes) {
        for (AttributeDef<?> attr : attributes) {
            addition(attr);
        }
        return this;
    }

    /**
     * Set the search criteria
     */
    public SelectDefinitionBuilder searchCriteria(CriteriaDef searchCriteria) {
        Preconditions.checkNotNull(searchCriteria, "Search criteria cannot be null");
        this.searchCriteria = searchCriteria;
        return this;
    }

    /**
     * Create search criteria inline
     */
    public SelectDefinitionBuilder searchCriteria(String name, String sql) {
        this.searchCriteria = CriteriaDef.criteria()
                .name(name)
                .sql(sql)
                .build();
        return this;
    }

    /**
     * Add a criteria
     */
    public SelectDefinitionBuilder criteria(CriteriaDef criteria) {
        Preconditions.checkNotNull(criteria, "Criteria cannot be null");
        Preconditions.checkNotNull(criteria.getName(), "Criteria name cannot be null");
        this.criteria.put(criteria.getName(), criteria);
        return this;
    }

    /**
     * Create criteria inline
     */
    public SelectDefinitionBuilder criteria(String name, String sql) {
        return criteria(CriteriaDef.criteria()
                .name(name)
                .sql(sql)
                .build());
    }

    /**
     * Create criteria inline with condition
     */
    public SelectDefinitionBuilder criteria(String name, String sql,
            Predicate<Object> condition) {
        return criteria(CriteriaDef.criteria()
                .name(name)
                .sql(sql)
                .condition(ctx -> {
                    // Apply the condition to the context
                    return condition.test(ctx);
                })
                .build());
    }

    /**
     * Add a parameter
     */
    public SelectDefinitionBuilder param(ParamDef<?> param) {
        Preconditions.checkNotNull(param, "Parameter cannot be null");
        Preconditions.checkNotNull(param.getName(), "Parameter name cannot be null");
        this.params.put(param.getName(), param);
        return this;
    }

    /**
     * Create parameter inline with lambda customizer
     */
    public <T> SelectDefinitionBuilder param(String name, Class<T> type,
            Function<ParamDef<T>, ParamDef<T>> customizer) {
        ParamDef<T> param = ParamDef.<T>param(name)
                .type(type)
                .build();

        if (customizer != null) {
            param = customizer.apply(param);
        }

        return param(param);
    }

    /**
     * Add a pre-processor
     */
    public SelectDefinitionBuilder preProcessor(PreProcessor processor) {
        Preconditions.checkNotNull(processor, "Pre-processor cannot be null");
        // PreProcessor expects QueryContext, we can't use it directly with
        // SelectContext
        // Store as a Function<Object, Object> that will be adapted at runtime
        this.preProcessors.add(processor);
        return this;
    }

    /**
     * Add a row processor
     */
    public SelectDefinitionBuilder rowProcessor(RowProcessor processor) {
        Preconditions.checkNotNull(processor, "Row processor cannot be null");
        this.rowProcessors.add(processor);
        return this;
    }

    /**
     * Add a post-processor
     */
    public SelectDefinitionBuilder postProcessor(PostProcessor processor) {
        Preconditions.checkNotNull(processor, "Post-processor cannot be null");
        this.postProcessors.add(processor);
        return this;
    }

    /**
     * Add a validation rule
     */
    public SelectDefinitionBuilder validationRule(ValidationRule rule) {
        Preconditions.checkNotNull(rule, "Validation rule cannot be null");
        this.validationRules.add(rule);
        return this;
    }

    /**
     * Configure caching
     */
    public SelectDefinitionBuilder cache(boolean enabled) {
        this.cacheEnabled = enabled;
        return this;
    }

    /**
     * Configure caching with TTL
     */
    public SelectDefinitionBuilder cache(Duration ttl) {
        Preconditions.checkNotNull(ttl, "Cache TTL cannot be null");
        this.cacheEnabled = true;
        this.cacheTTL = ttl;
        return this;
    }

    /**
     * Configure cache with custom key generator
     */
    public SelectDefinitionBuilder cacheKeyGenerator(Function<Object, String> generator) {
        this.cacheKeyGenerator = generator;
        return this;
    }

    /**
     * Set the default page size
     */
    public SelectDefinitionBuilder defaultPageSize(int defaultPageSize) {
        Preconditions.checkArgument(defaultPageSize > 0, "Default page size must be positive");
        this.defaultPageSize = defaultPageSize;
        return this;
    }

    /**
     * Set the maximum page size
     */
    public SelectDefinitionBuilder maxPageSize(int maxPageSize) {
        Preconditions.checkArgument(maxPageSize > 0, "Max page size must be positive");
        this.maxPageSize = maxPageSize;
        return this;
    }

    /**
     * Enable/disable audit logging
     */
    public SelectDefinitionBuilder auditEnabled(boolean enabled) {
        this.auditEnabled = enabled;
        return this;
    }

    /**
     * Enable/disable metrics collection
     */
    public SelectDefinitionBuilder metricsEnabled(boolean enabled) {
        this.metricsEnabled = enabled;
        return this;
    }

    /**
     * Set query timeout in seconds
     */
    public SelectDefinitionBuilder queryTimeout(int seconds) {
        Preconditions.checkArgument(seconds > 0, "Query timeout must be positive");
        this.queryTimeout = seconds;
        return this;
    }

    /**
     * Build the QueryDefinition with select-type configuration
     */
    public QueryDefinition build() {
        // Validation
        Preconditions.checkNotNull(sql, "SQL must be set");
        Preconditions.checkNotNull(valueDef, "Value definition must be set");
        Preconditions.checkNotNull(labelDef, "Label definition must be set");

        // If searchCriteria is defined, ensure we have a search parameter
        if (searchCriteria != null && !params.containsKey("search")) {
            // Auto-add search parameter
            params.put("search", ParamDef.param("search")
                    .type(String.class)
                    .description("Search term")
                    .required(false)
                    .build());
        }

        // Build cache config if enabled
        CacheConfig cacheConfig = null;
        if (cacheEnabled) {
            cacheConfig = CacheConfig.builder()
                    .enabled(true)
                    .ttl(cacheTTL != null ? cacheTTL : Duration.ofMinutes(5))
                    .keyGenerator(cacheKeyGenerator)
                    .build();
        }

        // Build the attributes map with standardized names
        Map<String, AttributeDef<?>> allAttributes = new LinkedHashMap<>();

        // Convert ValueDef and LabelDef to AttributeDef with predefined properties
        AttributeDef<?> valueAttribute = valueDef.toAttributeDef();
        AttributeDef<?> labelAttribute = labelDef.toAttributeDef();

        allAttributes.put("value", valueAttribute);
        allAttributes.put("label", labelAttribute);

        // Add addition attributes
        for (AttributeDef<?> attr : additionAttributes) {
            allAttributes.put(attr.getName(), attr);
        }

        // Add search criteria to main criteria if defined
        Map<String, CriteriaDef> allCriteria = new LinkedHashMap<>(criteria);
        if (searchCriteria != null) {
            allCriteria.put(searchCriteria.getName(), searchCriteria);
        }

        return QueryDefinition.builder()
                .name(name)
                .description(description)
                .sql(sql)
                .attributes(ImmutableMap.copyOf(allAttributes))
                .params(ImmutableMap.copyOf(params))
                .criteria(ImmutableMap.copyOf(allCriteria))
                .preProcessors(ImmutableList.copyOf(preProcessors))
                .rowProcessors(ImmutableList.copyOf(rowProcessors))
                .postProcessors(ImmutableList.copyOf(postProcessors))
                .validationRules(ImmutableList.copyOf(validationRules))
                .cacheConfig(cacheConfig)
                .defaultPageSize(defaultPageSize)
                .maxPageSize(maxPageSize)
                .auditEnabled(auditEnabled)
                .metricsEnabled(metricsEnabled)
                .queryTimeout(queryTimeout)
                .build();
    }
}