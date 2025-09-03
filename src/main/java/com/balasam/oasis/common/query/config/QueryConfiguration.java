package com.balasam.oasis.common.query.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.balasam.oasis.common.query.core.execution.QueryExecutor;
import com.balasam.oasis.common.query.core.execution.QueryExecutorImpl;
import com.balasam.oasis.common.query.core.execution.SqlBuilder;
import com.balasam.oasis.common.query.rest.QueryController;
import com.balasam.oasis.common.query.rest.QueryRequestParser;
import com.balasam.oasis.common.query.rest.QueryResponseBuilder;
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
    public QueryExecutor queryExecutor(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder) {
        return new QueryExecutorImpl(jdbcTemplate, sqlBuilder);
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
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        return new QueryController(queryExecutor, requestParser, responseBuilder);
    }
}