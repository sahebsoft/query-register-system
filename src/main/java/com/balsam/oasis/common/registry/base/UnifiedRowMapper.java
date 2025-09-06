package com.balsam.oasis.common.registry.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.execution.MetadataCache;
import com.balsam.oasis.common.registry.processor.AttributeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified row mapper that handles all attribute processing including regular
 * attributes,
 * transient attributes, formatters, calculators, and security rules.
 * Works for both Query and Select modules.
 * 
 * @param <T> The output type (Row, SelectItem, etc.)
 * @param <D> The definition type (QueryDefinition, SelectDefinition)
 * @param <C> The context type (QueryContext, SelectContext)
 */
public abstract class UnifiedRowMapper<T, D extends BaseDefinition, C extends BaseContext<D>> extends BaseRowMapper<T> {

    private static final Logger log = LoggerFactory.getLogger(UnifiedRowMapper.class);

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
            if (attr.isTransient()) {
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
            if (attr.isTransient()) {
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
            if (!attr.isTransient()) {
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
            if (!attr.isTransient() || !attr.hasFormatter()) {
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

        // Create final output
        return createFinalOutput(processedData, rawData, context);
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
}