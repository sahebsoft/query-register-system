package com.balsam.oasis.common.registry.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.query.QueryContext;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.balsam.oasis.common.registry.query.QueryResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test coverage for PreProcessor and PostProcessor interfaces.
 * Tests context modification, result processing, parameter validation, 
 * metadata enhancement, and error handling scenarios.
 */
class QueryProcessorsTest {

    // ========== PRE-PROCESSOR TESTS ==========

    @Test
    void testPreProcessor_BasicContextModification() {
        PreProcessor contextModifier = context -> {
            context.setParam("processedBy", "PreProcessor");
            context.setParam("timestamp", System.currentTimeMillis());
            context.addMetadata("preprocessed", true);
        };
        
        QueryContext context = mockContext();
        
        contextModifier.process(context);
        
        verify(context).setParam("processedBy", "PreProcessor");
        verify(context).setParam(eq("timestamp"), any(Long.class));
        verify(context).addMetadata("preprocessed", true);
    }

    @Test
    void testPreProcessor_ParameterValidation() {
        PreProcessor parameterValidator = context -> {
            // Validate required parameters
            if (!context.hasParam("userId")) {
                throw new IllegalArgumentException("userId parameter is required");
            }
            
            Integer maxResults = (Integer) context.getParam("maxResults");
            if (maxResults != null && maxResults > 1000) {
                context.setParam("maxResults", 1000);
                context.addMetadata("maxResultsCapped", true);
            }
            
            // Set default values
            if (!context.hasParam("includeInactive")) {
                context.setParam("includeInactive", false);
            }
        };
        
        QueryContext context = mockContextWithParams(Map.of(
            "userId", 123,
            "maxResults", 5000
        ));
        
        parameterValidator.process(context);
        
        verify(context).setParam("maxResults", 1000);
        verify(context).addMetadata("maxResultsCapped", true);
        verify(context).setParam("includeInactive", false);
    }

    @Test
    void testPreProcessor_SecurityEnforcement() {
        PreProcessor securityProcessor = context -> {
            Integer currentUserId = (Integer) context.getParam("currentUserId");
            String department = (String) context.getParam("department");
            
            // Apply row-level security constraints
            if (currentUserId != null) {
                context.addMetadata("securityUserId", currentUserId);
                
                // Add department filter if not admin
                if (!"ADMIN".equals(department)) {
                    context.setParam("departmentFilter", department);
                    context.addMetadata("departmentFiltered", true);
                }
            }
            
            // Add audit information
            context.setParam("auditUserId", currentUserId);
            context.setParam("auditTimestamp", LocalDateTime.now());
        };
        
        QueryContext context = mockContextWithParams(Map.of(
            "currentUserId", 456,
            "department", "SALES"
        ));
        
        securityProcessor.process(context);
        
        verify(context).addMetadata("securityUserId", 456);
        verify(context).setParam("departmentFilter", "SALES");
        verify(context).addMetadata("departmentFiltered", true);
        verify(context).setParam("auditUserId", 456);
        verify(context).setParam(eq("auditTimestamp"), any(LocalDateTime.class));
    }

    @Test
    void testPreProcessor_CacheConfiguration() {
        PreProcessor cacheConfigProcessor = context -> {
            String cacheKey = generateCacheKey(context);
            context.setCacheKey(cacheKey);
            
            // Configure cache based on query parameters
            Boolean forceFresh = (Boolean) context.getParam("forceFresh");
            if (Boolean.TRUE.equals(forceFresh)) {
                context.setCacheEnabled(false);
                context.addMetadata("cacheDisabled", "Force refresh requested");
            } else {
                context.setCacheEnabled(true);
                
                // Set TTL based on data sensitivity
                String dataSensitivity = (String) context.getParam("dataSensitivity");
                if ("HIGH".equals(dataSensitivity)) {
                    context.addMetadata("cacheTTL", 60); // 1 minute
                } else {
                    context.addMetadata("cacheTTL", 300); // 5 minutes
                }
            }
        };
        
        QueryContext context = mockContextWithParams(Map.of(
            "userId", 123,
            "department", "FINANCE",
            "dataSensitivity", "HIGH"
        ));
        
        cacheConfigProcessor.process(context);
        
        verify(context).setCacheKey(any(String.class));
        verify(context).setCacheEnabled(true);
        verify(context).addMetadata("cacheTTL", 60);
    }

    @Test
    void testPreProcessor_QueryOptimization() {
        PreProcessor optimizationProcessor = context -> {
            List<String> selectedFields = (List<String>) context.getParam("fields");
            Integer limit = (Integer) context.getParam("limit");
            
            // Optimize field selection
            if (selectedFields != null && selectedFields.size() < 5) {
                context.addMetadata("optimizedQuery", true);
                context.addMetadata("selectedFieldsCount", selectedFields.size());
            }
            
            // Optimize pagination
            if (limit != null && limit > 100) {
                context.setParam("limit", 100);
                context.addMetadata("limitCapped", true);
            }
            
            // Add query hints
            context.addMetadata("useIndex", true);
            context.addMetadata("fetchSize", 50);
        };
        
        QueryContext context = mockContextWithParams(Map.of(
            "fields", List.of("id", "name", "email"),
            "limit", 500
        ));
        
        optimizationProcessor.process(context);
        
        verify(context).addMetadata("optimizedQuery", true);
        verify(context).addMetadata("selectedFieldsCount", 3);
        verify(context).setParam("limit", 100);
        verify(context).addMetadata("limitCapped", true);
        verify(context).addMetadata("useIndex", true);
        verify(context).addMetadata("fetchSize", 50);
    }

    @Test
    void testPreProcessor_NullContext() {
        PreProcessor nullSafeProcessor = context -> {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
        };
        
        assertThatThrownBy(() -> nullSafeProcessor.process(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Context cannot be null");
    }

    @Test
    void testPreProcessor_Chaining() {
        AtomicInteger executionOrder = new AtomicInteger(0);
        
        PreProcessor firstProcessor = context -> {
            int order = executionOrder.incrementAndGet();
            context.setParam("firstProcessor", order);
        };
        
        PreProcessor secondProcessor = context -> {
            int order = executionOrder.incrementAndGet();
            context.setParam("secondProcessor", order);
        };
        
        PreProcessor chainedProcessor = context -> {
            firstProcessor.process(context);
            secondProcessor.process(context);
        };
        
        QueryContext context = mockContext();
        
        chainedProcessor.process(context);
        
        verify(context).setParam("firstProcessor", 1);
        verify(context).setParam("secondProcessor", 2);
    }

    // ========== POST-PROCESSOR TESTS ==========

    @Test
    void testPostProcessor_BasicResultModification() {
        PostProcessor resultModifier = (result, context) -> {
            return result.toBuilder()
                .executionTimeMs(System.currentTimeMillis() - context.getStartTime())
                .count(result.getRows().size())
                .summary(Map.of("processed", true, "modifier", "PostProcessor"))
                .build();
        };
        
        QueryResult originalResult = mockQueryResult(
            List.of(mockRow(Map.of("id", 1, "name", "John")))
        );
        QueryContext context = mockContext();
        when(context.getStartTime()).thenReturn(System.currentTimeMillis() - 100);
        
        QueryResult processedResult = resultModifier.process(originalResult, context);
        
        assertThat(processedResult).isNotNull();
        assertThat(processedResult.getCount()).isEqualTo(1);
        assertThat(processedResult.getSummary()).containsKeys("processed", "modifier");
        assertThat(processedResult.getExecutionTimeMs()).isNotNull();
    }

    @Test
    void testPostProcessor_DataTransformation() {
        PostProcessor dataTransformer = (result, context) -> {
            List<Row> transformedRows = new ArrayList<>();
            
            for (Row row : result.getRows()) {
                Row transformedRow = mockRow(Map.of(
                    "id", row.get("id"),
                    "name", row.getString("name"),
                    "upperName", row.getString("name") != null ? 
                        row.getString("name").toUpperCase() : null,
                    "processed", true
                ));
                transformedRows.add(transformedRow);
            }
            
            return result.toBuilder()
                .rows(transformedRows)
                .summary(Map.of("transformed", transformedRows.size()))
                .build();
        };
        
        QueryResult originalResult = mockQueryResult(List.of(
            mockRow(Map.of("id", 1, "name", "john")),
            mockRow(Map.of("id", 2, "name", "jane"))
        ));
        QueryContext context = mockContext();
        
        QueryResult processedResult = dataTransformer.process(originalResult, context);
        
        assertThat(processedResult.getRows()).hasSize(2);
        assertThat(processedResult.getSummary()).containsEntry("transformed", 2);
    }

    @Test
    void testPostProcessor_MetadataEnhancement() {
        PostProcessor metadataEnhancer = (result, context) -> {
            QueryResult.QueryMetadata.QueryMetadataBuilder metadataBuilder = 
                QueryResult.QueryMetadata.builder();
            
            // Add performance metadata
            metadataBuilder.performance(
                QueryResult.QueryMetadata.PerformanceMetadata.builder()
                    .executionTimeMs(context.getExecutionTime())
                    .rowsFetched(result.getRows().size())
                    .cacheHit(context.isCacheEnabled() && context.getCacheKey() != null)
                    .additionalMetrics(Map.of("optimized", true))
                    .build()
            );
            
            // Add pagination metadata if applicable
            if (context.hasPagination()) {
                metadataBuilder.pagination(
                    QueryResult.QueryMetadata.PaginationMetadata.builder()
                        .start(context.getPagination().getStart())
                        .end(context.getPagination().getEnd())
                        .total(context.getTotalCount() != null ? context.getTotalCount() : result.getRows().size())
                        .hasNext(context.getPagination().isHasNext())
                        .hasPrevious(context.getPagination().isHasPrevious())
                        .build()
                );
            }
            
            return result.toBuilder()
                .metadata(metadataBuilder.build())
                .build();
        };
        
        QueryResult originalResult = mockQueryResult(
            List.of(mockRow(Map.of("id", 1, "name", "John")))
        );
        QueryContext context = mockContextWithPagination();
        
        QueryResult processedResult = metadataEnhancer.process(originalResult, context);
        
        assertThat(processedResult.hasMetadata()).isTrue();
        assertThat(processedResult.getMetadata().getPerformance()).isNotNull();
        assertThat(processedResult.getMetadata().getPagination()).isNotNull();
    }

    @Test
    void testPostProcessor_Filtering() {
        PostProcessor filteringProcessor = (result, context) -> {
            String filterLevel = (String) context.getParam("filterLevel");
            
            List<Row> filteredRows = result.getRows().stream()
                .filter(row -> {
                    if ("HIGH".equals(filterLevel)) {
                        Integer score = row.getInteger("score");
                        return score != null && score >= 80;
                    } else if ("MEDIUM".equals(filterLevel)) {
                        Integer score = row.getInteger("score");
                        return score != null && score >= 60;
                    }
                    return true; // No filtering for LOW or null
                })
                .toList();
            
            return result.toBuilder()
                .rows(filteredRows)
                .count(filteredRows.size())
                .summary(Map.of(
                    "originalCount", result.getRows().size(),
                    "filteredCount", filteredRows.size(),
                    "filterLevel", filterLevel != null ? filterLevel : "NONE"
                ))
                .build();
        };
        
        QueryResult originalResult = mockQueryResult(List.of(
            mockRow(Map.of("id", 1, "score", 95)),
            mockRow(Map.of("id", 2, "score", 75)),
            mockRow(Map.of("id", 3, "score", 45))
        ));
        QueryContext context = mockContextWithParams(Map.of("filterLevel", "HIGH"));
        
        QueryResult processedResult = filteringProcessor.process(originalResult, context);
        
        assertThat(processedResult.getRows()).hasSize(1);
        assertThat(processedResult.getSummary()).containsEntry("originalCount", 3);
        assertThat(processedResult.getSummary()).containsEntry("filteredCount", 1);
    }

    @Test
    void testPostProcessor_Aggregation() {
        PostProcessor aggregationProcessor = (result, context) -> {
            // Calculate aggregations
            int totalRecords = result.getRows().size();
            BigDecimal totalValue = result.getRows().stream()
                .map(row -> row.getBigDecimal("value"))
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal averageValue = totalRecords > 0 ? 
                totalValue.divide(BigDecimal.valueOf(totalRecords)) : 
                BigDecimal.ZERO;
            
            Map<String, Object> aggregations = Map.of(
                "totalRecords", totalRecords,
                "totalValue", totalValue,
                "averageValue", averageValue,
                "maxValue", result.getRows().stream()
                    .map(row -> row.getBigDecimal("value"))
                    .filter(value -> value != null)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO)
            );
            
            return result.toBuilder()
                .summary(aggregations)
                .build();
        };
        
        QueryResult originalResult = mockQueryResult(List.of(
            mockRow(Map.of("id", 1, "value", new BigDecimal("100"))),
            mockRow(Map.of("id", 2, "value", new BigDecimal("200"))),
            mockRow(Map.of("id", 3, "value", new BigDecimal("300")))
        ));
        QueryContext context = mockContext();
        
        QueryResult processedResult = aggregationProcessor.process(originalResult, context);
        
        assertThat(processedResult.getSummary()).containsEntry("totalRecords", 3);
        assertThat(processedResult.getSummary()).containsEntry("totalValue", new BigDecimal("600"));
        assertThat(processedResult.getSummary()).containsKey("averageValue");
        assertThat(processedResult.getSummary()).containsEntry("maxValue", new BigDecimal("300"));
    }

    @Test
    void testPostProcessor_ErrorHandling() {
        PostProcessor errorHandlingProcessor = (result, context) -> {
            try {
                // Simulate processing that might fail
                String failureMode = (String) context.getParam("failureMode");
                if ("SIMULATE_ERROR".equals(failureMode)) {
                    throw new RuntimeException("Simulated processing error");
                }
                
                return result.toBuilder()
                    .summary(Map.of("processed", true))
                    .build();
                    
            } catch (Exception e) {
                return result.toBuilder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .summary(Map.of("processed", false, "error", e.getClass().getSimpleName()))
                    .build();
            }
        };
        
        QueryResult originalResult = mockQueryResult(
            List.of(mockRow(Map.of("id", 1, "name", "John")))
        );
        QueryContext context = mockContextWithParams(Map.of("failureMode", "SIMULATE_ERROR"));
        
        QueryResult processedResult = errorHandlingProcessor.process(originalResult, context);
        
        assertThat(processedResult.hasErrors()).isTrue();
        assertThat(processedResult.getErrorMessage()).isEqualTo("Simulated processing error");
        assertThat(processedResult.getSummary()).containsEntry("processed", false);
    }

    @Test
    void testPostProcessor_NullResult() {
        PostProcessor nullSafeProcessor = (result, context) -> {
            if (result == null) {
                return QueryResult.builder()
                    .success(false)
                    .errorMessage("Null result received")
                    .rows(List.of())
                    .build();
            }
            return result;
        };
        
        QueryContext context = mockContext();
        
        QueryResult processedResult = nullSafeProcessor.process(null, context);
        
        assertThat(processedResult).isNotNull();
        assertThat(processedResult.hasErrors()).isTrue();
        assertThat(processedResult.getErrorMessage()).isEqualTo("Null result received");
    }

    @Test
    void testPostProcessor_Chaining() {
        PostProcessor firstProcessor = (result, context) -> result.toBuilder()
            .summary(Map.of("step", "first"))
            .build();
        
        PostProcessor secondProcessor = (result, context) -> {
            Map<String, Object> summary = new HashMap<>(result.getSummary());
            summary.put("step", "second");
            summary.put("chained", true);
            return result.toBuilder()
                .summary(summary)
                .build();
        };
        
        PostProcessor chainedProcessor = (result, context) -> {
            QueryResult intermediate = firstProcessor.process(result, context);
            return secondProcessor.process(intermediate, context);
        };
        
        QueryResult originalResult = mockQueryResult(
            List.of(mockRow(Map.of("id", 1, "name", "John")))
        );
        QueryContext context = mockContext();
        
        QueryResult processedResult = chainedProcessor.process(originalResult, context);
        
        assertThat(processedResult.getSummary()).containsEntry("step", "second");
        assertThat(processedResult.getSummary()).containsEntry("chained", true);
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testPreAndPostProcessor_Integration() {
        AtomicBoolean preProcessorExecuted = new AtomicBoolean(false);
        AtomicBoolean postProcessorExecuted = new AtomicBoolean(false);
        
        PreProcessor preProcessor = context -> {
            preProcessorExecuted.set(true);
            context.setParam("preprocessed", true);
            context.addMetadata("preProcessorTimestamp", System.currentTimeMillis());
        };
        
        PostProcessor postProcessor = (result, context) -> {
            postProcessorExecuted.set(true);
            Boolean preprocessed = (Boolean) context.getParam("preprocessed");
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("preprocessed", preprocessed);
            summary.put("postprocessed", true);
            
            return result.toBuilder()
                .summary(summary)
                .build();
        };
        
        // Simulate pre-processing
        QueryContext context = mockContext();
        preProcessor.process(context);
        
        // Simulate query execution would happen here
        
        // Simulate post-processing
        QueryResult result = mockQueryResult(
            List.of(mockRow(Map.of("id", 1, "name", "John")))
        );
        QueryResult finalResult = postProcessor.process(result, context);
        
        assertThat(preProcessorExecuted.get()).isTrue();
        assertThat(postProcessorExecuted.get()).isTrue();
        assertThat(finalResult.getSummary()).containsEntry("preprocessed", true);
        assertThat(finalResult.getSummary()).containsEntry("postprocessed", true);
    }

    // ========== PARAMETERIZED TESTS ==========

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "MANAGER", "USER", "GUEST"})
    void testPreProcessor_RoleBasedProcessing(String role) {
        PreProcessor roleProcessor = context -> {
            context.setParam("userRole", role);
            
            switch (role) {
                case "ADMIN" -> {
                    context.setParam("fullAccess", true);
                    context.setParam("maxResults", Integer.MAX_VALUE);
                }
                case "MANAGER" -> {
                    context.setParam("fullAccess", false);
                    context.setParam("maxResults", 1000);
                    context.setParam("departmentOnly", true);
                }
                case "USER" -> {
                    context.setParam("fullAccess", false);
                    context.setParam("maxResults", 100);
                    context.setParam("ownDataOnly", true);
                }
                case "GUEST" -> {
                    context.setParam("fullAccess", false);
                    context.setParam("maxResults", 10);
                    context.setParam("publicDataOnly", true);
                }
            }
        };
        
        QueryContext context = mockContext();
        
        roleProcessor.process(context);
        
        verify(context).setParam("userRole", role);
        verify(context).setParam("fullAccess", "ADMIN".equals(role));
        
        if ("ADMIN".equals(role)) {
            verify(context).setParam("maxResults", Integer.MAX_VALUE);
        } else if ("MANAGER".equals(role)) {
            verify(context).setParam("maxResults", 1000);
            verify(context).setParam("departmentOnly", true);
        } else if ("USER".equals(role)) {
            verify(context).setParam("maxResults", 100);
            verify(context).setParam("ownDataOnly", true);
        } else if ("GUEST".equals(role)) {
            verify(context).setParam("maxResults", 10);
            verify(context).setParam("publicDataOnly", true);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, EMPTY",
        "1, 1, SINGLE",
        "50, 50, NORMAL",
        "100, 100, LARGE",
        "1000, 1000, VERY_LARGE"
    })
    void testPostProcessor_ResultSizeAnalysis(int inputSize, int expectedCount, String expectedCategory) {
        PostProcessor sizeAnalyzer = (result, context) -> {
            int resultSize = result.getRows().size();
            String category;
            
            if (resultSize == 0) {
                category = "EMPTY";
            } else if (resultSize == 1) {
                category = "SINGLE";
            } else if (resultSize <= 50) {
                category = "NORMAL";
            } else if (resultSize <= 100) {
                category = "LARGE";
            } else {
                category = "VERY_LARGE";
            }
            
            return result.toBuilder()
                .summary(Map.of(
                    "resultSize", resultSize,
                    "sizeCategory", category,
                    "analyzed", true
                ))
                .build();
        };
        
        // Create result with specified size
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < inputSize; i++) {
            rows.add(mockRow(Map.of("id", i, "name", "Item" + i)));
        }
        QueryResult originalResult = mockQueryResult(rows);
        QueryContext context = mockContext();
        
        QueryResult processedResult = sizeAnalyzer.process(originalResult, context);
        
        assertThat(processedResult.getSummary()).containsEntry("resultSize", expectedCount);
        assertThat(processedResult.getSummary()).containsEntry("sizeCategory", expectedCategory);
        assertThat(processedResult.getSummary()).containsEntry("analyzed", true);
    }

    // ========== HELPER METHODS ==========

    private QueryContext mockContext() {
        QueryContext context = mock(QueryContext.class);
        
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        
        when(context.getParam(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return params.get(key);
        });
        
        when(context.hasParam(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return params.containsKey(key) && params.get(key) != null;
        });
        
        when(context.getParams()).thenReturn(params);
        when(context.getMetadata()).thenReturn(metadata);
        
        when(context.getExecutionTime()).thenReturn(0L);
        when(context.getStartTime()).thenReturn(System.currentTimeMillis());
        
        // Mock parameter setting
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            params.put(key, value);
            return null;
        }).when(context).setParam(any(String.class), any());
        
        // Mock metadata addition
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            metadata.put(key, value);
            return null;
        }).when(context).addMetadata(any(String.class), any());
        
        return context;
    }
    
    private QueryContext mockContextWithParams(Map<String, Object> initialParams) {
        QueryContext context = mockContext();
        
        when(context.getParam(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return initialParams.get(key);
        });
        
        when(context.hasParam(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return initialParams.containsKey(key) && initialParams.get(key) != null;
        });
        
        when(context.getParams()).thenReturn(new HashMap<>(initialParams));
        
        return context;
    }
    
    private QueryContext mockContextWithPagination() {
        QueryContext context = mockContext();
        
        QueryContext.Pagination pagination = QueryContext.Pagination.builder()
            .start(0)
            .end(10)
            .total(100)
            .hasNext(true)
            .hasPrevious(false)
            .build();
        
        when(context.hasPagination()).thenReturn(true);
        when(context.getPagination()).thenReturn(pagination);
        when(context.getTotalCount()).thenReturn(100);
        when(context.isCacheEnabled()).thenReturn(true);
        when(context.getCacheKey()).thenReturn("test-cache-key");
        
        return context;
    }

    private Row mockRow(Map<String, Object> data) {
        Row row = mock(Row.class);
        
        when(row.get(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return data.get(key);
        });
        
        data.forEach((key, value) -> {
            if (value instanceof String) {
                when(row.getString(key)).thenReturn((String) value);
            } else if (value instanceof Integer) {
                when(row.getInteger(key)).thenReturn((Integer) value);
            } else if (value instanceof Long) {
                when(row.getLong(key)).thenReturn((Long) value);
            } else if (value instanceof Double) {
                when(row.getDouble(key)).thenReturn((Double) value);
            } else if (value instanceof BigDecimal) {
                when(row.getBigDecimal(key)).thenReturn((BigDecimal) value);
            } else if (value instanceof LocalDate) {
                when(row.getLocalDate(key)).thenReturn((LocalDate) value);
            } else if (value instanceof LocalDateTime) {
                when(row.getLocalDateTime(key)).thenReturn((LocalDateTime) value);
            } else if (value instanceof Boolean) {
                when(row.getBoolean(key)).thenReturn((Boolean) value);
            }
        });
        
        when(row.toMap()).thenReturn(new HashMap<>(data));
        
        return row;
    }

    private QueryResult mockQueryResult(List<Row> rows) {
        return QueryResult.builder()
            .rows(rows)
            .count(rows.size())
            .success(true)
            .summary(new HashMap<>())
            .build();
    }

    private String generateCacheKey(QueryContext context) {
        // Simple cache key generation for testing
        return "cache_" + context.hashCode();
    }
}