package com.balsam.oasis.common.registry.domain.metadata;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaginationMetadata {
    int start;
    int end;
    int total;
    Boolean hasNext;
    Boolean hasPrevious;
    int pageSize;
    int pageCount;
    int currentPage;
}