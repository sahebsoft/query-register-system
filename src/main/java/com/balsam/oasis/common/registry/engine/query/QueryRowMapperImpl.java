package com.balsam.oasis.common.registry.engine.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.sql.MetadataCache;
import com.balsam.oasis.common.registry.engine.sql.MetadataOperations;

/**
 * Simple row mapper that extracts raw data and applies calculators.
 * All formatting, security, and naming transformations are handled in REST layer.
 */
@Slf4j
public class QueryRowMapperImpl {

    /**
     * Map a ResultSet row to QueryRow with minimal processing.
     */
    public QueryRow mapRow(ResultSet rs, int rowNum, QueryContext context) throws SQLException {
        QueryDefinitionBuilder definition = context.getDefinition();
        MetadataCache cache = definition.getMetadataCache();

        // Extract all raw data from ResultSet
        Map<String, Object> rawData = MetadataOperations.extractRawData(rs, cache);

        // Create QueryRow with raw data
        QueryRow row = QueryRow.create(rawData, rawData, context);

        // Apply calculators for virtual attributes if any
        if (definition.hasAttributes()) {
            for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
                AttributeDef<?> attr = entry.getValue();
                if (attr.virtual() && attr.hasCalculator()) {
                    try {
                        Object calculatedValue = attr.calculator().calculate(row, context);
                        row.set(entry.getKey(), calculatedValue);
                    } catch (Exception e) {
                        log.warn("Failed to calculate virtual attribute {}: {}", entry.getKey(), e.getMessage());
                        row.set(entry.getKey(), null);
                    }
                }
            }
        }

        return row;
    }
}