package com.balasam.oasis.common.query.config;

import lombok.Data;

/**
 * Configurer for Query Registration System
 */
@Data
public class QueryRegistrationConfigurer {
    
    private final QueryProperties properties;
    private String scanPackage;
    private boolean enableRestApi = true;
    private String restApiPrefix = "/api/query";
    private boolean enableCache = true;
    private int defaultPageSize = 50;
    private int maxPageSize = 1000;
    private boolean enableMetrics = true;
    private boolean enableSecurity = true;
    private boolean enableSwaggerDocs = true;
    
    public QueryRegistrationConfigurer(QueryProperties properties) {
        this.properties = properties;
        // Initialize from properties
        this.enableRestApi = properties.getRest().isEnabled();
        this.restApiPrefix = properties.getRest().getPrefix();
        this.enableCache = properties.getCache().isEnabled();
        this.defaultPageSize = properties.getRest().getDefaultPageSize();
        this.maxPageSize = properties.getRest().getMaxPageSize();
        this.enableSecurity = properties.getSecurity().isEnabled();
    }
    
    public QueryRegistrationConfigurer scanPackage(String packageName) {
        this.scanPackage = packageName;
        return this;
    }
    
    public QueryRegistrationConfigurer enableRestApi(boolean enable) {
        this.enableRestApi = enable;
        return this;
    }
    
    public QueryRegistrationConfigurer restApiPrefix(String prefix) {
        this.restApiPrefix = prefix;
        return this;
    }
    
    public QueryRegistrationConfigurer enableCache(boolean enable) {
        this.enableCache = enable;
        return this;
    }
    
    public QueryRegistrationConfigurer defaultPageSize(int size) {
        this.defaultPageSize = size;
        return this;
    }
    
    public QueryRegistrationConfigurer maxPageSize(int size) {
        this.maxPageSize = size;
        return this;
    }
    
    public QueryRegistrationConfigurer enableMetrics(boolean enable) {
        this.enableMetrics = enable;
        return this;
    }
    
    public QueryRegistrationConfigurer enableSecurity(boolean enable) {
        this.enableSecurity = enable;
        return this;
    }
    
    public QueryRegistrationConfigurer enableSwaggerDocs(boolean enable) {
        this.enableSwaggerDocs = enable;
        return this;
    }
}