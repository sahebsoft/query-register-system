package com.balsam.oasis.common.registry.processor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.query.QueryContext;
import com.balsam.oasis.common.registry.query.QueryResult;

/**
 * Simple integration tests for processor components working together
 */
class ProcessorIntegrationSimpleTest {

    @Test
    void testPreProcessorAndRowProcessor() {
        AtomicInteger preProcessorExecuted = new AtomicInteger(0);
        AtomicInteger rowProcessorExecuted = new AtomicInteger(0);

        // Simple pre-processor that modifies context
        PreProcessor preProcessor = context -> {
            preProcessorExecuted.incrementAndGet();
            context.setParam("processedBy", "PreProcessor");
            context.addMetadata("preprocessed", true);
        };

        // Simple row processor that adds data to row
        RowProcessor rowProcessor = (row, context) -> {
            rowProcessorExecuted.incrementAndGet();
            String processedBy = (String) context.getParam("processedBy");
            row.set("contextData", processedBy);
            row.set("processed", true);
            return row;
        };

        // Create context and mock row
        QueryContext context = mockContext();
        Row row = mockRow(Map.of("name", "John", "value", new BigDecimal("100")));

        // Execute processors
        preProcessor.process(context);
        Row result = rowProcessor.process(row, context);

        // Verify execution
        assertThat(preProcessorExecuted.get()).isEqualTo(1);
        assertThat(rowProcessorExecuted.get()).isEqualTo(1);

        // Verify context was modified
        verify(context).setParam("processedBy", "PreProcessor");
        verify(context).addMetadata("preprocessed", true);

        // Verify row was processed
        verify(result).set("contextData", "PreProcessor");
        verify(result).set("processed", true);
    }

    @Test
    void testRowProcessorAndPostProcessor() {
        // Row processor that enriches data
        RowProcessor rowProcessor = (row, context) -> {
            String name = row.getString("name");
            if (name != null) {
                row.set("upperName", name.toUpperCase());
                row.set("nameLength", name.length());
            }
            return row;
        };

        // Post processor that aggregates results
        PostProcessor postProcessor = (result, context) -> {
            List<Row> rows = result.getRows();
            int totalRows = rows.size();

            Map<String, Object> summary = Map.of(
                    "totalRows", totalRows,
                    "processedBy", "PostProcessor");

            return result.toBuilder()
                    .summary(summary)
                    .build();
        };

        // Create test data
        Row row1 = mockRow(Map.of("name", "John"));
        Row row2 = mockRow(Map.of("name", "Jane"));
        QueryContext context = mockContext();

        // Process rows
        Row processedRow1 = rowProcessor.process(row1, context);
        Row processedRow2 = rowProcessor.process(row2, context);

        // Create result
        QueryResult originalResult = QueryResult.builder()
                .rows(List.of(processedRow1, processedRow2))
                .success(true)
                .build();

        // Process result
        QueryResult finalResult = postProcessor.process(originalResult, context);

        // Verify row processing
        verify(processedRow1).set("upperName", "JOHN");
        verify(processedRow1).set("nameLength", 4);
        verify(processedRow2).set("upperName", "JANE");
        verify(processedRow2).set("nameLength", 4);

        // Verify post processing
        assertThat(finalResult.getSummary()).containsEntry("totalRows", 2);
        assertThat(finalResult.getSummary()).containsEntry("processedBy", "PostProcessor");
    }

    @Test
    void testCompleteProcessingPipeline() {
        AtomicInteger executionOrder = new AtomicInteger(0);

        // Pre processor adds parameters
        PreProcessor preProcessor = context -> {
            context.setParam("executionOrder", executionOrder.incrementAndGet());
            context.setParam("multiplier", 2.0);
        };

        // Row processor uses parameters and modifies data
        RowProcessor rowProcessor = (row, context) -> {
            row.set("executionOrder", executionOrder.incrementAndGet());

            Double multiplier = (Double) context.getParam("multiplier");
            BigDecimal value = row.getBigDecimal("value");
            if (value != null && multiplier != null) {
                BigDecimal multipliedValue = value.multiply(BigDecimal.valueOf(multiplier)).setScale(0,
                        java.math.RoundingMode.HALF_UP);
                row.set("multipliedValue", multipliedValue);
            }
            return row;
        };

        // Post processor calculates aggregates
        PostProcessor postProcessor = (result, context) -> {
            Integer preOrder = (Integer) context.getParam("executionOrder");

            BigDecimal total = result.getRows().stream()
                    .map(row -> row.getBigDecimal("multipliedValue"))
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> summary = Map.of(
                    "preProcessorOrder", preOrder,
                    "postProcessorOrder", executionOrder.incrementAndGet(),
                    "totalValue", total);

            return result.toBuilder()
                    .summary(summary)
                    .executionTimeMs(100L)
                    .build();
        };

        // Execute complete pipeline
        QueryContext context = mockContext();

        // 1. Pre-processing
        preProcessor.process(context);

        // 2. Row processing
        Row row1 = mockRow(Map.of("value", new BigDecimal("50")));
        Row row2 = mockRow(Map.of("value", new BigDecimal("75")));

        Row processedRow1 = rowProcessor.process(row1, context);
        Row processedRow2 = rowProcessor.process(row2, context);

        // 3. Create result
        QueryResult originalResult = QueryResult.builder()
                .rows(List.of(processedRow1, processedRow2))
                .success(true)
                .build();

        // 4. Post-processing
        QueryResult finalResult = postProcessor.process(originalResult, context);

        // Verify complete pipeline
        verify(context).setParam("executionOrder", 1);
        verify(context).setParam("multiplier", 2.0);

        verify(processedRow1).set("executionOrder", 2);
        verify(processedRow1).set("multipliedValue", new BigDecimal("100"));
        verify(processedRow2).set("executionOrder", 3);
        verify(processedRow2).set("multipliedValue", new BigDecimal("150"));

        assertThat(finalResult.getSummary()).containsEntry("preProcessorOrder", 1);
        assertThat(finalResult.getSummary()).containsEntry("postProcessorOrder", 4);
        assertThat(finalResult.getSummary()).containsEntry("totalValue", new BigDecimal("250"));
        assertThat(finalResult.getExecutionTimeMs()).isEqualTo(100L);
    }

    @Test
    void testErrorHandlingInPipeline() {
        // Row processor that fails on certain conditions
        RowProcessor riskyProcessor = (row, context) -> {
            String name = row.getString("name");
            if ("FAIL".equals(name)) {
                throw new RuntimeException("Simulated failure");
            }
            row.set("processed", true);
            return row;
        };

        // Post processor with error recovery
        PostProcessor resilientPostProcessor = (result, context) -> {
            // Check if processing had errors (in real scenarios this might come from
            // context)
            boolean hasErrors = result.getRows().stream()
                    .anyMatch(row -> !Boolean.TRUE.equals(row.getBoolean("processed")));

            Map<String, Object> summary = Map.of(
                    "hasErrors", hasErrors,
                    "totalRows", result.getRows().size());

            return result.toBuilder()
                    .summary(summary)
                    .success(!hasErrors)
                    .build();
        };

        QueryContext context = mockContext();

        // Process successful row
        Row successRow = mockRow(Map.of("name", "SUCCESS"));
        Row processedSuccess = riskyProcessor.process(successRow, context);

        // Verify successful processing
        verify(processedSuccess).set("processed", true);

        // Process failing row (would throw exception in real scenario)
        Row failRow = mockRow(Map.of("name", "FAIL"));
        try {
            riskyProcessor.process(failRow, context);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Simulated failure");
        }
    }

    // Helper methods
    private QueryContext mockContext() {
        QueryContext context = mock(QueryContext.class);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();

        when(context.getParam(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return params.get(key);
        });

        when(context.getParams()).thenReturn(params);
        when(context.getMetadata()).thenReturn(metadata);

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            params.put(key, value);
            return null;
        }).when(context).setParam(any(String.class), any());

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            metadata.put(key, value);
            return null;
        }).when(context).addMetadata(any(String.class), any());

        return context;
    }

    private Row mockRow(Map<String, Object> data) {
        Row row = mock(Row.class);
        Map<String, Object> mutableData = new HashMap<>(data);

        when(row.get(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return mutableData.get(key);
        });

        mutableData.forEach((key, value) -> {
            if (value instanceof String) {
                when(row.getString(key)).thenReturn((String) value);
            } else if (value instanceof BigDecimal) {
                when(row.getBigDecimal(key)).thenReturn((BigDecimal) value);
            } else if (value instanceof Boolean) {
                when(row.getBoolean(key)).thenReturn((Boolean) value);
            }
        });

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            mutableData.put(key, value);

            // Update the mock to return the new value
            if (value instanceof String) {
                when(row.getString(key)).thenReturn((String) value);
            } else if (value instanceof BigDecimal) {
                when(row.getBigDecimal(key)).thenReturn((BigDecimal) value);
            } else if (value instanceof Boolean) {
                when(row.getBoolean(key)).thenReturn((Boolean) value);
            }
            when(row.get(key)).thenReturn(value);

            return null;
        }).when(row).set(any(String.class), any());

        return row;
    }
}