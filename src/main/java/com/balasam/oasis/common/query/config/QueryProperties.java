package com.balasam.oasis.common.query.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Query Registration System
 */
@Data
@ConfigurationProperties(prefix = "query.registration")
public class QueryProperties {
    
    /**
     * Enable Query Registration System
     */
    private boolean enabled = true;
    
    /**
     * REST API configuration
     */
    private RestProperties rest = new RestProperties();
    
    /**
     * Cache configuration
     */
    private CacheProperties cache = new CacheProperties();
    
    /**
     * Security configuration
     */
    private SecurityProperties security = new SecurityProperties();
    
    /**
     * Metadata configuration
     */
    private MetadataProperties metadata = new MetadataProperties();
    
    /**
     * Validation configuration
     */
    private ValidationProperties validation = new ValidationProperties();
    
    /**
     * JDBC configuration
     */
    private JdbcProperties jdbc = new JdbcProperties();
    
    @Data
    public static class RestProperties {
        private boolean enabled = true;
        private String prefix = "/api/query";
        private int defaultPageSize = 50;
        private int maxPageSize = 1000;
        private boolean enableCors = true;
        private boolean enableCompression = true;
        private String responseFormat = "json";
        private String parameterFormat = "standard";
    }
    
    @Data
    public static class CacheProperties {
        private boolean enabled = true;
        private String provider = "caffeine";
        private Duration defaultTtl = Duration.ofMinutes(5);
        private int maxEntries = 1000;
    }
    
    @Data
    public static class SecurityProperties {
        private boolean enabled = true;
        private boolean checkPermissions = true;
        private boolean maskSensitiveData = true;
        private boolean auditQueries = true;
    }
    
    @Data
    public static class MetadataProperties {
        private String includeByDefault = "full";
        private boolean includeQueryPlan = false;
        private boolean includeExecutionStats = true;
    }
    
    @Data
    public static class ValidationProperties {
        private boolean strictMode = true;
        private boolean validateParams = true;
        private boolean validateFilters = true;
    }
    
    @Data
    public static class JdbcProperties {
        private int fetchSize = 100;
        private Duration queryTimeout = Duration.ofSeconds(30);
        private boolean enableSqlLogging = true;
    }
}