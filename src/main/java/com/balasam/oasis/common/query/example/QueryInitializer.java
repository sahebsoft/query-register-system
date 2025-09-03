package com.balasam.oasis.common.query.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.balasam.oasis.common.query.core.execution.QueryExecutorImpl;

/**
 * Initialize example queries on application startup
 */
@Component
public class QueryInitializer {
    
    @Autowired
    private Environment env;
    
    @Bean
    CommandLineRunner initializeQueries(QueryExecutorImpl executor,  
                                       @Autowired(required = false) OracleHRQueryConfig oracleConfig) {
        return args -> {
            // Register example queries
            
            // Always register Oracle queries (Oracle is now default)
            if (oracleConfig != null) {
                executor.registerQuery(oracleConfig.employeesQuery());
                executor.registerQuery(oracleConfig.departmentStatsQuery());
            }
            
            System.out.println("✅ Oracle Query Registration System started successfully!");
            System.out.println("📊 Registered queries: userDashboard, users, employees, departmentStats");
            System.out.println("🔗 API Documentation: http://localhost:8080/swagger-ui.html");
            System.out.println("\n📌 Oracle HR API calls:");
            System.out.println("   GET http://localhost:8080/api/query/employees");
            System.out.println("   GET http://localhost:8080/api/query/departmentStats");
            System.out.println("   GET http://localhost:8080/api/query/employees?filter.salary.gt=5000&sort=salary.desc");
            System.out.println("   GET http://localhost:8080/api/query/departmentStats?param.country=United%20States%20of%20America");
            System.out.println("\n📌 Example API calls:");
            System.out.println("   GET http://localhost:8080/api/query/users");
            System.out.println("   GET http://localhost:8080/api/query/userDashboard?filter.status=ACTIVE&sort=lifetimeValue.desc");
        };
    }
}