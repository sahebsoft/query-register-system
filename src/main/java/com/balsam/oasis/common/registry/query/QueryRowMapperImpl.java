package com.balsam.oasis.common.registry.query;

import java.util.Map;

import com.balsam.oasis.common.registry.base.BaseContext;
import com.balsam.oasis.common.registry.base.BaseRowMapper;
import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.execution.MetadataCache;
import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.core.result.RowImpl;

/**
 * Query-specific implementation of BaseRowMapper that produces Row objects.
 */
public class QueryRowMapperImpl extends BaseRowMapper<Row, QueryDefinition, QueryContext> {
    
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
    protected boolean hasSecurityContext(QueryContext context) {
        return context.getSecurityContext() != null;
    }
    
    @Override
    protected Object getSecurityContext(QueryContext context) {
        return context.getSecurityContext();
    }
    
    @Override
    protected Object calculateAttribute(AttributeDef<?> attr, Row intermediateResult, QueryContext context) {
        // Calculator now expects Row and BaseContext<?> - pass QueryContext as BaseContext
        return attr.getCalculator().calculate(intermediateResult, (BaseContext<?>) context);
    }
    
    @Override
    protected MetadataCache getCache(BaseContext<?> context) {
        // QueryDefinition has MetadataCache
        if (context != null && context.getDefinition() != null && context.getDefinition() instanceof QueryDefinition) {
            return ((QueryDefinition) context.getDefinition()).getMetadataCache();
        }
        return null;
    }
}