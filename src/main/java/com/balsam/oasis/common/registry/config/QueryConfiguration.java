package com.balsam.oasis.common.registry.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.balsam.oasis.common.registry.domain.api.QueryExecutor;
import com.balsam.oasis.common.registry.domain.api.QueryRegistry;
import com.balsam.oasis.common.registry.engine.query.QueryExecutorImpl;
import com.balsam.oasis.common.registry.engine.query.QueryRegistryImpl;
import com.balsam.oasis.common.registry.engine.query.QuerySqlBuilder;
import com.balsam.oasis.common.registry.engine.sql.MetadataCacheBuilder;
import com.balsam.oasis.common.registry.web.builder.QueryResponseBuilder;
import com.balsam.oasis.common.registry.web.controller.QueryController;
import com.balsam.oasis.common.registry.web.parser.QueryRequestParser;

/**
 * Configuration for Query Registration System
 */
@Configuration
@EnableConfigurationProperties(QueryProperties.class)
public class QueryConfiguration {

    @Bean
    QuerySqlBuilder sqlBuilder(QueryProperties properties) {
        return new QuerySqlBuilder(properties.getDatabaseDialect());
    }

    @Bean
    QueryRegistry queryRegistry() {
        return new QueryRegistryImpl();
    }

    @Bean
    QueryExecutor queryExecutor(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder,
            QueryRegistry queryRegistry,
            QueryProperties properties) {
        return new QueryExecutorImpl(jdbcTemplate, sqlBuilder, queryRegistry);
    }

    @Bean
    QueryRequestParser queryRequestParser() {
        return new QueryRequestParser();
    }

    @Bean
    QueryResponseBuilder queryResponseBuilder() {
        return new QueryResponseBuilder();
    }

    @Bean
    MetadataCacheBuilder metadataCacheBuilder(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder) {
        return new MetadataCacheBuilder(jdbcTemplate, sqlBuilder);
    }

    @Bean
    QueryController queryController(
            QueryExecutor queryExecutor,
            QueryRegistry queryRegistry,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        return new QueryController(queryExecutor, queryRegistry, requestParser, responseBuilder);
    }
}