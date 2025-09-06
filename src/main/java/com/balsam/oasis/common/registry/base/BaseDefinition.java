package com.balsam.oasis.common.registry.base;

import com.balsam.oasis.common.registry.core.definition.CacheConfig;
import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.core.definition.ValidationRule;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Data
@SuperBuilder
public abstract class BaseDefinition {
    protected String name;
    protected String description;
    protected String sql;
    protected Map<String, ParamDef<?>> params;
    protected Map<String, CriteriaDef> criteria;
    protected List<Function<Object, Object>> preProcessors;
    protected List<Function<Object, Object>> rowProcessors;
    protected List<Function<Object, Object>> postProcessors;
    protected List<ValidationRule> validationRules;
    protected CacheConfig cacheConfig;
    @Builder.Default
    protected int defaultPageSize = 100;
    @Builder.Default
    protected int maxPageSize = 1000;
    @Builder.Default
    protected boolean auditEnabled = true;
    @Builder.Default
    protected boolean metricsEnabled = true;
    protected Integer queryTimeout;
    
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
}