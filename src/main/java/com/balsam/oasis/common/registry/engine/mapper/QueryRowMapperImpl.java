package com.balsam.oasis.common.registry.engine.mapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.result.Row;
import com.balsam.oasis.common.registry.domain.result.RowImpl;
import com.balsam.oasis.common.registry.engine.sql.BaseRowMapper;

/**
 * Query-specific implementation of BaseRowMapper that produces Row objects.
 */
public class QueryRowMapperImpl extends BaseRowMapper<Row> {

    @Override
    protected Map<String, AttributeDef<?>> getAttributesToProcess(QueryDefinition definition) {
        // For Query, process all attributes defined in the definition
        return definition.getAttributes();
    }

    @Override
    protected Row createIntermediateOutput(Map<String, Object> processedData,
            Map<String, Object> rawData, QueryContext context) {
        // Create a Row that can be used in calculators
        return new RowImpl(processedData, rawData, context);
    }

    @Override
    protected Row createFinalOutput(Map<String, Object> processedData,
            Map<String, Object> rawData, QueryContext context) {
        // For Query, the final output is the same Row object
        return new RowImpl(processedData, rawData, context);
    }

    @Override
    protected Object getValueFromOutput(Row output, String attributeName) {
        return output.get(attributeName);
    }

    @Override
    protected void setValueInOutput(Row output, String attributeName, Object value) {
        output.set(attributeName, value);
    }

    @Override
    protected Object calculateAttribute(AttributeDef<?> attr, Row intermediateResult, QueryContext context) {
        // Calculator now expects Row and QueryContext
        return attr.getCalculator().calculate(intermediateResult, context);
    }

    @Override
    protected void addDynamicAttributes(Map<String, Object> processedData,
            Map<String, Object> rawData,
            Map<String, AttributeDef<?>> definedAttributes,
            QueryDefinition definition) {
        if (rawData == null || rawData.isEmpty()) {
            return;
        }

        // Get the naming strategy
        NamingStrategy namingStrategy = definition.getDynamicAttributeNamingStrategy();

        // Create a set of defined attribute names and their aliases for quick lookup
        // (uppercase)
        Set<String> definedNames = new HashSet<>();
        for (Map.Entry<String, AttributeDef<?>> entry : definedAttributes.entrySet()) {
            definedNames.add(entry.getKey().toUpperCase());
            AttributeDef<?> attr = entry.getValue();
            if (attr.getAliasName() != null) {
                definedNames.add(attr.getAliasName().toUpperCase());
            }
        }

        // Add undefined columns with naming strategy
        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            String columnName = entry.getKey(); // Already uppercase from BaseRowMapper

            // Skip if this column is already defined
            if (definedNames.contains(columnName)) {
                continue;
            }

            // Apply naming strategy to get the attribute name
            String attributeName = namingStrategy.convert(columnName);

            // Skip if the converted name conflicts with an existing attribute
            if (processedData.containsKey(attributeName)) {
                continue;
            }

            // Add the dynamic attribute
            processedData.put(attributeName, entry.getValue());
        }
    }
}