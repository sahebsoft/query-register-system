package com.balsam.oasis.common.registry.engine.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.processor.AttributeFormatter;
import com.balsam.oasis.common.registry.engine.sql.MetadataCache;
import com.balsam.oasis.common.registry.engine.sql.MetadataOperations;
import com.balsam.oasis.common.registry.engine.sql.util.JavaTypeConverter;

/**
 * Base row mapper that provides comprehensive attribute processing including
 * regular attributes, transient attributes, formatters, calculators, and
 * security
 * rules. Uses cached indexes when available for performance, falls back to
 * name-based
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
        Map<String, Object> rawData = MetadataOperations.extractRawData(rs, cache);

        // Get all attributes to process
        Map<String, AttributeDef<?>> attributes = definition.getAttributes();

        // Process all attributes (regular + virtual)
        processAttributes(rs, attributes, processedData, rawData, cache, context);

        // Create intermediate object for calculator context
        T intermediateResult = createIntermediateOutput(processedData, rawData, context);

        // Apply formatters to all attributes that have them
        applyFormatters(attributes, intermediateResult, processedData, context);

        // Add dynamic attributes if enabled
        if (shouldIncludeDynamicAttributes(definition)) {
            addDynamicAttributes(processedData, rawData, attributes, definition);
        }

        // Create final output
        return createFinalOutput(processedData, rawData, context);
    }

    /**
     * Process all attributes in a single pass (regular and virtual).
     */
    private void processAttributes(ResultSet rs, Map<String, AttributeDef<?>> attributes,
            Map<String, Object> processedData, Map<String, Object> rawData,
            MetadataCache cache, QueryContext context) throws SQLException {

        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip if attribute not selected
            if (!isAttributeIncluded(attr, attrName, context)) {
                if (attr.virtual()) {
                    log.debug("Skipping unselected field: {}", attrName);
                }
                continue;
            }

            // Check security
            if (attr.isSecured() && hasSecurityContext(context)) {
                Object securityContext = getSecurityContext(context);
                Boolean allowed = attr.securityRule().apply(securityContext);
                if (!Boolean.TRUE.equals(allowed)) {
                    processedData.put(attrName, null);
                    continue;
                }
            }

            if (attr.virtual()) {
                // Will be calculated after intermediate object creation
                continue;
            }

            // Get value from ResultSet
            Object rawValue = MetadataOperations.extractAttributeValue(rs, attr, cache, rawData);

            // Apply automatic type conversion
            Object convertedValue = rawValue;
            if (rawValue != null && attr.type() != null) {
                convertedValue = convertToType(rawValue, attr.type());
            }

            processedData.put(attrName, convertedValue);
        }
    }

    /**
     * Apply formatters to all attributes that have them.
     */
    private void applyFormatters(Map<String, AttributeDef<?>> attributes,
            T intermediateResult, Map<String, Object> processedData,
            QueryContext context) {

        // Calculate virtual attributes first
        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            if (!attr.virtual() || !isAttributeIncluded(attr, attrName, context)) {
                continue;
            }

            if (attr.hasCalculator()) {
                try {
                    Object calculatedValue = calculateAttribute(attr, intermediateResult, context);

                    // Ensure type safety
                    if (calculatedValue != null && attr.type() != null) {
                        calculatedValue = convertToType(calculatedValue, attr.type());
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

        // Apply formatters to all attributes
        for (Map.Entry<String, AttributeDef<?>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            if (!attr.hasFormatter() || !isAttributeIncluded(attr, attrName, context)) {
                continue;
            }

            try {
                Object currentValue = getValueFromOutput(intermediateResult, attrName);
                if (currentValue != null) {
                    AttributeFormatter formatter = attr.formatter();
                    String formattedValue = formatter.format(currentValue);
                    setValueInOutput(intermediateResult, attrName, formattedValue);
                    processedData.put(attrName, formattedValue);
                }
            } catch (Exception e) {
                log.warn("Failed to format attribute {}: {}", attrName, e.getMessage());
            }
        }
    }

    /**
     * Check if an attribute should be included in the result based on:
     * 1. The attribute's selected field (if false, exclude unless explicitly
     * requested)
     * 2. The context's field selection (if specified, only include selected fields)
     */
    protected boolean isAttributeIncluded(AttributeDef<?> attr, String attrName, QueryContext context) {
        // If attribute has selected=false, only include if explicitly requested
        if (!attr.selected()) {
            return context.getSelectedFields() != null &&
                    context.getSelectedFields().contains(attrName);
        }

        // Otherwise, include based on field selection
        return context.isFieldSelected(attrName);
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
            return JavaTypeConverter.convert(value, targetType);
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
     * Check if dynamic attributes should be included.
     * Subclasses can override to provide custom logic.
     */
    protected boolean shouldIncludeDynamicAttributes(QueryDefinition definition) {
        return definition.isIncludeDynamicAttributes();
    }

    /**
     * Add dynamic attributes (columns not defined in AttributeDef) to the processed
     * data. These are columns returned by the SQL query but not explicitly defined.
     */
    protected void addDynamicAttributes(Map<String, Object> processedData,
            Map<String, Object> rawData,
            Map<String, AttributeDef<?>> definedAttributes,
            QueryDefinition definition) {
        // Default implementation does nothing
        // Subclasses should override this to add dynamic attributes
    }
}