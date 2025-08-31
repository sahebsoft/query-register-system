package com.balasam.oasis.common.query.core.definition;

import lombok.Builder;
import lombok.Value;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;

import java.util.Map;
import java.util.List;
import java.util.function.Function;

/**
 * Immutable query definition containing all metadata and configuration
 */
@Value
@Builder(toBuilder = true)
public class QueryDefinition {
    String name;
    String sql;
    String description;
    
    @Builder.Default
    Map<String, AttributeDef> attributes = ImmutableMap.of();
    
    @Builder.Default
    Map<String, ParamDef> params = ImmutableMap.of();
    
    @Builder.Default
    Map<String, CriteriaDef> criteria = ImmutableMap.of();
    
    @Builder.Default
    List<Function<Object, Object>> preProcessors = ImmutableList.of();
    
    @Builder.Default
    List<Function<Object, Object>> rowProcessors = ImmutableList.of();
    
    @Builder.Default
    List<Function<Object, Object>> postProcessors = ImmutableList.of();
    
    @Builder.Default
    Map<String, Function<Object, Object>> calculators = ImmutableMap.of();
    
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
    
    public boolean hasCalculators() {
        return calculators != null && !calculators.isEmpty();
    }
    
    public boolean hasValidationRules() {
        return validationRules != null && !validationRules.isEmpty();
    }
    
    public boolean isCacheEnabled() {
        return cacheConfig != null && cacheConfig.isEnabled();
    }
    
    public AttributeDef getAttribute(String name) {
        return attributes.get(name);
    }
    
    public ParamDef getParam(String name) {
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
}