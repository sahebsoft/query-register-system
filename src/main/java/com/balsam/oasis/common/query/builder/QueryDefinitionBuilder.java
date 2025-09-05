package com.balsam.oasis.common.query.builder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.balsam.oasis.common.query.core.definition.AttributeDef;
import com.balsam.oasis.common.query.core.definition.CacheConfig;
import com.balsam.oasis.common.query.core.definition.CriteriaDef;
import com.balsam.oasis.common.query.core.definition.ParamDef;
import com.balsam.oasis.common.query.core.definition.QueryDefinition;
import com.balsam.oasis.common.query.core.definition.ValidationRule;
import com.balsam.oasis.common.query.processor.PostProcessor;
import com.balsam.oasis.common.query.processor.PreProcessor;
import com.balsam.oasis.common.query.processor.RowProcessor;
import com.balsam.oasis.common.query.validation.BindParameterValidator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Fluent builder for QueryDefinition
 * Accepts fully-built isolated objects for attributes, params, and criteria
 */
public class QueryDefinitionBuilder {

    private final String name;
    private String sql;
    private String description;
    private final Map<String, AttributeDef<?>> attributes = new LinkedHashMap<>();
    private final Map<String, ParamDef<?>> params = new LinkedHashMap<>();
    private final Map<String, CriteriaDef> criteria = new LinkedHashMap<>();
    private final List<Function<Object, Object>> preProcessors = new ArrayList<>();
    private final List<Function<Object, Object>> rowProcessors = new ArrayList<>();
    private final List<Function<Object, Object>> postProcessors = new ArrayList<>();
    private final List<ValidationRule> validationRules = new ArrayList<>();

    // Cache configuration
    private boolean cacheEnabled = false;
    private Duration cacheTTL;
    private Function<Object, String> cacheKeyGenerator;

    // Pagination configuration
    private int defaultPageSize = 50;
    private int maxPageSize = 1000;
    private boolean paginationEnabled = true;

    // Other configurations
    private boolean auditEnabled = true;
    private boolean metricsEnabled = true;
    private Integer queryTimeout;
    private String findByKeyCriteriaName;
    

    private QueryDefinitionBuilder(String name) {
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
        if (this.attributes.containsKey(attribute.getName())) {
            throw new IllegalStateException(String.format(
                    "Duplicate attribute definition: Attribute '%s' is already defined in this query",
                    attribute.getName()));
        }

        this.attributes.put(attribute.getName(), attribute);
        return this;
    }

    /**
     * Add a fully-built parameter to the query definition
     */
    public QueryDefinitionBuilder param(ParamDef<?> param) {
        Preconditions.checkNotNull(param, "Parameter cannot be null");

        // Check for duplicate parameter during building
        if (this.params.containsKey(param.getName())) {
            throw new IllegalStateException(String.format(
                    "Duplicate parameter definition: Parameter '%s' is already defined in this query",
                    param.getName()));
        }

        this.params.put(param.getName(), param);
        return this;
    }

    /**
     * Add a fully-built criteria to the query definition
     */
    public QueryDefinitionBuilder criteria(CriteriaDef criteria) {
        Preconditions.checkNotNull(criteria, "Criteria cannot be null");

        // Check for duplicate criteria during building
        if (this.criteria.containsKey(criteria.getName())) {
            throw new IllegalStateException(String.format(
                    "Duplicate criteria definition: Criteria '%s' is already defined in this query",
                    criteria.getName()));
        }

        this.criteria.put(criteria.getName(), criteria);
        if (criteria.isFindByKey()) {
            this.findByKeyCriteriaName = criteria.getName();
        }
        return this;
    }

    // Processor methods
    public QueryDefinitionBuilder preProcessor(PreProcessor processor) {
        Preconditions.checkNotNull(processor, "PreProcessor cannot be null");
        this.preProcessors.add(context -> {
            processor.process((com.balsam.oasis.common.query.core.execution.QueryContext) context);
            return null;
        });
        return this;
    }

    public QueryDefinitionBuilder preProcessor(Function<Object, Object> processor) {
        Preconditions.checkNotNull(processor, "PreProcessor cannot be null");
        this.preProcessors.add(processor);
        return this;
    }

    public QueryDefinitionBuilder rowProcessor(RowProcessor processor) {
        Preconditions.checkNotNull(processor, "RowProcessor cannot be null");
        this.rowProcessors.add((row) -> processor.process(
                (com.balsam.oasis.common.query.core.result.Row) row,
                (com.balsam.oasis.common.query.core.execution.QueryContext) null));
        return this;
    }

    public QueryDefinitionBuilder rowProcessor(Function<Object, Object> processor) {
        Preconditions.checkNotNull(processor, "RowProcessor cannot be null");
        this.rowProcessors.add(processor);
        return this;
    }

    public QueryDefinitionBuilder postProcessor(PostProcessor processor) {
        Preconditions.checkNotNull(processor, "PostProcessor cannot be null");
        this.postProcessors.add((result) -> processor.process(
                (com.balsam.oasis.common.query.core.result.QueryResult) result,
                (com.balsam.oasis.common.query.core.execution.QueryContext) null));
        return this;
    }

    public QueryDefinitionBuilder postProcessor(Function<Object, Object> processor) {
        Preconditions.checkNotNull(processor, "PostProcessor cannot be null");
        this.postProcessors.add(processor);
        return this;
    }

    // Validation rules
    public QueryDefinitionBuilder validationRule(ValidationRule rule) {
        Preconditions.checkNotNull(rule, "ValidationRule cannot be null");
        this.validationRules.add(rule);
        return this;
    }

    // Cache configuration
    public QueryDefinitionBuilder cache(boolean enabled) {
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
    public QueryDefinitionBuilder defaultPageSize(int size) {
        Preconditions.checkArgument(size > 0, "Page size must be positive");
        this.defaultPageSize = size;
        return this;
    }

    public QueryDefinitionBuilder maxPageSize(int size) {
        Preconditions.checkArgument(size > 0, "Max page size must be positive");
        this.maxPageSize = size;
        return this;
    }

    public QueryDefinitionBuilder paginationEnabled(boolean enabled) {
        this.paginationEnabled = enabled;
        return this;
    }

    // Other configurations
    public QueryDefinitionBuilder auditEnabled(boolean enabled) {
        this.auditEnabled = enabled;
        return this;
    }

    public QueryDefinitionBuilder metricsEnabled(boolean enabled) {
        this.metricsEnabled = enabled;
        return this;
    }

    public QueryDefinitionBuilder queryTimeout(int seconds) {
        this.queryTimeout = seconds;
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
                .params(ImmutableMap.copyOf(params))
                .criteria(ImmutableMap.copyOf(criteria))
                .preProcessors(ImmutableList.copyOf(preProcessors))
                .rowProcessors(ImmutableList.copyOf(rowProcessors))
                .postProcessors(ImmutableList.copyOf(postProcessors))
                .validationRules(ImmutableList.copyOf(validationRules))
                .cacheConfig(cacheConfig)
                .defaultPageSize(defaultPageSize)
                .maxPageSize(maxPageSize)
                .paginationEnabled(paginationEnabled)
                .auditEnabled(auditEnabled)
                .metricsEnabled(metricsEnabled)
                .queryTimeout(queryTimeout)
                .findByKeyCriteriaName(findByKeyCriteriaName)
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

        // Validate attributes
        validateAttributes();

        // Validate criteria placeholders
        validateCriteriaPlaceholders();

        // Validate parameter references
        validateParamReferences();
    }

    private void validateAttributes() {
        // Check for primary key if needed
        boolean hasPrimaryKey = attributes.values().stream()
                .anyMatch(AttributeDef::isPrimaryKey);

        // Validate transient attributes have calculators
        attributes.values().stream()
                .filter(AttributeDef::isTransient)
                .forEach(attr -> {
                    if (!attr.hasCalculator()) {
                        throw new IllegalArgumentException(
                                "Transient attribute must have a calculator: " + attr.getName());
                    }
                });
    }

    private void validateCriteriaPlaceholders() {
        // Check that SQL contains placeholders for all criteria
        for (CriteriaDef criteriaDef : criteria.values()) {
            String placeholder = "--" + criteriaDef.getName();
            if (!sql.contains(placeholder)) {
                throw new IllegalArgumentException(
                        "SQL does not contain placeholder for criteria: " + placeholder);
            }
        }
    }

    private void validateParamReferences() {
        for (ParamDef<?> paramDef : params.values()) {
            if (paramDef.isRequired()) {
                boolean referenced = sql.contains(":" + paramDef.getName());

                if (!referenced) {
                    throw new IllegalArgumentException(
                            "Required parameter not referenced in SQL: " + paramDef.getName());
                }
            }
        }
    }
}