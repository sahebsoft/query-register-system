package com.balsam.oasis.common.registry.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.balsam.oasis.common.registry.engine.query.QueryExecutorImpl;
import com.balsam.oasis.common.registry.engine.query.QueryRegistryImpl;
import com.balsam.oasis.common.registry.engine.query.QuerySqlBuilder;
import com.balsam.oasis.common.registry.web.builder.QueryResponseBuilder;
import com.balsam.oasis.common.registry.service.QueryService;
import com.balsam.oasis.common.registry.web.controller.QueryController;
import com.balsam.oasis.common.registry.web.controller.SelectController;
import com.balsam.oasis.common.registry.web.parser.QueryRequestParser;

/**
 * Configuration for Query Registration System
 */
@Configuration
@EnableConfigurationProperties(QueryProperties.class)
public class QueryConfiguration {

    @Bean
    QuerySqlBuilder sqlBuilder() {
        return new QuerySqlBuilder();
    }

    @Bean
    QueryRegistryImpl queryRegistry() {
        return new QueryRegistryImpl();
    }

    @Bean
    QueryExecutorImpl queryExecutor(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder,
            QueryRegistryImpl queryRegistry,
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
    QueryService queryService(QueryExecutorImpl queryExecutor, QueryRegistryImpl queryRegistry) {
        return new QueryService(queryExecutor, queryRegistry);
    }

    @Bean
    QueryController queryController(
            QueryService queryService,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        return new QueryController(queryService, requestParser, responseBuilder);
    }

    @Bean
    SelectController selectController(
            QueryService queryService,
            QueryResponseBuilder responseBuilder,
            QueryRequestParser requestParser) {
        return new SelectController(queryService, responseBuilder, requestParser);
    }
}