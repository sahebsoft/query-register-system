package com.balasam.oasis.common.query.builder;

import com.balasam.oasis.common.query.core.definition.*;
import com.balasam.oasis.common.query.processor.PreProcessor;
import com.balasam.oasis.common.query.processor.RowProcessor;
import com.balasam.oasis.common.query.processor.PostProcessor;
import com.balasam.oasis.common.query.processor.Processor;
import com.balasam.oasis.common.query.processor.ParamProcessor;
import com.balasam.oasis.common.query.processor.Validator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent builder for QueryDefinition
 */
public class QueryDefinitionBuilder {
    
    private String name;
    private String sql;
    private String description;
    private final Map<String, AttributeDef> attributes = new LinkedHashMap<>();
    private final Map<String, ParamDef> params = new LinkedHashMap<>();
    private final Map<String, CriteriaDef> criteria = new LinkedHashMap<>();
    private final List<Function<Object, Object>> preProcessors = new ArrayList<>();
    private final List<Function<Object, Object>> rowProcessors = new ArrayList<>();
    private final List<Function<Object, Object>> postProcessors = new ArrayList<>();
    private final List<ValidationRule> validationRules = new ArrayList<>();
    private CacheConfig cacheConfig;
    private int defaultPageSize = 50;
    private int maxPageSize = 1000;
    private boolean paginationEnabled = true;
    private boolean auditEnabled = true;
    private boolean metricsEnabled = true;
    private Integer queryTimeout;
    private String findByKeyCriteriaName;
    
    public QueryDefinitionBuilder(String name) {
        this.name = name;
    }
    
    public static QueryDefinitionBuilder builder(String name) {
        return new QueryDefinitionBuilder(name);
    }
    
    public QueryDefinitionBuilder sql(String sql) {
        this.sql = sql;
        return this;
    }
    
    public QueryDefinitionBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    public AttributeBuilder attribute(String name) {
        return new AttributeBuilder(this, name);
    }
    
    public VirtualAttributeBuilder virtualAttribute(String name) {
        return new VirtualAttributeBuilder(this, name);
    }
    
    public ParamBuilder param(String name) {
        return new ParamBuilder(this, name);
    }
    
    public CriteriaBuilder criteria(String name) {
        return new CriteriaBuilder(this, name);
    }
    
    public FindByKeyBuilder findByKey(String name) {
        return new FindByKeyBuilder(this, name);
    }
    
    public QueryDefinitionBuilder preProcessor(PreProcessor processor) {
        this.preProcessors.add(ctx -> {
            processor.process((com.balasam.oasis.common.query.core.execution.QueryContext) ctx);
            return null;
        });
        return this;
    }
    
    public QueryDefinitionBuilder rowProcessor(RowProcessor processor) {
        this.rowProcessors.add(obj -> processor.process(
            (com.balasam.oasis.common.query.core.result.Row) ((Object[]) obj)[0],
            (com.balasam.oasis.common.query.core.execution.QueryContext) ((Object[]) obj)[1]
        ));
        return this;
    }
    
    public QueryDefinitionBuilder postProcessor(PostProcessor processor) {
        this.postProcessors.add(obj -> processor.process(
            (com.balasam.oasis.common.query.core.result.QueryResult) ((Object[]) obj)[0],
            (com.balasam.oasis.common.query.core.execution.QueryContext) ((Object[]) obj)[1]
        ));
        return this;
    }
    
    public QueryDefinitionBuilder validationRule(ValidationRule rule) {
        this.validationRules.add(rule);
        return this;
    }
    
    public QueryDefinitionBuilder validationRule(String name, Predicate<Object> rule, String errorMessage) {
        this.validationRules.add(ValidationRule.builder()
            .name(name)
            .rule(rule)
            .errorMessage(errorMessage)
            .build());
        return this;
    }
    
    public QueryDefinitionBuilder cache(boolean enabled) {
        if (this.cacheConfig == null) {
            this.cacheConfig = CacheConfig.builder().build();
        }
        this.cacheConfig = this.cacheConfig.toBuilder().enabled(enabled).build();
        return this;
    }
    
    public QueryDefinitionBuilder cacheKey(Function<Object, String> keyGen) {
        if (this.cacheConfig == null) {
            this.cacheConfig = CacheConfig.builder().build();
        }
        this.cacheConfig = this.cacheConfig.toBuilder().keyGenerator(keyGen).build();
        return this;
    }
    
    public QueryDefinitionBuilder cacheTTL(Duration ttl) {
        if (this.cacheConfig == null) {
            this.cacheConfig = CacheConfig.builder().build();
        }
        this.cacheConfig = this.cacheConfig.toBuilder().ttl(ttl).build();
        return this;
    }
    
    public QueryDefinitionBuilder defaultPageSize(int size) {
        this.defaultPageSize = size;
        return this;
    }
    
    public QueryDefinitionBuilder maxPageSize(int size) {
        this.maxPageSize = size;
        return this;
    }
    
    public QueryDefinitionBuilder paginationEnabled(boolean enabled) {
        this.paginationEnabled = enabled;
        return this;
    }
    
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
    
    void addAttribute(AttributeDef attribute) {
        this.attributes.put(attribute.getName(), attribute);
    }
    
    void addParam(ParamDef param) {
        this.params.put(param.getName(), param);
    }
    
    void addCriteria(CriteriaDef criteria) {
        this.criteria.put(criteria.getName(), criteria);
    }
    
    void setFindByKeyCriteriaName(String name) {
        this.findByKeyCriteriaName = name;
    }
    
    public QueryDefinition build() {
        validate();
        
        return QueryDefinition.builder()
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
    }
    
    private void validate() {
        Preconditions.checkNotNull(name, "Query name cannot be null");
        Preconditions.checkNotNull(sql, "SQL cannot be null");
        Preconditions.checkArgument(!attributes.isEmpty(), "At least one attribute is required");
        validateCriteriaPlaceholders();
        validateParamReferences();
    }
    
    private void validateCriteriaPlaceholders() {
        for (CriteriaDef criteriaDef : criteria.values()) {
            String placeholder = "--" + criteriaDef.getName();
            if (!sql.contains(placeholder)) {
                throw new IllegalArgumentException(
                    "SQL does not contain placeholder for criteria: " + placeholder);
            }
        }
    }
    
    private void validateParamReferences() {
        for (ParamDef paramDef : params.values()) {
            if (paramDef.isRequired()) {
                boolean referenced = sql.contains(":" + paramDef.getName()) ||
                    criteria.values().stream()
                        .anyMatch(c -> c.getBindParams().contains(paramDef.getName()));
                
                if (!referenced) {
                    throw new IllegalArgumentException(
                        "Required parameter not referenced in SQL: " + paramDef.getName());
                }
            }
        }
    }
    
    /**
     * Attribute builder for regular attributes
     */
    public static class AttributeBuilder {
        private final QueryDefinitionBuilder parent;
        private final String name;
        private String dbColumn;
        private Class<?> type = String.class;
        private boolean filterable = false;
        private boolean sortable = false;
        private boolean calculated = false;
        private boolean primaryKey = false;
        private Set<FilterOp> allowedOperators = new HashSet<>();
        private List<String> allowedValues = new ArrayList<>();
        private Object defaultValue;
        private Processor processor;  // Handles all transformations
        private Function<Object, Boolean> securityRule;
        private String description;
        
        AttributeBuilder(QueryDefinitionBuilder parent, String name) {
            this.parent = parent;
            this.name = name;
            this.dbColumn = name; // Default to same as name
            this.allowedOperators.add(FilterOp.EQUALS); // Default operator
        }
        
        public AttributeBuilder dbColumn(String column) {
            this.dbColumn = column;
            return this;
        }
        
        public AttributeBuilder type(Class<?> type) {
            this.type = type;
            return this;
        }
        
        public AttributeBuilder filterable(boolean filterable) {
            this.filterable = filterable;
            return this;
        }
        
        public AttributeBuilder sortable(boolean sortable) {
            this.sortable = sortable;
            return this;
        }
        
        public AttributeBuilder calculated(boolean calculated) {
            this.calculated = calculated;
            return this;
        }
        
        public AttributeBuilder primaryKey(boolean pk) {
            this.primaryKey = pk;
            return this;
        }
        
        
        public AttributeBuilder filterOperators(FilterOp... ops) {
            this.allowedOperators = new HashSet<>(Arrays.asList(ops));
            return this;
        }
        
        public AttributeBuilder allowedValues(String... values) {
            this.allowedValues = Arrays.asList(values);
            return this;
        }
        
        public AttributeBuilder defaultValue(Object value) {
            this.defaultValue = value;
            return this;
        }
        
        public AttributeBuilder secure(Function<Object, Boolean> rule) {
            this.securityRule = rule;
            return this;
        }
        
        public AttributeBuilder processor(Processor processor) {
            this.processor = processor;
            return this;
        }
        
        // Convenience method for simple processing
        public AttributeBuilder processor(Function<Object, Object> func) {
            this.processor = Processor.simple(func);
            return this;
        }
        
        // Convenience method for masking
        public AttributeBuilder masked() {
            this.processor = Processor.mask("***");
            return this;
        }
        
        // Convenience method for formatting
        public AttributeBuilder formatter(Function<Object, String> formatter) {
            this.processor = Processor.formatter(formatter);
            return this;
        }
        
        public AttributeBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public QueryDefinitionBuilder build() {
            AttributeDef attribute = AttributeDef.builder()
                .name(name)
                .dbColumn(dbColumn)
                .type(type)
                .filterable(filterable)
                .sortable(sortable)
                .calculated(calculated)
                .primaryKey(primaryKey)
                .allowedOperators(ImmutableSet.copyOf(allowedOperators))
                .allowedValues(ImmutableList.copyOf(allowedValues))
                .defaultValue(defaultValue)
                .processor(processor)
                .securityRule(securityRule)
                .description(description)
                .build();
            
            parent.addAttribute(attribute);
            return parent;
        }
    }
    
    /**
     * Virtual attribute builder for computed fields
     */
    public static class VirtualAttributeBuilder extends AttributeBuilder {
        private Set<String> dependencies = new HashSet<>();
        
        VirtualAttributeBuilder(QueryDefinitionBuilder parent, String name) {
            super(parent, name);
            super.calculated(true); // Virtual attributes are always calculated
        }
        
        public VirtualAttributeBuilder dependencies(String... deps) {
            this.dependencies = new HashSet<>(Arrays.asList(deps));
            return this;
        }
        
        @Override
        public QueryDefinitionBuilder build() {
            AttributeDef attribute = AttributeDef.builder()
                .name(super.name)
                .dbColumn(null) // Virtual attributes don't have DB columns
                .type(super.type)
                .filterable(super.filterable)
                .sortable(false) // Virtual attributes can't be sorted at DB level
                .calculated(true)
                .virtual(true)
                .primaryKey(false)
                .allowedOperators(ImmutableSet.copyOf(super.allowedOperators))
                .allowedValues(ImmutableList.copyOf(super.allowedValues))
                .defaultValue(super.defaultValue)
                .processor(super.processor)
                .securityRule(super.securityRule)
                .dependencies(ImmutableSet.copyOf(dependencies))
                .description(super.description)
                .build();
            
            super.parent.addAttribute(attribute);
            return super.parent;
        }
    }
    
    /**
     * Parameter builder
     */
    public static class ParamBuilder {
        private final QueryDefinitionBuilder parent;
        private final String name;
        private Class<?> type = String.class;
        private Class<?> genericType;
        private Object defaultValue;
        private boolean required = false;
        private ParamProcessor processor;  // Handles validation and transformation
        private String description;
        
        ParamBuilder(QueryDefinitionBuilder parent, String name) {
            this.parent = parent;
            this.name = name;
        }
        
        public ParamBuilder type(Class<?> type) {
            this.type = type;
            return this;
        }
        
        public ParamBuilder genericType(Class<?> genericType) {
            this.genericType = genericType;
            return this;
        }
        
        public ParamBuilder defaultValue(Object value) {
            this.defaultValue = value;
            return this;
        }
        
        public ParamBuilder required(boolean required) {
            this.required = required;
            return this;
        }
        
        public ParamBuilder processor(ParamProcessor processor) {
            this.processor = processor;
            return this;
        }
        
        // Convenience method for simple processing
        public ParamBuilder processor(Function<Object, Object> func) {
            this.processor = ParamProcessor.simple(func);
            return this;
        }
        
        // Convenience method for validation-only processor
        public ParamBuilder validator(Function<Object, Boolean> validator) {
            this.processor = ParamProcessor.validator(
                value -> validator.apply(value),
                "Validation failed for parameter: " + name
            );
            return this;
        }
        
        public ParamBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        // Convenience method for range validation
        public ParamBuilder range(int min, int max) {
            this.processor = ParamProcessor.range(min, max);
            return this;
        }
        
        // Convenience method for string length validation
        public ParamBuilder lengthBetween(int min, int max) {
            this.processor = ParamProcessor.lengthBetween(min, max);
            return this;
        }
        
        // Convenience method for pattern validation
        public ParamBuilder pattern(String pattern) {
            this.processor = ParamProcessor.pattern(pattern);
            return this;
        }
        
        public QueryDefinitionBuilder build() {
            ParamDef param = ParamDef.builder()
                .name(name)
                .type(type)
                .genericType(genericType)
                .defaultValue(defaultValue)
                .required(required)
                .processor(processor)
                .description(description)
                .build();
            
            parent.addParam(param);
            return parent;
        }
    }
    
    /**
     * Criteria builder
     */
    public static class CriteriaBuilder {
        private final QueryDefinitionBuilder parent;
        private final String name;
        private String sql;
        private Predicate<Object> condition;
        private Set<String> bindParams = new HashSet<>();
        private Function<Object, Object> processor;
        private boolean dynamic = false;
        private Function<Object, String> generator;
        private boolean securityRelated = false;
        private String description;
        private String appliedReason;
        private int priority = 0;
        
        CriteriaBuilder(QueryDefinitionBuilder parent, String name) {
            this.parent = parent;
            this.name = name;
        }
        
        public CriteriaBuilder sql(String sql) {
            this.sql = sql;
            extractBindParams(sql);
            return this;
        }
        
        private void extractBindParams(String sql) {
            // Extract :paramName patterns from SQL
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":(\\w+)");
            java.util.regex.Matcher matcher = pattern.matcher(sql);
            while (matcher.find()) {
                bindParams.add(matcher.group(1));
            }
        }
        
        public CriteriaBuilder condition(Predicate<Object> condition) {
            this.condition = condition;
            return this;
        }
        
        public CriteriaBuilder bindParams(String... params) {
            this.bindParams = new HashSet<>(Arrays.asList(params));
            return this;
        }
        
        public CriteriaBuilder processor(Function<Object, Object> processor) {
            this.processor = processor;
            return this;
        }
        
        public CriteriaBuilder dynamic(boolean dynamic) {
            this.dynamic = dynamic;
            return this;
        }
        
        public CriteriaBuilder generator(Function<Object, String> generator) {
            this.generator = generator;
            this.dynamic = true;
            return this;
        }
        
        public CriteriaBuilder securityRelated(boolean securityRelated) {
            this.securityRelated = securityRelated;
            return this;
        }
        
        public CriteriaBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public CriteriaBuilder appliedReason(String reason) {
            this.appliedReason = reason;
            return this;
        }
        
        public CriteriaBuilder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public QueryDefinitionBuilder build() {
            CriteriaDef criteria = CriteriaDef.builder()
                .name(name)
                .sql(sql)
                .condition(condition)
                .bindParams(ImmutableSet.copyOf(bindParams))
                .processor(processor)
                .dynamic(dynamic)
                .generator(generator)
                .securityRelated(securityRelated)
                .description(description)
                .appliedReason(appliedReason)
                .priority(priority)
                .build();
            
            parent.addCriteria(criteria);
            return parent;
        }
    }
    
    /**
     * FindByKey builder for single object queries
     */
    public static class FindByKeyBuilder {
        private final QueryDefinitionBuilder parent;
        private final String name;
        private String sql;
        private Set<String> bindParams = new HashSet<>();
        private Function<Object, Object> processor;
        private String description;
        
        FindByKeyBuilder(QueryDefinitionBuilder parent, String name) {
            this.parent = parent;
            this.name = name;
        }
        
        public FindByKeyBuilder sql(String sql) {
            this.sql = sql;
            extractBindParams(sql);
            return this;
        }
        
        private void extractBindParams(String sql) {
            // Extract :paramName patterns from SQL
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":(\\w+)");
            java.util.regex.Matcher matcher = pattern.matcher(sql);
            while (matcher.find()) {
                bindParams.add(matcher.group(1));
            }
        }
        
        public FindByKeyBuilder processor(Function<Object, Object> processor) {
            this.processor = processor;
            return this;
        }
        
        public FindByKeyBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public QueryDefinitionBuilder build() {
            CriteriaDef criteria = CriteriaDef.builder()
                .name(name)
                .sql(sql)
                .bindParams(ImmutableSet.copyOf(bindParams))
                .processor(processor)
                .description(description)
                .isFindByKey(true)
                .priority(-100)  // High priority to ensure it's applied first
                .build();
            
            parent.addCriteria(criteria);
            parent.setFindByKeyCriteriaName(name);
            return parent;
        }
    }
}