package com.balsam.oasis.common.registry.engine.sql;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.sql.util.SqlTypeMapper;
import com.balsam.oasis.common.registry.engine.sql.util.TypeConverter;
import com.balsam.oasis.common.registry.processor.AttributeFormatter;

/**
 * Base row mapper that provides comprehensive attribute processing including
 * regular
 * attributes, transient attributes, formatters, calculators, and security
 * rules.
 * Uses cached indexes when available for performance, falls back to name-based
 * access otherwise.
 * 
 * @param <T> The output type (Row, SelectItem, etc.)
 */
public abstract class BaseRowMapper<T> implements RowMapper<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseRowMapper.class);

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        // This method is required by RowMapper interface but we need context
        throw new UnsupportedOperationException("Use mapRow(ResultSet, int, Context) instead");
    }

    /**
     * Map a ResultSet row with full context support.
     */
    public T mapRow(ResultSet rs, int rowNum, QueryContext context) throws SQLException {
        QueryDefinition definition = context.getDefinition();
        MetadataCache cache = definition.getMetadataCache();

        Map<String, Object> processedData = new HashMap<>();
        Map<String, Object> rawData = extractRawData(rs, cache);

        // Get all attributes to process
        Map<String, AttributeDef<?>> attributes = definition.getAttributes();

        // First pass: Map regular (non-transient) attributes from database
        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip transient attributes (they're calculated later)
            if (attr.isVirual()) {
                continue;
            }

            // Skip if attribute not selected
            if (!isAttributeIncluded(attr, attrName, context)) {
                continue;
            }

            // Check security
            if (attr.isSecured() && hasSecurityContext(context)) {
                Object securityContext = getSecurityContext(context);
                Boolean allowed = attr.getSecurityRule().apply(securityContext);
                if (!Boolean.TRUE.equals(allowed)) {
                    // Attribute is restricted - set to null
                    processedData.put(attrName, null);
                    continue;
                }
            }

            // Get value from ResultSet
            Object rawValue = extractAttributeValue(rs, attr, cache, rawData);

            // Apply automatic type conversion
            Object convertedValue = rawValue;
            if (rawValue != null && attr.getType() != null) {
                convertedValue = convertToType(rawValue, attr.getType());
            }

            processedData.put(attrName, convertedValue);
        }

        // Create intermediate object for calculator context
        T intermediateResult = createIntermediateOutput(processedData, rawData, context);

        // Second pass: Apply formatters to regular attributes
        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip transient attributes
            if (attr.isVirual()) {
                continue;
            }

            // Skip if attribute not selected
            if (!isAttributeIncluded(attr, attrName, context)) {
                continue;
            }

            // Apply formatter if present (converts to String)
            if (attr.hasFormatter()) {
                try {
                    Object currentValue = getValueFromOutput(intermediateResult, attrName);
                    if (currentValue != null) {
                        AttributeFormatter formatter = attr.getFormatter();
                        String formattedValue = formatter.format(currentValue);
                        setValueInOutput(intermediateResult, attrName, formattedValue);
                        processedData.put(attrName, formattedValue);
                    }
                } catch (Exception e) {
                    log.warn("Failed to format value for attribute {}: {}", attrName, e.getMessage());
                }
            }
        }

        // Third pass: Calculate transient attributes
        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Only process transient attributes
            if (!attr.isVirual()) {
                continue;
            }

            // Skip if attribute not selected
            if (!isAttributeIncluded(attr, attrName, context)) {
                log.debug("Skipping unselected field: {}", attrName);
                continue;
            }

            // Check security for transient attributes
            if (attr.isSecured() && hasSecurityContext(context)) {
                Object securityContext = getSecurityContext(context);
                Boolean allowed = attr.getSecurityRule().apply(securityContext);
                if (!Boolean.TRUE.equals(allowed)) {
                    setValueInOutput(intermediateResult, attrName, null);
                    processedData.put(attrName, null);
                    continue;
                }
            }

            // Calculate the transient value
            if (attr.hasCalculator()) {
                try {
                    Object calculatedValue = calculateAttribute(attr, intermediateResult, context);

                    // Ensure type safety
                    if (calculatedValue != null && attr.getType() != null) {
                        calculatedValue = convertToType(calculatedValue, attr.getType());
                    }

                    setValueInOutput(intermediateResult, attrName, calculatedValue);
                    processedData.put(attrName, calculatedValue);
                } catch (Exception e) {
                    log.warn("Failed to calculate transient attribute {}: {}", attrName, e.getMessage());
                    setValueInOutput(intermediateResult, attrName, null);
                    processedData.put(attrName, null);
                }
            }
        }

        // Final pass: Apply formatters to transient attributes
        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Only process transient attributes with formatters
            if (!attr.isVirual() || !attr.hasFormatter()) {
                continue;
            }

            // Skip if attribute not selected
            if (!isAttributeIncluded(attr, attrName, context)) {
                continue;
            }

            try {
                Object currentValue = getValueFromOutput(intermediateResult, attrName);
                if (currentValue != null) {
                    AttributeFormatter formatter = attr.getFormatter();
                    String formattedValue = formatter.format(currentValue);
                    setValueInOutput(intermediateResult, attrName, formattedValue);
                    processedData.put(attrName, formattedValue);
                }
            } catch (Exception e) {
                log.warn("Failed to format transient attribute {}: {}", attrName, e.getMessage());
            }
        }

        // Add dynamic attributes if enabled
        if (shouldIncludeDynamicAttributes(definition)) {
            addDynamicAttributes(processedData, rawData, attributes, definition);
        }

        // Create final output
        return createFinalOutput(processedData, rawData, context);
    }

    /**
     * Check if an attribute should be included in the result based on:
     * 1. The attribute's selected field (if false, exclude unless explicitly
     * requested)
     * 2. The context's field selection (if specified, only include selected fields)
     */
    protected boolean isAttributeIncluded(AttributeDef<?> attr, String attrName, QueryContext context) {
        // If attribute has selected=false, only include if explicitly requested
        if (!attr.isSelected()) {
            return context.getSelectedFields() != null &&
                    context.getSelectedFields().contains(attrName);
        }

        // Otherwise, include based on field selection
        return context.isFieldSelected(attrName);
    }

    /**
     * Extract raw column data from ResultSet into a map.
     * Uses cache for index-based access when available, otherwise uses metadata.
     */
    protected Map<String, Object> extractRawData(ResultSet rs, MetadataCache cache) throws SQLException {
        Map<String, Object> rawData = new HashMap<>();

        if (cache != null && cache.isInitialized()) {
            // Optimized path: use cached metadata
            String[] columnNames = cache.getColumnNames();
            String[] columnLabels = cache.getColumnLabels();

            for (int i = 0; i < columnNames.length; i++) {
                int columnIndex = i + 1; // JDBC uses 1-based indexing
                Integer sqlType = cache.getColumnType(columnIndex);
                Object value = extractValueByIndex(rs, columnIndex, sqlType != null ? sqlType : Types.OTHER);

                // Store with uppercase keys (Oracle standard)
                String columnName = columnNames[i].toUpperCase();
                rawData.put(columnName, value);

                if (columnLabels != null && i < columnLabels.length) {
                    String label = columnLabels[i].toUpperCase();
                    if (!columnName.equals(label)) {
                        rawData.put(label, value);
                    }
                }
            }
        } else {
            // Fallback path: use ResultSetMetaData
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i).toUpperCase();
                String columnLabel = metaData.getColumnLabel(i).toUpperCase();
                Object value = extractValueByIndex(rs, i, metaData.getColumnType(i));

                rawData.put(columnName, value);
                if (!columnName.equals(columnLabel)) {
                    rawData.put(columnLabel, value);
                }
            }
        }

        return rawData;
    }

    /**
     * Extract value for a specific attribute using the most efficient method
     * available.
     */
    protected Object extractAttributeValue(ResultSet rs, AttributeDef<?> attr,
            MetadataCache cache, Map<String, Object> rawData) throws SQLException {
        // First try to get from raw data if available
        if (rawData != null && attr.getAliasName() != null) {
            // Use uppercase for lookup (Oracle standard)
            String aliasName = attr.getAliasName().toUpperCase();
            if (rawData.containsKey(aliasName)) {
                return rawData.get(aliasName);
            }
        }

        // If cache is available, use index-based access
        if (cache != null && cache.isInitialized()) {
            Integer columnIndex = cache.getColumnIndexForAttribute(attr.getName());
            if (columnIndex == null && attr.getAliasName() != null) {
                columnIndex = cache.getColumnIndex(attr.getAliasName());
            }

            if (columnIndex != null) {
                Integer sqlType = cache.getColumnType(columnIndex);
                return extractValueByIndex(rs, columnIndex, sqlType != null ? sqlType : Types.OTHER);
            }
        }

        // Fallback to name-based access
        if (attr.getAliasName() != null) {
            try {
                return rs.getObject(attr.getAliasName());
            } catch (SQLException e) {
                log.trace("Column not found: {}", attr.getAliasName());
                return null;
            }
        }

        return null;
    }

    /**
     * Extract value by column index with proper type handling.
     * Uses SqlTypeMapper to determine the expected Java type for consistency.
     */
    protected Object extractValueByIndex(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        // Check for NULL first
        rs.getObject(columnIndex);
        if (rs.wasNull()) {
            return null;
        }

        // Use SqlTypeMapper to determine expected Java type
        Class<?> expectedType = SqlTypeMapper.sqlTypeToJavaClass(sqlType);

        // Handle types based on expected Java type for best performance
        if (expectedType == String.class) {
            return rs.getString(columnIndex);
        } else if (expectedType == Integer.class) {
            return rs.getInt(columnIndex);
        } else if (expectedType == Long.class) {
            return rs.getLong(columnIndex);
        } else if (expectedType == java.math.BigDecimal.class) {
            return rs.getBigDecimal(columnIndex);
        } else if (expectedType == Float.class) {
            return rs.getFloat(columnIndex);
        } else if (expectedType == Double.class) {
            return rs.getDouble(columnIndex);
        } else if (expectedType == Boolean.class) {
            return rs.getBoolean(columnIndex);
        } else if (expectedType == java.time.LocalDate.class) {
            Date date = rs.getDate(columnIndex);
            return date != null ? date.toLocalDate() : null;
        } else if (expectedType == java.time.LocalTime.class) {
            Time time = rs.getTime(columnIndex);
            return time != null ? time.toLocalTime() : null;
        } else if (expectedType == java.time.LocalDateTime.class) {
            Timestamp timestamp = rs.getTimestamp(columnIndex);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        } else if (expectedType == byte[].class) {
            // Handle BLOB and binary types
            if (sqlType == Types.BLOB) {
                Blob blob = rs.getBlob(columnIndex);
                if (blob != null) {
                    try {
                        return blob.getBytes(1, (int) blob.length());
                    } catch (SQLException e) {
                        log.warn("Error reading BLOB at column {}: {}", columnIndex, e.getMessage());
                        return blob;
                    }
                }
                return null;
            } else {
                return rs.getBytes(columnIndex);
            }
        } else if (sqlType == Types.CLOB || sqlType == Types.NCLOB) {
            // Special handling for CLOBs
            Clob clob = rs.getClob(columnIndex);
            if (clob != null) {
                try {
                    return clob.getSubString(1, (int) clob.length());
                } catch (SQLException e) {
                    log.warn("Error reading CLOB at column {}: {}", columnIndex, e.getMessage());
                    return clob;
                }
            }
            return null;
        } else if (sqlType == Types.ARRAY) {
            return rs.getArray(columnIndex);
        } else {
            // Fall back to generic getObject for unknown types
            return rs.getObject(columnIndex);
        }
    }

    /**
     * Convert value to target type using centralized TypeConverter.
     */
    protected Object convertToType(Object value, Class<?> targetType) {
        if (value == null || targetType == null) {
            return value;
        }

        // If already the target type, return as-is
        if (targetType.isInstance(value)) {
            return value;
        }

        try {
            return TypeConverter.convert(value, targetType);
        } catch (Exception e) {
            log.warn("Failed to convert value {} to type {}: {}",
                    value, targetType.getSimpleName(), e.getMessage());
            return value;
        }
    }

    /**
     * Get all attributes to process from the definition.
     * Subclasses can override to customize which attributes are processed.
     */
    protected abstract Map<String, AttributeDef<?>> getAttributesToProcess(QueryDefinition definition);

    /**
     * Create intermediate output object for use in calculators.
     * This may be the same as the final output for some implementations.
     */
    protected abstract T createIntermediateOutput(Map<String, Object> processedData,
            Map<String, Object> rawData, QueryContext context);

    /**
     * Create the final output object.
     */
    protected abstract T createFinalOutput(Map<String, Object> processedData,
            Map<String, Object> rawData, QueryContext context);

    /**
     * Get a value from the output object by attribute name.
     * Used for formatters and calculators.
     */
    protected abstract Object getValueFromOutput(T output, String attributeName);

    /**
     * Set a value in the output object.
     * Used for formatters and calculators.
     */
    protected abstract void setValueInOutput(T output, String attributeName, Object value);

    /**
     * Check if the context has security context.
     * Override in subclasses that support security.
     */
    protected boolean hasSecurityContext(QueryContext context) {
        return context.getSecurityContext() != null;
    }

    /**
     * Get security context from context.
     * Override in subclasses that support security.
     */
    protected Object getSecurityContext(QueryContext context) {
        return context.getSecurityContext();
    }

    /**
     * Calculate attribute value using the calculator.
     * Override in subclasses to handle specific calculator types.
     */
    protected abstract Object calculateAttribute(AttributeDef<?> attr, T intermediateResult, QueryContext context);

    /**
     * Get MetadataCache from context if available.
     * Subclasses can override to provide cache from different sources.
     */

    /**
     * Check if dynamic attributes should be included.
     * Subclasses can override to provide custom logic.
     */
    protected boolean shouldIncludeDynamicAttributes(QueryDefinition definition) {
        return definition.isIncludeDynamicAttributes();
    }

    /**
     * Add dynamic attributes (columns not defined in AttributeDef) to the processed
     * data.
     * These are columns returned by the SQL query but not explicitly defined.
     */
    protected void addDynamicAttributes(Map<String, Object> processedData,
            Map<String, Object> rawData,
            Map<String, AttributeDef<?>> definedAttributes,
            QueryDefinition definition) {
        // Default implementation does nothing
        // Subclasses should override this to add dynamic attributes
    }
}