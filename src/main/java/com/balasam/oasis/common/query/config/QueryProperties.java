package com.balasam.oasis.common.query.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Simplified configuration properties for Query Registration System
 * Only includes actually used properties
 */
@Data
@ConfigurationProperties(prefix = "query.registration")
public class QueryProperties {
    
    /**
     * Enable Query Registration System
     */
    private boolean enabled = true;
    
    /**
     * Database dialect (ORACLE_11G or ORACLE_12C)
     */
    private String databaseDialect = "ORACLE_11G";
    
    /**
     * REST API configuration
     */
    private RestProperties rest = new RestProperties();
    
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
    }
    
    @Data  
    public static class JdbcProperties {
        private int fetchSize = 100;
        private Duration queryTimeout = Duration.ofSeconds(30);
        private boolean enableSqlLogging = true;
    }
}