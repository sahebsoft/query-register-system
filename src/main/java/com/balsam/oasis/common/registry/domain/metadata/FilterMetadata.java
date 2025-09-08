package com.balsam.oasis.common.registry.domain.metadata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterMetadata {
    String attribute;
    String operator;
    Object value;
    Object value2;
    List<Object> values;
}