package com.balsam.oasis.common.registry.domain.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParameterMetadata {
    String name;
    Object value;
    String type;
    boolean required;
}