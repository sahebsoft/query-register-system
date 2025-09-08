package com.balsam.oasis.common.registry.base;

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

import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.engine.metadata.MetadataCache;
import com.balsam.oasis.common.registry.processor.AttributeFormatter;
import com.balsam.oasis.common.registry.util.TypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

/**
 * Base row mapper that provides comprehensive attribute processing including
 * regular
 * attributes, transient attributes, formatters, calculators, and security
 * rules.
 * Uses cached indexes when available for performance, falls back to name-based
 * access otherwise.
 * Works for both Query and Select modules.
 * 
 * @param <T> The output type (Row, SelectItem, etc.)
 * @param <D> The definition type (QueryDefinition, SelectDefinition)
 * @param <C> The context type (QueryContext, SelectContext)
 */
public abstract class BaseRowMapper<T, D extends BaseDefinition, C extends BaseContext<D>> implements RowMapper<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseRowMapper.class);

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        // This method is required by RowMapper interface but we need context
        throw new UnsupportedOperationException("Use mapRow(ResultSet, int, Context) instead");
    }

    /**
     * Map a ResultSet row with full context support.
     */
    public T mapRow(ResultSet rs, int rowNum, C context) throws SQLException {
        D definition = context.getDefinition();
        MetadataCache cache = getCache(context);

        Map<String, Object> processedData = new HashMap<>();
        Map<String, Object> rawData = extractRawData(rs, cache);

        // Get all attributes to process
        Map<String, AttributeDef<?>> attributes = getAttributesToProcess(definition);

        // First pass: Map regular (non-transient) attributes from database
        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip transient attributes (they're calculated later)
            if (attr.isVirual()) {
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

                String columnName = columnNames[i].toLowerCase();
                rawData.put(columnName, value);

                if (columnLabels != null && i < columnLabels.length) {
                    String label = columnLabels[i].toLowerCase();
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
                String columnName = metaData.getColumnName(i).toLowerCase();
                String columnLabel = metaData.getColumnLabel(i).toLowerCase();
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
            String aliasName = attr.getAliasName().toLowerCase();
            if (rawData.containsKey(aliasName)) {
                return rawData.get(aliasName);
            }
            // Try uppercase variant
            aliasName = attr.getAliasName().toUpperCase();
            if (rawData.containsKey(aliasName.toLowerCase())) {
                return rawData.get(aliasName.toLowerCase());
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
     */
    protected Object extractValueByIndex(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        // Check for NULL first
        Object value = rs.getObject(columnIndex);
        if (rs.wasNull()) {
            return null;
        }

        // Handle types based on SQL type for best performance
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
                return rs.getString(columnIndex);

            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return rs.getInt(columnIndex);

            case Types.BIGINT:
                return rs.getLong(columnIndex);

            case Types.DECIMAL:
            case Types.NUMERIC:
                return rs.getBigDecimal(columnIndex);

            case Types.FLOAT:
            case Types.REAL:
                return rs.getFloat(columnIndex);

            case Types.DOUBLE:
                return rs.getDouble(columnIndex);

            case Types.BOOLEAN:
            case Types.BIT:
                return rs.getBoolean(columnIndex);

            case Types.DATE:
                Date date = rs.getDate(columnIndex);
                return date != null ? date.toLocalDate() : null;

            case Types.TIME:
                Time time = rs.getTime(columnIndex);
                return time != null ? time.toLocalTime() : null;

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                Timestamp timestamp = rs.getTimestamp(columnIndex);
                return timestamp != null ? timestamp.toLocalDateTime() : null;

            case Types.BLOB:
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

            case Types.CLOB:
            case Types.NCLOB:
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

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return rs.getBytes(columnIndex);

            case Types.ARRAY:
                return rs.getArray(columnIndex);

            default:
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
    protected abstract Map<String, AttributeDef<?>> getAttributesToProcess(D definition);

    /**
     * Create intermediate output object for use in calculators.
     * This may be the same as the final output for some implementations.
     */
    protected abstract T createIntermediateOutput(Map<String, Object> processedData,
            Map<String, Object> rawData, C context);

    /**
     * Create the final output object.
     */
    protected abstract T createFinalOutput(Map<String, Object> processedData,
            Map<String, Object> rawData, C context);

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
    protected boolean hasSecurityContext(C context) {
        return false;
    }

    /**
     * Get security context from context.
     * Override in subclasses that support security.
     */
    protected Object getSecurityContext(C context) {
        return null;
    }

    /**
     * Calculate attribute value using the calculator.
     * Override in subclasses to handle specific calculator types.
     */
    protected abstract Object calculateAttribute(AttributeDef<?> attr, T intermediateResult, C context);

    /**
     * Get MetadataCache from context if available.
     * Subclasses can override to provide cache from different sources.
     */
    protected MetadataCache getCache(BaseContext<?> context) {
        // Default implementation returns null
        // Subclasses should override this to provide cache if available
        return null;
    }

    /**
     * Check if dynamic attributes should be included.
     * Subclasses can override to provide custom logic.
     */
    protected boolean shouldIncludeDynamicAttributes(D definition) {
        // Default implementation returns false
        // Subclasses should override this based on their definition type
        return false;
    }

    /**
     * Add dynamic attributes (columns not defined in AttributeDef) to the processed data.
     * These are columns returned by the SQL query but not explicitly defined.
     */
    protected void addDynamicAttributes(Map<String, Object> processedData, 
                                       Map<String, Object> rawData,
                                       Map<String, AttributeDef<?>> definedAttributes,
                                       D definition) {
        // Default implementation does nothing
        // Subclasses should override this to add dynamic attributes
    }
}