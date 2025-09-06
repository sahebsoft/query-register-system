package com.balsam.oasis.common.registry.select;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for creating SelectDefinition instances with fluent API
 */
public class SelectDefinitionBuilder {

    private String name;
    private String description;
    private String sql;
    private AttributeDef<?> valueAttribute;
    private AttributeDef<?> labelAttribute;
    private final List<AttributeDef<?>> additionAttributes = new ArrayList<>();
    private final Map<String, ParamDef<?>> params = new HashMap<>();
    private final Map<String, CriteriaDef> criteria = new HashMap<>();
    private CriteriaDef searchCriteria;
    private int defaultPageSize = 100;
    private int maxPageSize = 1000;

    private SelectDefinitionBuilder(String name) {
        this.name = name;
    }

    /**
     * Create a new builder with the given select name
     */
    public static SelectDefinitionBuilder builder(String name) {
        Preconditions.checkNotNull(name, "Select name cannot be null");
        Preconditions.checkArgument(!name.trim().isEmpty(), "Select name cannot be empty");
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
     * Set the value attribute
     */
    public SelectDefinitionBuilder value(AttributeDef<?> valueAttribute) {
        Preconditions.checkNotNull(valueAttribute, "Value attribute cannot be null");
        this.valueAttribute = valueAttribute;
        return this;
    }

    /**
     * Set the label attribute
     */
    public SelectDefinitionBuilder label(AttributeDef<?> labelAttribute) {
        Preconditions.checkNotNull(labelAttribute, "Label attribute cannot be null");
        this.labelAttribute = labelAttribute;
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
     * Add multiple additional attributes
     */
    public SelectDefinitionBuilder additions(AttributeDef<?>... attributes) {
        for (AttributeDef<?> attr : attributes) {
            addition(attr);
        }
        return this;
    }

    /**
     * Set the search criteria (optional)
     * If not set, search will auto-wrap the query with label LIKE condition
     */
    public SelectDefinitionBuilder searchCriteria(CriteriaDef searchCriteria) {
        Preconditions.checkNotNull(searchCriteria, "Search criteria cannot be null");
        this.searchCriteria = searchCriteria;
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
     * Add a parameter
     */
    public SelectDefinitionBuilder param(ParamDef<?> param) {
        Preconditions.checkNotNull(param, "Parameter cannot be null");
        Preconditions.checkNotNull(param.getName(), "Parameter name cannot be null");
        this.params.put(param.getName(), param);
        return this;
    }

    /**
     * Add multiple parameters
     */
    public SelectDefinitionBuilder params(ParamDef<?>... params) {
        for (ParamDef<?> param : params) {
            param(param);
        }
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
     * Build the SelectDefinition
     */
    public SelectDefinition build() {
        // Validation
        Preconditions.checkNotNull(sql, "SQL must be set");
        Preconditions.checkNotNull(valueAttribute, "Value attribute must be set");
        Preconditions.checkNotNull(labelAttribute, "Label attribute must be set");

        // If searchCriteria is defined, ensure we have a search parameter
        if (searchCriteria != null && !params.containsKey("search")) {
            // Auto-add search parameter
            params.put("search", ParamDef.param("search")
                    .type(String.class)
                    .description("Search term")
                    .required(false)
                    .build());
        }

        return SelectDefinition.builder()
                .name(name)
                .description(description)
                .sql(sql)
                .valueAttribute(valueAttribute)
                .labelAttribute(labelAttribute)
                .additionAttributes(new ArrayList<>(additionAttributes))
                .params(new HashMap<>(params))
                .criteria(new HashMap<>(criteria))
                .searchCriteria(searchCriteria)
                .defaultPageSize(defaultPageSize)
                .maxPageSize(maxPageSize)
                .build();
    }
}