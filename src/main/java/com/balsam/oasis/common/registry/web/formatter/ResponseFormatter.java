package com.balsam.oasis.common.registry.web.formatter;

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.processor.AttributeFormatter;
import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseFormatter {

    private final ColumnNameTransformer nameTransformer = new ColumnNameTransformer();

    public List<Map<String, Object>> formatRows(List<QueryRow> rows, QueryDefinitionBuilder definition, Object securityContext) {
        List<Map<String, Object>> formattedData = new ArrayList<>();

        for (QueryRow row : rows) {
            Map<String, Object> formattedRow = formatRow(row, definition, securityContext);
            formattedData.add(formattedRow);
        }

        return formattedData;
    }

    public Map<String, Object> formatRow(QueryRow row, QueryDefinitionBuilder definition, Object securityContext) {
        Map<String, Object> formattedData = new HashMap<>();
        Set<String> definedAttributes = new HashSet<>();

        // Process defined attributes
        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();
            definedAttributes.add(attrName.toUpperCase());

            // Apply security
            if (attr.isSecured() && securityContext != null) {
                Boolean allowed = attr.securityRule().apply(securityContext);
                if (!Boolean.TRUE.equals(allowed)) {
                    formattedData.put(attrName, null);
                    continue;
                }
            }

            // Get value from raw data using column name or attribute name
            Object value;
            if (attr.aliasName() != null && !attr.virtual()) {
                // For mapped attributes, get from raw data using column name
                value = row.getRaw(attr.aliasName());
            } else if (attr.virtual()) {
                // For virtual attributes, get calculated value
                value = row.get(attrName);
            } else {
                // For attributes without aliasName, try uppercase attribute name
                value = row.getRaw(attrName.toUpperCase());
                if (value == null) {
                    value = row.get(attrName);
                }
            }

            // Apply formatter
            if (value != null && attr.hasFormatter()) {
                try {
                    AttributeFormatter formatter = attr.formatter();
                    value = formatter.format(value);
                } catch (Exception e) {
                    log.warn("Failed to format attribute {}: {}", attrName, e.getMessage());
                }
            }

            formattedData.put(attrName, value);
        }

        // Add dynamic attributes if enabled
        if (definition.isDynamic()) {
            Map<String, Object> rawData = row.toMap();
            NamingStrategy strategy = definition.getNamingStrategy();

            // Add all unmapped columns as dynamic attributes
            for (Map.Entry<String, Object> rawEntry : rawData.entrySet()) {
                String columnName = rawEntry.getKey();
                if (!definedAttributes.contains(columnName.toUpperCase())) {
                    String transformedName = nameTransformer.transformColumnName(columnName, strategy);
                    if (!formattedData.containsKey(transformedName)) {
                        formattedData.put(transformedName, rawEntry.getValue());
                    }
                }
            }
        }

        return formattedData;
    }
}