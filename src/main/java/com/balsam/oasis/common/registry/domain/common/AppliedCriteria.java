package com.balsam.oasis.common.registry.domain.common;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppliedCriteria {
    private String name;
    private String sql;
    private Map<String, Object> params;
}