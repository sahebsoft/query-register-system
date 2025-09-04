package com.balasam.oasis.common.query.example;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.balasam.oasis.common.query.core.definition.FilterOp;
import com.balasam.oasis.common.query.core.definition.SortDir;
import com.balasam.oasis.common.query.core.execution.QueryExecutor;
import com.balasam.oasis.common.query.core.result.QueryResult;
import com.balasam.oasis.common.query.core.result.Row;

/**
 * Test runner for metadata caching with 10 different query variants
 */
@Component
public class TestQueryRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestQueryRunner.class);

    @Autowired
    private QueryExecutor queryExecutor;

    @Override
    public void run(String... args) throws Exception {
        // Skip if not in test mode
        if (!Arrays.asList(args).contains("--test-metadata-cache")) {
            return;
        }

        log.info("=== Starting Metadata Cache Test Suite ===");
        Thread.sleep(2000); // Wait for system to stabilize

        try {
            // Test 1: Simple query with no parameters
            test1_SimpleQuery();

            // Test 2: Query with parameters
            test2_QueryWithParameters();

            // Test 3: Query with filters
            test3_QueryWithFilters();

            // Test 4: Query with sorting
            test4_QueryWithSorting();

            // Test 5: Query with pagination
            test5_QueryWithPagination();

            // Test 6: Complex query with all features
            test6_ComplexQuery();

            // Test 7: Multiple executions (cache reuse test)
            test7_CacheReuseTest();

            // Test 8: Query with IN filter
            test8_QueryWithInFilter();

            // Test 9: Query with range filters
            test9_QueryWithRangeFilters();

            // Test 10: Performance comparison test
            test10_PerformanceTest();

            log.info("=== All Tests Completed Successfully ===");

        } catch (Exception e) {
            log.error("Test failed: ", e);
        }
    }

    private void test1_SimpleQuery() {
        log.info("\n--- Test 1: Simple Query (No Parameters) ---");
        long start = System.currentTimeMillis();

        QueryResult result = queryExecutor.execute("employees")
                .withPagination(0, 5)
                .includeMetadata(false)
                .execute();

        result.getRows().forEach(row -> {
            row.getString("deptId");
        });

        long elapsed = System.currentTimeMillis() - start;
        log.info("Execution time: {} ms", elapsed);
        log.info("Rows returned: {}", result.getRows().size());

        if (!result.getRows().isEmpty()) {
            Row firstRow = result.getRows().get(0);
            log.info("First employee: ID={}, Name={} {}",
                    firstRow.get("employeeId"),
                    firstRow.get("firstName"),
                    firstRow.get("lastName"));
        }
    }

    private void test2_QueryWithParameters() {
        log.info("\n--- Test 2: Query with Parameters ---");
        long start = System.currentTimeMillis();

        QueryResult result = queryExecutor.execute("employees")
                .withParam("minSalary", new BigDecimal("50000"))
                .withParam("deptId", 30) // Sales department
                .withPagination(0, 5)
                .includeMetadata(false)
                .execute();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Execution time: {} ms", elapsed);
        log.info("Rows returned: {}", result.getRows().size());
    }

    private void test3_QueryWithFilters() {
        log.info("\n--- Test 3: Query with Filters ---");
        long start = System.currentTimeMillis();

        QueryResult result = queryExecutor.execute("employees")
                .withFilter("salary", FilterOp.GREATER_THAN_OR_EQUAL, new BigDecimal("60000"))
                .withFilter("lastName", FilterOp.LIKE, "%Smith%")
                .withPagination(0, 10)
                .includeMetadata(true)
                .execute();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Execution time: {} ms", elapsed);
        log.info("Rows returned: {}", result.getRows().size());

        if (result.getMetadata() != null && result.getMetadata().getAppliedFilters() != null) {
            log.info("Applied filters: {}", result.getMetadata().getAppliedFilters().size());
        }
    }

    private void test4_QueryWithSorting() {
        log.info("\n--- Test 4: Query with Sorting ---");
        long start = System.currentTimeMillis();

        QueryResult result = queryExecutor.execute("employees")
                .withSort("salary", SortDir.DESC)
                .withSort("lastName", SortDir.ASC)
                .withPagination(0, 5)
                .includeMetadata(false)
                .execute();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Execution time: {} ms", elapsed);
        log.info("Rows returned: {}", result.getRows().size());

        if (!result.getRows().isEmpty()) {
            Row firstRow = result.getRows().get(0);
            log.info("Highest paid: {} {} - ${}",
                    firstRow.get("firstName"),
                    firstRow.get("lastName"),
                    firstRow.get("salary"));
        }
    }

    private void test5_QueryWithPagination() {
        log.info("\n--- Test 5: Query with Pagination ---");

        // Page 1
        long start = System.currentTimeMillis();
        QueryResult page1 = queryExecutor.execute("employees")
                .withPagination(0, 10)
                .includeMetadata(true)
                .execute();
        long elapsed1 = System.currentTimeMillis() - start;

        // Page 2
        start = System.currentTimeMillis();
        QueryResult page2 = queryExecutor.execute("employees")
                .withPagination(10, 20)
                .includeMetadata(true)
                .execute();
        long elapsed2 = System.currentTimeMillis() - start;

        log.info("Page 1: {} rows in {} ms", page1.getRows().size(), elapsed1);
        log.info("Page 2: {} rows in {} ms", page2.getRows().size(), elapsed2);

        if (page1.getMetadata() != null && page1.getMetadata().getPagination() != null) {
            log.info("Total records: {}", page1.getMetadata().getPagination().getTotal());
        }
    }

    private void test6_ComplexQuery() {
        log.info("\n--- Test 6: Complex Query (All Features) ---");
        long start = System.currentTimeMillis();

        QueryResult result = queryExecutor.execute("employees")
                .withParam("minSalary", new BigDecimal("40000"))
                .withParam("deptId", 60) // IT department
                .withFilter("email", FilterOp.LIKE, "%@company.com")
                .withFilter("hireDate", FilterOp.GREATER_THAN_OR_EQUAL, LocalDate.of(2020, 1, 1))
                .withSort("salary", SortDir.DESC)
                .withSort("hireDate", SortDir.ASC)
                .withPagination(0, 15)
                .includeMetadata(true)
                .execute();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Execution time: {} ms", elapsed);
        log.info("Rows returned: {}", result.getRows().size());

        if (result.getMetadata() != null) {
            log.info("Metadata included: {}", result.getMetadata() != null);
            if (result.getMetadata().getPerformance() != null) {
                log.info("Query execution time from metadata: {} ms",
                        result.getMetadata().getPerformance().getExecutionTimeMs());
            }
        }
    }

    private void test7_CacheReuseTest() {
        log.info("\n--- Test 7: Cache Reuse Test (5 iterations) ---");

        long totalTime = 0;
        for (int i = 1; i <= 5; i++) {
            long start = System.currentTimeMillis();

            QueryResult result = queryExecutor.execute("employees")
                    .withParam("minSalary", new BigDecimal("55000"))
                    .withPagination(0, 10)
                    .includeMetadata(false)
                    .execute();

            long elapsed = System.currentTimeMillis() - start;
            totalTime += elapsed;
            log.info("Iteration {}: {} ms, {} rows", i, elapsed, result.getRows().size());
        }

        log.info("Average execution time: {} ms", totalTime / 5);
        log.info("Note: First execution builds cache, subsequent should be faster");
    }

    private void test8_QueryWithInFilter() {
        log.info("\n--- Test 8: Query with IN Filter ---");
        long start = System.currentTimeMillis();

        QueryResult result = queryExecutor.execute("employees")
                .withFilter("departmentName", FilterOp.IN, Arrays.asList("IT", "Sales", "Marketing"))
                .withPagination(0, 20)
                .includeMetadata(false)
                .execute();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Execution time: {} ms", elapsed);
        log.info("Rows returned: {}", result.getRows().size());

        // Group by department
        Map<String, Integer> deptCounts = new HashMap<>();
        for (Row row : result.getRows()) {
            String dept = (String) row.get("departmentName");
            deptCounts.put(dept, deptCounts.getOrDefault(dept, 0) + 1);
        }
        log.info("Department distribution: {}", deptCounts);
    }

    private void test9_QueryWithRangeFilters() {
        log.info("\n--- Test 9: Query with Range Filters ---");
        long start = System.currentTimeMillis();

        QueryResult result = queryExecutor.execute("employees")
                .withFilter("salary", FilterOp.GREATER_THAN_OR_EQUAL, new BigDecimal("50000"))
                .withFilter("salary", FilterOp.LESS_THAN_OR_EQUAL, new BigDecimal("80000"))
                .withFilter("hireDate", FilterOp.GREATER_THAN_OR_EQUAL, LocalDate.of(2019, 1, 1))
                .withFilter("hireDate", FilterOp.LESS_THAN_OR_EQUAL, LocalDate.of(2023, 12, 31))
                .withPagination(0, 15)
                .includeMetadata(true)
                .execute();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Execution time: {} ms", elapsed);
        log.info("Rows in salary range $50K-$80K hired 2019-2023: {}", result.getRows().size());
    }

    private void test10_PerformanceTest() {
        log.info("\n--- Test 10: Performance Comparison Test ---");

        // First run - cold cache
        log.info("Cold cache test (first execution):");
        long coldStart = System.currentTimeMillis();

        QueryResult coldResult = queryExecutor.execute("departmentStats")
                .withPagination(0, 100)
                .includeMetadata(false)
                .execute();

        long coldTime = System.currentTimeMillis() - coldStart;
        log.info("Cold cache execution: {} ms for {} rows", coldTime, coldResult.getRows().size());

        // Warm cache test - run same query 10 times
        log.info("\nWarm cache test (10 executions):");
        long totalWarmTime = 0;
        for (int i = 1; i <= 10; i++) {
            long warmStart = System.currentTimeMillis();

            QueryResult warmResult = queryExecutor.execute("departmentStats")
                    .withPagination(0, 100)
                    .includeMetadata(false)
                    .execute();

            long warmTime = System.currentTimeMillis() - warmStart;
            totalWarmTime += warmTime;

            if (i == 1 || i == 5 || i == 10) {
                log.info("  Execution {}: {} ms", i, warmTime);
            }
        }

        long avgWarmTime = totalWarmTime / 10;
        log.info("Average warm cache execution: {} ms", avgWarmTime);

        double improvement = ((double) (coldTime - avgWarmTime) / coldTime) * 100;
        log.info("Performance improvement: {}%", String.format("%.2f", improvement));

        // Large result set test
        log.info("\nLarge result set test:");
        long largeStart = System.currentTimeMillis();

        QueryResult largeResult = queryExecutor.execute("employees")
                .withPagination(0, 500) // Large page size
                .includeMetadata(false)
                .execute();

        long largeTime = System.currentTimeMillis() - largeStart;
        log.info("Large result set ({} rows): {} ms", largeResult.getRows().size(), largeTime);
        log.info("Average time per row: {} ms",
                String.format("%.3f", (double) largeTime / largeResult.getRows().size()));
    }
}