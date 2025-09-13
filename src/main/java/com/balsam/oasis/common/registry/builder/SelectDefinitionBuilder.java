package com.balsam.oasis.common.registry.builder;

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.processor.PostProcessor;
import com.balsam.oasis.common.registry.domain.processor.PreProcessor;
import com.balsam.oasis.common.registry.domain.processor.RowProcessor;
import com.balsam.oasis.common.registry.domain.select.LabelDef;
import com.balsam.oasis.common.registry.domain.select.ValueDef;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.google.common.base.Preconditions;

import java.time.Duration;
import java.util.function.Function;

/**
 * Builder for creating select-type QueryDefinition instances with fluent API.
 * This builder creates QueryDefinition objects that work with the Select API
 * by ensuring the first attribute is the value field and second is the label
 * field.
 * Leverages the full Query infrastructure while maintaining the Select API
 * pattern.
 */
public class SelectDefinitionBuilder {

    private final QueryDefinitionBuilder.Builder delegateBuilder;
    private boolean hasValueAttribute = false;
    private boolean hasLabelAttribute = false;

    private SelectDefinitionBuilder(String name) {
        this.delegateBuilder = QueryDefinitionBuilder.builder(name);
        // Set select-specific defaults
        this.delegateBuilder.defaultPageSize(100);
        this.delegateBuilder.paginationEnabled(true);
    }

    /**
     * Create a new builder with the given select name
     */
    public static SelectDefinitionBuilder builder(String name) {
        return new SelectDefinitionBuilder(name);
    }

    public SelectDefinitionBuilder valueAttribute(ValueDef valueDef) {
        this.delegateBuilder.attribute(valueDef.toAttributeDef());
        this.hasValueAttribute = true;
        return this;
    }

    public SelectDefinitionBuilder valueAttribute(String aliasName, Class<?> type) {
        this.delegateBuilder.attribute(ValueDef.of(aliasName, type).toAttributeDef());
        this.hasValueAttribute = true;
        return this;
    }

    public SelectDefinitionBuilder valueAttribute(String aliasName) {
        this.delegateBuilder.attribute(ValueDef.of(aliasName, String.class).toAttributeDef());
        this.hasValueAttribute = true;
        return this;
    }

    public SelectDefinitionBuilder labelAttribute(LabelDef labelDef) {
        this.delegateBuilder.attribute(labelDef.toAttributeDef());
        this.hasLabelAttribute = true;
        return this;
    }

    public SelectDefinitionBuilder labelAttribute(String aliasName, Class<?> type) {
        this.delegateBuilder.attribute(LabelDef.of(aliasName, type).toAttributeDef());
        this.hasLabelAttribute = true;
        return this;
    }

    public SelectDefinitionBuilder labelAttribute(String aliasName) {
        this.delegateBuilder.attribute(LabelDef.of(aliasName).toAttributeDef());
        this.hasLabelAttribute = true;
        return this;
    }

    // Delegating methods that return SelectDefinitionBuilder for fluent API

    public SelectDefinitionBuilder sql(String sql) {
        this.delegateBuilder.sql(sql);
        return this;
    }

    public SelectDefinitionBuilder description(String description) {
        this.delegateBuilder.description(description);
        return this;
    }

    public SelectDefinitionBuilder attribute(AttributeDef<?> attribute) {
        this.delegateBuilder.attribute(attribute);
        // Check if this is a value or label attribute
        if ("value".equals(attribute.name())) {
            this.hasValueAttribute = true;
        } else if ("label".equals(attribute.name())) {
            this.hasLabelAttribute = true;
        }
        return this;
    }

    public SelectDefinitionBuilder parameter(ParamDef<?> param) {
        this.delegateBuilder.parameter(param);
        return this;
    }

    public SelectDefinitionBuilder criteria(CriteriaDef criteria) {
        this.delegateBuilder.criteria(criteria);
        return this;
    }

    public SelectDefinitionBuilder preProcessor(PreProcessor processor) {
        this.delegateBuilder.preProcessor(processor);
        return this;
    }

    public SelectDefinitionBuilder rowProcessor(RowProcessor processor) {
        this.delegateBuilder.rowProcessor(processor);
        return this;
    }

    public SelectDefinitionBuilder postProcessor(PostProcessor processor) {
        this.delegateBuilder.postProcessor(processor);
        return this;
    }

    public SelectDefinitionBuilder defaultPageSize(Integer size) {
        this.delegateBuilder.defaultPageSize(size);
        return this;
    }

    public SelectDefinitionBuilder maxPageSize(Integer size) {
        this.delegateBuilder.maxPageSize(size);
        return this;
    }

    public SelectDefinitionBuilder paginationEnabled(Boolean enabled) {
        this.delegateBuilder.paginationEnabled(enabled);
        return this;
    }

    public SelectDefinitionBuilder fetchSize(Integer size) {
        this.delegateBuilder.fetchSize(size);
        return this;
    }

    // Cache configuration methods
    public SelectDefinitionBuilder cache(Boolean enabled) {
        this.delegateBuilder.cache(enabled);
        return this;
    }

    public SelectDefinitionBuilder cacheTTL(Duration ttl) {
        this.delegateBuilder.cacheTTL(ttl);
        return this;
    }

    public SelectDefinitionBuilder cacheKey(Function<Object, String> keyGenerator) {
        this.delegateBuilder.cacheKey(keyGenerator);
        return this;
    }

    // Other configuration methods
    public SelectDefinitionBuilder auditEnabled(Boolean enabled) {
        this.delegateBuilder.auditEnabled(enabled);
        return this;
    }

    public SelectDefinitionBuilder metricsEnabled(Boolean enabled) {
        this.delegateBuilder.metricsEnabled(enabled);
        return this;
    }

    public SelectDefinitionBuilder queryTimeout(Integer seconds) {
        this.delegateBuilder.queryTimeout(seconds);
        return this;
    }

    // Dynamic attributes configuration
    public SelectDefinitionBuilder dynamic() {
        this.delegateBuilder.dynamic();
        return this;
    }

    public SelectDefinitionBuilder dynamic(NamingStrategy strategy) {
        this.delegateBuilder.dynamic(strategy);
        return this;
    }

    public QueryDefinitionBuilder build() {
        // Validate that value and label attributes are defined
        Preconditions.checkState(hasValueAttribute,
                "Value attribute must be defined. Use valueAttribute() to set it.");
        Preconditions.checkState(hasLabelAttribute,
                "Label attribute must be defined. Use labelAttribute() to set it.");

        return this.delegateBuilder.build();
    }
}