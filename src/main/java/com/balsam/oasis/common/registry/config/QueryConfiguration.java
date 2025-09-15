package com.balsam.oasis.common.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.balsam.oasis.common.registry.engine.query.QueryExecutorImpl;
import com.balsam.oasis.common.registry.engine.query.QueryRegistryImpl;
import com.balsam.oasis.common.registry.engine.query.QuerySqlBuilder;
import com.balsam.oasis.common.registry.service.QueryService;
import com.balsam.oasis.common.registry.web.controller.QueryController;
import com.balsam.oasis.common.registry.web.parser.QueryRequestParser;
import com.balsam.oasis.common.registry.engine.plsql.PlsqlExecutorImpl;
import com.balsam.oasis.common.registry.engine.plsql.PlsqlRegistryImpl;
import com.balsam.oasis.common.registry.service.PlsqlService;

/**
 * Configuration for Query Registration System
 */
@Configuration
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
            QueryRegistryImpl queryRegistry) {
        return new QueryExecutorImpl(jdbcTemplate, sqlBuilder, queryRegistry);
    }

    @Bean
    QueryRequestParser queryRequestParser() {
        return new QueryRequestParser();
    }

    @Bean
    QueryService queryService(QueryExecutorImpl queryExecutor, QueryRegistryImpl queryRegistry) {
        return new QueryService(queryExecutor, queryRegistry);
    }

    @Bean
    QueryController queryController(
            QueryService queryService,
            QueryRequestParser requestParser,
            PlsqlService plsqlService) {
        return new QueryController(queryService, requestParser, plsqlService);
    }

    // PL/SQL Configuration
    @Bean
    PlsqlRegistryImpl plsqlRegistry() {
        return new PlsqlRegistryImpl();
    }

    @Bean
    PlsqlExecutorImpl plsqlExecutor(JdbcTemplate jdbcTemplate, PlsqlRegistryImpl plsqlRegistry) {
        return new PlsqlExecutorImpl(jdbcTemplate, plsqlRegistry);
    }

    @Bean
    PlsqlService plsqlService(PlsqlExecutorImpl plsqlExecutor, PlsqlRegistryImpl plsqlRegistry) {
        return new PlsqlService(plsqlExecutor, plsqlRegistry);
    }

}