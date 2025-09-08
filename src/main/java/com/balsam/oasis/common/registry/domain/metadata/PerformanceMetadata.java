package com.balsam.oasis.common.registry.domain.metadata;

import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerformanceMetadata {
    long executionTimeMs;
    int rowsFetched;
    int totalRowsScanned;
    boolean cacheHit;
    String queryPlan;
    Map<String, Object> additionalMetrics;
}