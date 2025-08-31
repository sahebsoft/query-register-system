package com.balasam.oasis.common.query.example;

import com.balasam.oasis.common.query.core.execution.QueryExecutorImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Initialize example queries on application startup
 */
@Component
public class QueryInitializer {
    
    @Bean
    CommandLineRunner initializeQueries(QueryExecutorImpl executor, ExampleQueryConfig config) {
        return args -> {
            // Register example queries
            executor.registerQuery(config.userDashboardQuery());
            executor.registerQuery(config.simpleUserQuery());
            
            System.out.println("✅ Query Registration System started successfully!");
            System.out.println("📊 Registered queries: userDashboard, users");
            System.out.println("🔗 API Documentation: http://localhost:8080/swagger-ui.html");
            System.out.println("🔗 H2 Console: http://localhost:8080/h2-console");
            System.out.println("\n📌 Example API calls:");
            System.out.println("   GET http://localhost:8080/api/query/users");
            System.out.println("   GET http://localhost:8080/api/query/userDashboard?filter.status=ACTIVE&sort=lifetimeValue.desc");
        };
    }
}