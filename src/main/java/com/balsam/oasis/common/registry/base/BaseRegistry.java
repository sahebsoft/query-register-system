package com.balsam.oasis.common.registry.base;

import java.util.Collection;
import java.util.Set;

public interface BaseRegistry<D extends BaseDefinition> {
    
    void register(D definition);
    
    void register(String name, D definition);
    
    D get(String name);
    
    boolean exists(String name);
    
    void remove(String name);
    
    void clear();
    
    Set<String> getNames();
    
    Collection<D> getAll();
    
    int size();
}