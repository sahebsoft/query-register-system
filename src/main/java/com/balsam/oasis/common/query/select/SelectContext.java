package com.balsam.oasis.common.query.select;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple context for select execution.
 * Holds parameters and other execution context.
 */
public class SelectContext {
    
    private final Map<String, Object> params = new HashMap<>();
    
    public SelectContext() {
    }
    
    public void setParams(Map<String, Object> params) {
        this.params.clear();
        if (params != null) {
            this.params.putAll(params);
        }
    }
    
    public void setParam(String name, Object value) {
        this.params.put(name, value);
    }
    
    public Map<String, Object> getParams() {
        return new HashMap<>(params);
    }
    
    public Object getParam(String name) {
        return params.get(name);
    }
    
    public boolean hasParam(String name) {
        return params.containsKey(name) && params.get(name) != null;
    }
}