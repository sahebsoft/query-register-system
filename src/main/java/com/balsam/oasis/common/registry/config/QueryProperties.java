package com.balsam.oasis.common.registry.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Simplified configuration properties for Query Registration System
 * Only includes actually used properties
 */
@Data
@ConfigurationProperties(prefix = "query.registration")
public class QueryProperties {

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

    /**
     * Metadata cache configuration
     */
    private MetadataProperties metadata = new MetadataProperties();

    @Data
    public static class RestProperties {
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

    @Data
    public static class MetadataProperties {
        private CacheProperties cache = new CacheProperties();

        @Data
        public static class CacheProperties {
            private boolean prewarm = false;
            private boolean failOnError = true; // Stop application if pre-warming fails
        }
    }
}