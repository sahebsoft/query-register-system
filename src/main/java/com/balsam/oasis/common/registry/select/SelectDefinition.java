package com.balsam.oasis.common.registry.select;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.CacheConfig;
import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.core.definition.ValidationRule;

/**
 * Definition for a Select query used in dropdown/select components.
 * Now includes processing pipeline support similar to QueryDefinition.
 */
@Value
@Builder
public class SelectDefinition {

    /**
     * Unique name for this select definition
     */
    String name;

    /**
     * Optional description
     */
    String description;

    /**
     * SQL query with comment placeholders for criteria
     */
    String sql;

    /**
     * The attribute to use as the value (e.g., employee_id)
     */
    AttributeDef<?> valueAttribute;

    /**
     * The attribute to use as the label (e.g., full_name)
     */
    AttributeDef<?> labelAttribute;

    /**
     * Additional attributes to include in the additions field
     */
    @Builder.Default
    List<AttributeDef<?>> additionAttributes = List.of();

    /**
     * Parameters that can be passed to this select
     */
    @Builder.Default
    Map<String, ParamDef<?>> params = Map.of();

    /**
     * Criteria that can be applied to this select
     */
    @Builder.Default
    Map<String, CriteriaDef> criteria = Map.of();

    /**
     * Optional dedicated search criteria.
     * If not defined, search will auto-wrap the query with label LIKE condition
     */
    CriteriaDef searchCriteria;

    /**
     * Processing pipeline - pre-processors
     */
    @Builder.Default
    List<Function<Object, Object>> preProcessors = List.of();

    /**
     * Processing pipeline - row processors
     */
    @Builder.Default
    List<Function<Object, Object>> rowProcessors = List.of();

    /**
     * Processing pipeline - post-processors
     */
    @Builder.Default
    List<Function<Object, Object>> postProcessors = List.of();

    /**
     * Validation rules for the select
     */
    @Builder.Default
    List<ValidationRule> validationRules = List.of();

    /**
     * Cache configuration
     */
    CacheConfig cacheConfig;

    /**
     * Default page size for pagination
     */
    @Builder.Default
    int defaultPageSize = 100;

    /**
     * Maximum allowed page size
     */
    @Builder.Default
    int maxPageSize = 1000;

    /**
     * Enable/disable audit logging
     */
    @Builder.Default
    boolean auditEnabled = true;

    /**
     * Enable/disable metrics collection
     */
    @Builder.Default
    boolean metricsEnabled = true;

    /**
     * Query timeout in seconds
     */
    Integer queryTimeout;

    /**
     * Check if this select has parameters
     */
    public boolean hasParams() {
        return params != null && !params.isEmpty();
    }

    /**
     * Check if this select has criteria
     */
    public boolean hasCriteria() {
        return criteria != null && !criteria.isEmpty();
    }

    /**
     * Check if this select has search criteria defined
     */
    public boolean hasSearchCriteria() {
        return searchCriteria != null;
    }

    /**
     * Check if this select has additional attributes
     */
    public boolean hasAdditions() {
        return additionAttributes != null && !additionAttributes.isEmpty();
    }

    /**
     * Get a parameter by name
     */
    public ParamDef<?> getParam(String name) {
        return params != null ? params.get(name) : null;
    }

    /**
     * Get a criteria by name
     */
    public CriteriaDef getCriteria(String name) {
        return criteria != null ? criteria.get(name) : null;
    }

    /**
     * Check if this select has pre-processors
     */
    public boolean hasPreProcessors() {
        return preProcessors != null && !preProcessors.isEmpty();
    }

    /**
     * Check if this select has row processors
     */
    public boolean hasRowProcessors() {
        return rowProcessors != null && !rowProcessors.isEmpty();
    }

    /**
     * Check if this select has post-processors
     */
    public boolean hasPostProcessors() {
        return postProcessors != null && !postProcessors.isEmpty();
    }

    /**
     * Check if this select has validation rules
     */
    public boolean hasValidationRules() {
        return validationRules != null && !validationRules.isEmpty();
    }

    /**
     * Check if caching is enabled
     */
    public boolean isCacheEnabled() {
        return cacheConfig != null && cacheConfig.isEnabled();
    }
}