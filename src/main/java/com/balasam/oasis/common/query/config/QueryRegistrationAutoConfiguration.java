package com.balasam.oasis.common.query.config;

import com.balasam.oasis.common.query.core.execution.QueryExecutor;
import com.balasam.oasis.common.query.core.execution.QueryExecutorImpl;
import com.balasam.oasis.common.query.rest.QueryController;
import com.balasam.oasis.common.query.rest.QueryRequestParser;
import com.balasam.oasis.common.query.rest.QueryResponseBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Auto-configuration for Query Registration System
 */
@AutoConfiguration
@ConditionalOnClass({JdbcTemplate.class})
@EnableConfigurationProperties(QueryProperties.class)
@ComponentScan(basePackages = "com.balasam.oasis.common.query")
public class QueryRegistrationAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public QueryExecutor queryExecutor(JdbcTemplate jdbcTemplate) {
        return new QueryExecutorImpl(jdbcTemplate);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public QueryRequestParser queryRequestParser() {
        return new QueryRequestParser();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public QueryResponseBuilder queryResponseBuilder(ObjectMapper objectMapper) {
        return new QueryResponseBuilder(objectMapper);
    }
    
    @Configuration
    @ConditionalOnProperty(prefix = "query.registration.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class RestApiConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public QueryController queryController(
                QueryExecutor queryExecutor,
                QueryRequestParser requestParser,
                QueryResponseBuilder responseBuilder) {
            return new QueryController(queryExecutor, requestParser, responseBuilder);
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    public QueryRegistrationConfigurer queryRegistrationConfigurer(QueryProperties properties) {
        return new QueryRegistrationConfigurer(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public GlobalProcessors globalProcessors() {
        return GlobalProcessors.builder().build();
    }
}