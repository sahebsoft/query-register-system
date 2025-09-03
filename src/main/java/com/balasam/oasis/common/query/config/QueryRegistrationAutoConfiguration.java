package com.balasam.oasis.common.query.config;

import com.balasam.oasis.common.query.core.execution.QueryExecutor;
import com.balasam.oasis.common.query.core.execution.QueryExecutorImpl;
import com.balasam.oasis.common.query.core.execution.SqlBuilder;
import com.balasam.oasis.common.query.rest.QueryController;
import com.balasam.oasis.common.query.rest.QueryRequestParser;
import com.balasam.oasis.common.query.rest.QueryResponseBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Auto-configuration for Query Registration System
 * Automatically enabled when JdbcTemplate is on classpath
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(prefix = "query.registration", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(QueryProperties.class)
public class QueryRegistrationAutoConfiguration {
    
    @Autowired
    private QueryProperties properties;
    
    @Bean
    @ConditionalOnMissingBean
    public SqlBuilder sqlBuilder() {
        return new SqlBuilder(properties.getDatabaseDialect());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public QueryExecutor queryExecutor(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder) {
        return new QueryExecutorImpl(jdbcTemplate, sqlBuilder);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "query.registration.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
    public QueryRequestParser queryRequestParser() {
        return new QueryRequestParser();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "query.registration.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
    public QueryResponseBuilder queryResponseBuilder(ObjectMapper objectMapper) {
        return new QueryResponseBuilder(objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "query.registration.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
    public QueryController queryController(
            QueryExecutor queryExecutor,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        return new QueryController(queryExecutor, requestParser, responseBuilder);
    }
}