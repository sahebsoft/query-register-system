package com.balsam.oasis.common.registry.domain.metadata;

import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.common.AppliedCriteria;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QueryMetadata {
    PaginationMetadata pagination;
    List<AttributeMetadata> attributes;
    List<AppliedCriteria> appliedCriteria;
    Map<String, FilterMetadata> appliedFilters;
    List<SortMetadata> appliedSort;
    Map<String, ParameterMetadata> parameters;
    PerformanceMetadata performance;
}