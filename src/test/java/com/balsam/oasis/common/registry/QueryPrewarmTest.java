package com.balsam.oasis.common.registry;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.balsam.oasis.common.registry.engine.query.QueryExecutorImpl;
import com.balsam.oasis.common.registry.engine.query.QueryRegistryImpl;
import com.balsam.oasis.common.registry.domain.common.QueryResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Test class that executes all registered queries with empty parameters
 * to prewarm the system and validate queries are working.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:oracle:thin:@localhost:1521:ORCLCDB",
    "spring.datasource.username=hr",
    "spring.datasource.password=oracle",
    "spring.datasource.driver-class-name=oracle.jdbc.OracleDriver"
})
public class QueryPrewarmTest {

    private static final Logger log = LoggerFactory.getLogger(QueryPrewarmTest.class);

    @Autowired
    private QueryRegistryImpl queryRegistry;

    @Autowired
    private QueryExecutorImpl queryExecutor;

    @Test
    public void prewarmAllQueries() {
        Set<String> queryNames = queryRegistry.getQueryNames();

        log.info("========================================");
        log.info("Starting query prewarm test");
        log.info("Found {} registered queries", queryNames.size());
        log.info("========================================");

        Map<String, TestResult> results = new HashMap<>();

        for (String queryName : queryNames) {
            log.info("\nExecuting query: {}", queryName);
            log.info("----------------------------------------");

            long startTime = System.currentTimeMillis();
            TestResult result = new TestResult();
            result.queryName = queryName;

            try {
                // Execute with empty params and minimal pagination
                QueryResult queryResult = queryExecutor.execute(queryName)
                    .withPagination(0, 1) // Only fetch 1 row to minimize load
                    .includeMetadata(false) // Skip metadata for performance
                    .execute();

                long executionTime = System.currentTimeMillis() - startTime;

                result.success = true;
                result.executionTimeMs = executionTime;
                result.rowCount = queryResult.size();

                log.info("✓ Query '{}' executed successfully", queryName);
                log.info("  - Execution time: {} ms", executionTime);
                log.info("  - Rows returned: {}", result.rowCount);

            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;

                result.success = false;
                result.executionTimeMs = executionTime;
                result.errorMessage = e.getMessage();

                log.error("✗ Query '{}' failed", queryName);
                log.error("  - Error: {}", e.getMessage());
                log.error("  - Execution time: {} ms", executionTime);

                // Log the full stack trace in debug mode
                log.debug("Full stack trace:", e);
            }

            results.put(queryName, result);
        }

        // Print summary
        printSummary(results);
    }

    private void printSummary(Map<String, TestResult> results) {
        log.info("\n========================================");
        log.info("QUERY PREWARM TEST SUMMARY");
        log.info("========================================");

        int successCount = 0;
        int failureCount = 0;
        long totalTime = 0;

        for (TestResult result : results.values()) {
            if (result.success) {
                successCount++;
            } else {
                failureCount++;
            }
            totalTime += result.executionTimeMs;
        }

        log.info("Total queries tested: {}", results.size());
        log.info("Successful: {} ({}%)", successCount,
            results.isEmpty() ? 0 : (successCount * 100 / results.size()));
        log.info("Failed: {} ({}%)", failureCount,
            results.isEmpty() ? 0 : (failureCount * 100 / results.size()));
        log.info("Total execution time: {} ms", totalTime);

        if (!results.isEmpty()) {
            log.info("Average execution time: {} ms", totalTime / results.size());
        }

        // List failed queries if any
        if (failureCount > 0) {
            log.info("\n----------------------------------------");
            log.info("FAILED QUERIES:");
            for (Map.Entry<String, TestResult> entry : results.entrySet()) {
                if (!entry.getValue().success) {
                    log.info("  - {}: {}", entry.getKey(), entry.getValue().errorMessage);
                }
            }
        }

        // List successful queries with execution times
        if (successCount > 0) {
            log.info("\n----------------------------------------");
            log.info("SUCCESSFUL QUERIES:");
            results.entrySet().stream()
                .filter(e -> e.getValue().success)
                .sorted((a, b) -> Long.compare(b.getValue().executionTimeMs, a.getValue().executionTimeMs))
                .forEach(entry -> {
                    log.info("  - {} ({} ms, {} rows)",
                        entry.getKey(),
                        entry.getValue().executionTimeMs,
                        entry.getValue().rowCount);
                });
        }

        log.info("\n========================================");
    }

    /**
     * Inner class to hold test results
     */
    private static class TestResult {
        String queryName;
        boolean success;
        long executionTimeMs;
        int rowCount;
        String errorMessage;
    }
}