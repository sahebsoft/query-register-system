package com.balsam.oasis.common.registry.web.formatter;

import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import java.util.Map;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ColumnNameTransformer {

    /**
     * Transform raw column names to user-friendly names based on naming strategy.
     * Used for dynamic attributes that come from database columns.
     */
    public Map<String, Object> transformColumnNames(Map<String, Object> rawData, NamingStrategy strategy) {
        if (strategy == null || strategy == NamingStrategy.AS_IS) {
            return rawData;
        }

        Map<String, Object> transformedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            String columnName = entry.getKey();
            String transformedName = strategy.convert(columnName);
            transformedData.put(transformedName, entry.getValue());
        }

        return transformedData;
    }

    /**
     * Transform a single column name based on naming strategy.
     */
    public String transformColumnName(String columnName, NamingStrategy strategy) {
        if (strategy == null || strategy == NamingStrategy.AS_IS) {
            return columnName;
        }
        return strategy.convert(columnName);
    }
}