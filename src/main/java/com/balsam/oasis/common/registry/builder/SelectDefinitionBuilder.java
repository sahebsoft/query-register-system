package com.balsam.oasis.common.registry.builder;

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.processor.PostProcessor;
import com.balsam.oasis.common.registry.domain.processor.PreProcessor;
import com.balsam.oasis.common.registry.domain.processor.RowProcessor;
import com.balsam.oasis.common.registry.domain.select.LabelDef;
import com.balsam.oasis.common.registry.domain.select.ValueDef;
import com.google.common.base.Preconditions;

/**
 * Builder for creating select-type QueryDefinition instances with fluent API.
 * This builder creates QueryDefinition objects that work with the Select API
 * by ensuring the first attribute is the value field and second is the label
 * field.
 * Leverages the full Query infrastructure while maintaining the Select API
 * pattern.
 */
public class SelectDefinitionBuilder extends QueryDefinitionBuilder {

    private SelectDefinitionBuilder(String name) {
        super(name);
        // Set select-specific defaults
        super.defaultPageSize(100);
        super.paginationEnabled(true);
    }

    /**
     * Create a new builder with the given select name
     */
    public static SelectDefinitionBuilder builder(String name) {
        return new SelectDefinitionBuilder(name);
    }

    public SelectDefinitionBuilder valueAttribute(ValueDef valueDef) {
        super.attribute(valueDef.toAttributeDef());
        return this;
    }

    public SelectDefinitionBuilder valueAttribute(String aliasName, Class<?> type) {
        super.attribute(ValueDef.of(aliasName, type).toAttributeDef());
        return this;
    }

    public SelectDefinitionBuilder valueAttribute(String aliasName) {
        super.attribute(ValueDef.of(aliasName, String.class).toAttributeDef());
        return this;
    }

    public SelectDefinitionBuilder labelAttribute(LabelDef labelDef) {
        super.attribute(labelDef.toAttributeDef());
        return this;
    }

    public SelectDefinitionBuilder labelAttribute(String aliasName, Class<?> type) {
        super.attribute(LabelDef.of(aliasName, type).toAttributeDef());
        return this;
    }

    public SelectDefinitionBuilder labelAttribute(String aliasName) {
        super.attribute(LabelDef.of(aliasName).toAttributeDef());
        return this;
    }

    // Override methods to return SelectDefinitionBuilder for fluent API

    @Override
    public SelectDefinitionBuilder sql(String sql) {
        super.sql(sql);
        return this;
    }

    @Override
    public SelectDefinitionBuilder description(String description) {
        super.description(description);
        return this;
    }

    @Override
    public SelectDefinitionBuilder attribute(
            AttributeDef<?> attribute) {
        super.attribute(attribute);
        return this;
    }

    @Override
    public SelectDefinitionBuilder parameter(ParamDef param) {
        super.parameter(param);
        return this;
    }

    @Override
    public SelectDefinitionBuilder criteria(CriteriaDef criteria) {
        super.criteria(criteria);
        return this;
    }

    @Override
    public SelectDefinitionBuilder preProcessor(
            PreProcessor processor) {
        super.preProcessor(processor);
        return this;
    }

    @Override
    public SelectDefinitionBuilder rowProcessor(
            RowProcessor processor) {
        super.rowProcessor(processor);
        return this;
    }

    @Override
    public SelectDefinitionBuilder postProcessor(
            PostProcessor processor) {
        super.postProcessor(processor);
        return this;
    }

    @Override
    public SelectDefinitionBuilder defaultPageSize(Integer size) {
        super.defaultPageSize(size);
        return this;
    }

    @Override
    public SelectDefinitionBuilder maxPageSize(Integer size) {
        super.maxPageSize(size);
        return this;
    }

    @Override
    public QueryDefinition build() {
        // Validate that value and label attributes are defined
        boolean hasValue = attributes.containsKey("value");
        boolean hasLabel = attributes.containsKey("label");

        Preconditions.checkState(hasValue,
                "Value attribute must be defined. Use valueAttribute() to set it.");
        Preconditions.checkState(hasLabel,
                "Label attribute must be defined. Use labelAttribute() to set it.");

        return super.build();
    }
}