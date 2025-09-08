package com.balsam.oasis.common.registry.domain.metadata;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SortMetadata {
    String field;
    String direction;
    int priority;
}