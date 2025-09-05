package com.balsam.oasis.common.query.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.balsam.oasis.common.query.core.execution.QueryExecutor;
import com.balsam.oasis.common.query.core.execution.QueryExecutorImpl;
import com.balsam.oasis.common.query.core.execution.SqlBuilder;
import com.balsam.oasis.common.query.registry.QueryRegistrar;
import com.balsam.oasis.common.query.registry.QueryRegistrarImpl;
import com.balsam.oasis.common.query.registry.QueryRegistry;
import com.balsam.oasis.common.query.rest.QueryController;
import com.balsam.oasis.common.query.rest.QueryRequestParser;
import com.balsam.oasis.common.query.rest.QueryResponseBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for Query Registration System
 */
@Configuration
@EnableConfigurationProperties(QueryProperties.class)
public class QueryConfiguration {

    @Bean
    public SqlBuilder sqlBuilder(QueryProperties properties) {
        return new SqlBuilder(properties.getDatabaseDialect());
    }

    @Bean
    public QueryRegistrar queryRegistrar() {
        return new QueryRegistrarImpl();
    }

    @Bean
    public QueryExecutor queryExecutor(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder, QueryRegistry queryRegistry, QueryProperties properties) {
        QueryExecutorImpl executor = new QueryExecutorImpl(jdbcTemplate, sqlBuilder, queryRegistry);

        // Configure metadata caching
        if (properties.getMetadata() != null && properties.getMetadata().getCache() != null) {
            QueryProperties.MetadataProperties.CacheProperties cacheProps = properties.getMetadata().getCache();
            executor.setPrewarmMetadataOnStartup(cacheProps.isPrewarm());
            executor.setUseOptimizedMapper(cacheProps.isUseOptimizedMapper());
        }

        return executor;
    }

    @Bean
    public QueryRequestParser queryRequestParser() {
        return new QueryRequestParser();
    }

    @Bean
    public QueryResponseBuilder queryResponseBuilder(ObjectMapper objectMapper) {
        return new QueryResponseBuilder(objectMapper);
    }

    @Bean
    public QueryController queryController(
            QueryExecutor queryExecutor,
            QueryRegistrar queryRegistrar,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        return new QueryController(queryExecutor, queryRegistrar, requestParser, responseBuilder);
    }
}