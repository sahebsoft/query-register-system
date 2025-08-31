package com.balasam.oasis.common.query.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable Query Registration System
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(QueryRegistrationAutoConfiguration.class)
public @interface EnableQueryRegistration {
    
    /**
     * Base packages to scan for query definitions
     */
    String[] scanPackages() default {};
    
    /**
     * Enable REST API endpoints
     */
    boolean enableRestApi() default true;
    
    /**
     * Enable caching
     */
    boolean enableCache() default true;
    
    /**
     * Enable security integration
     */
    boolean enableSecurity() default true;
    
    /**
     * Enable metrics collection
     */
    boolean enableMetrics() default true;
    
    /**
     * Enable Swagger/OpenAPI documentation
     */
    boolean enableSwagger() default true;
}