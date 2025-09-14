package com.balsam.oasis.common.registry.domain.execution;

import java.util.HashMap;
import java.util.Map;

import com.balsam.oasis.common.registry.builder.PlsqlDefinitionBuilder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlsqlContext {
    private PlsqlDefinitionBuilder definition;
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
    @Builder.Default
    private boolean includeMetadata = true;

    public void addParam(String name, Object value) {
        params.put(name, value);
    }

    public Object getParam(String name) {
        return params.get(name);
    }

    public boolean hasParam(String name) {
        return params.containsKey(name) && params.get(name) != null;
    }

}