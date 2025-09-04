package com.balasam.oasis.common.query.core.execution;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Test Oracle driver behavior for metadata retrieval to determine the best
 * approach
 * for pre-warming queries without executing them.
 * 
 * Based on METADATA.md recommendations, we test:
 * 1. PreparedStatement.getMetaData() without parameters
 * 2. PreparedStatement.getMetaData() with dummy parameters
 * 3. Execute with WHERE 1=0 (current approach)
 * 4. Execute with WHERE ROWNUM = 0 (Oracle-specific)
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:oracle:thin:@localhost:31521:XE?oracle.jdbc.timezoneAsRegion=false",
        "spring.datasource.username=hr",
        "spring.datasource.password=hr",
        "query.registration.metadata.cache.prewarm=false", // Disable pre-warming to avoid System.exit
        "query.registration.metadata.cache.fail-on-error=false" // Don't fail on errors
})
public class OracleMetadataRetrievalTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Test queries from the actual system
    private static final Map<String, String> TEST_QUERIES = new LinkedHashMap<>();

    static {
        // Simple query with parameters
        TEST_QUERIES.put("simple_with_param",
                "SELECT ? test_param , employees.*  FROM employees WHERE department_id = ?");

        // Query with multiple parameters
        TEST_QUERIES.put("multiple_params",
                "SELECT * FROM employees WHERE department_id = ? AND salary > ?");

        // Complex JOIN query (like employees query in the system)
        TEST_QUERIES.put("complex_join",
                """
                        SELECT e.employee_id, e.first_name, e.last_name, e.email,
                               j.job_title, e.salary, d.department_name, l.city
                        FROM employees e
                        LEFT JOIN jobs j ON e.job_id = j.job_id
                        LEFT JOIN departments d ON e.department_id = d.department_id
                        LEFT JOIN locations l ON d.location_id = l.location_id
                        WHERE e.department_id = ? AND e.salary >= ?
                        """);

        // Aggregation query (like departmentStats)
        TEST_QUERIES.put("aggregation",
                """
                        SELECT d.department_id, d.department_name,
                               COUNT(e.employee_id) as employee_count,
                               AVG(e.salary) as avg_salary
                        FROM departments d
                        LEFT JOIN employees e ON d.department_id = e.department_id
                        WHERE d.location_id = ?
                        GROUP BY d.department_id, d.department_name
                        HAVING COUNT(e.employee_id) > 0
                        """);

        // UNION query (like testUnion)
        TEST_QUERIES.put("union_query",
                """
                        SELECT employee_id FROM employees WHERE department_id = ?
                        UNION
                        SELECT employee_id FROM employees WHERE job_id = ?
                        """);

        // Query with CASE statement
        TEST_QUERIES.put("case_statement",
                """
                        SELECT employee_id, first_name,
                               CASE
                                   WHEN salary > ? THEN 'HIGH'
                                   WHEN salary > ? THEN 'MEDIUM'
                                   ELSE 'LOW'
                               END as salary_grade
                        FROM employees
                        WHERE department_id = ?
                        """);

        // Subquery
        TEST_QUERIES.put("subquery",
                """
                        SELECT * FROM employees
                        WHERE department_id IN (
                            SELECT department_id FROM departments WHERE location_id = ?
                        )
                        """);

        // Named parameters (using ? for JDBC)
        TEST_QUERIES.put("with_named_params",
                "SELECT * FROM employees WHERE job_id = ? AND manager_id = ?");
    }

    @Test
    public void testMetadataRetrievalMethods() throws SQLException {
        System.out.println("=== Oracle Metadata Retrieval Test Results ===\n");

        for (Map.Entry<String, String> entry : TEST_QUERIES.entrySet()) {
            String queryName = entry.getKey();
            String sql = entry.getValue();

            System.out.println("Testing Query: " + queryName);
            // System.out
            // .println("SQL: " + sql.replaceAll("\\s+", " ").substring(0, Math.min(100,
            // sql.length())) + "...\n");

            testMethod1_PreparedStatementOnly(queryName, sql);
            testMethod2_PreparedStatementWithParams(queryName, sql);
            testMethod3_Where1Equals0(queryName, sql);
            testMethod4_WhereRownum0(queryName, sql);

            System.out.println("----------------------------------------\n");
        }

        printSummary();
    }

    private Map<String, Map<String, TestResult>> results = new HashMap<>();

    private static class TestResult {
        boolean success;
        long executionTimeMs;
        int columnCount;
        String errorMessage;

        TestResult(boolean success, long time, int columns, String error) {
            this.success = success;
            this.executionTimeMs = time;
            this.columnCount = columns;
            this.errorMessage = error;
        }
    }

    /**
     * Method 1: Try PreparedStatement.getMetaData() without setting parameters
     * This is the fastest as it doesn't execute the query
     */
    private void testMethod1_PreparedStatementOnly(String queryName, String sql) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            // Try to get metadata WITHOUT setting parameters
            ResultSetMetaData metadata = ps.getMetaData();

            long executionTime = System.currentTimeMillis() - startTime;

            if (metadata != null) {
                int columnCount = metadata.getColumnCount();
                var name = metadata.getColumnName(1);
                var type = metadata.getColumnType(1);
                System.out.println(String.format("%s type is %s", name, type));
                recordResult(queryName, "Method1_PS_NoParams", true, executionTime, columnCount, null);
                System.out.println(
                        "✓ Method 1 (PS.getMetaData): SUCCESS - " + columnCount + " columns, " + executionTime + "ms");
            } else {
                recordResult(queryName, "Method1_PS_NoParams", false, executionTime, 0, "getMetaData() returned null");
                System.out.println("✗ Method 1 (PS.getMetaData): FAILED - returned null");
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordResult(queryName, "Method1_PS_NoParams", false, executionTime, 0, e.getMessage());
            System.out.println("✗ Method 1 (PS.getMetaData): ERROR - " + e.getMessage());
        }
    }

    /**
     * Method 2: PreparedStatement.getMetaData() with dummy parameters set
     */
    private void testMethod2_PreparedStatementWithParams(String queryName, String sql) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            // First try without params
            ResultSetMetaData metadata = ps.getMetaData();

            if (metadata == null) {
                // Set dummy parameters
                ParameterMetaData pmd = ps.getParameterMetaData();
                int paramCount = pmd.getParameterCount();

                for (int i = 1; i <= paramCount; i++) {
                    // Set dummy values based on common types
                    try {
                        int paramType = pmd.getParameterType(i);
                        setDummyParameter(ps, i, paramType);
                    } catch (SQLException e) {
                        // If can't get type, use generic string
                        ps.setString(i, "DUMMY");
                    }
                }

                // Try again with parameters set
                metadata = ps.getMetaData();
            }

            long executionTime = System.currentTimeMillis() - startTime;

            if (metadata != null) {
                int columnCount = metadata.getColumnCount();
                recordResult(queryName, "Method2_PS_WithParams", true, executionTime, columnCount, null);
                System.out.println(
                        "✓ Method 2 (PS with params): SUCCESS - " + columnCount + " columns, " + executionTime + "ms");
            } else {
                recordResult(queryName, "Method2_PS_WithParams", false, executionTime, 0,
                        "getMetaData() returned null even with params");
                System.out.println("✗ Method 2 (PS with params): FAILED - still null");
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordResult(queryName, "Method2_PS_WithParams", false, executionTime, 0, e.getMessage());
            System.out.println("✗ Method 2 (PS with params): ERROR - " + e.getMessage());
        }
    }

    /**
     * Method 3: Execute with WHERE 1=0 (wrapped approach)
     * This executes the query but returns no rows
     */
    private void testMethod3_Where1Equals0(String queryName, String sql) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            // Wrap query with WHERE 1=0 to avoid syntax issues
            String modifiedSql = "SELECT * FROM (" + sql + ") WHERE 1=0";

            try (PreparedStatement ps = conn.prepareStatement(modifiedSql)) {
                // Set dummy parameters
                ParameterMetaData pmd = ps.getParameterMetaData();
                int paramCount = pmd.getParameterCount();

                for (int i = 1; i <= paramCount; i++) {
                    try {
                        ParameterMetaData pmd2 = ps.getParameterMetaData();
                        int paramType = pmd2.getParameterType(i);
                        setDummyParameter(ps, i, paramType);
                    } catch (SQLException e) {
                        // If can't get type, use generic value
                        ps.setObject(i, getDummyValue(i));
                    }
                }

                // Execute query
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    long executionTime = System.currentTimeMillis() - startTime;

                    int columnCount = metadata.getColumnCount();
                    recordResult(queryName, "Method3_Where_1=0", true, executionTime, columnCount, null);
                    System.out.println(
                            "✓ Method 3 (WHERE 1=0): SUCCESS - " + columnCount + " columns, " + executionTime + "ms");
                }
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordResult(queryName, "Method3_Where_1=0", false, executionTime, 0, e.getMessage());
            System.out.println("✗ Method 3 (WHERE 1=0): ERROR - " + e.getMessage());
        }
    }

    /**
     * Method 4: Execute with WHERE ROWNUM = 0 (Oracle-specific optimization)
     * This should be faster than WHERE 1=0 for Oracle
     */
    private void testMethod4_WhereRownum0(String queryName, String sql) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            // Wrap query with ROWNUM = 0
            String modifiedSql = "SELECT * FROM (" + sql + ") WHERE ROWNUM = 0";

            try (PreparedStatement ps = conn.prepareStatement(modifiedSql)) {
                // Set dummy parameters
                ParameterMetaData pmd = ps.getParameterMetaData();
                int paramCount = pmd.getParameterCount();

                for (int i = 1; i <= paramCount; i++) {
                    try {
                        ParameterMetaData pmd2 = ps.getParameterMetaData();
                        int paramType = pmd2.getParameterType(i);
                        setDummyParameter(ps, i, paramType);
                    } catch (SQLException e) {
                        // If can't get type, use generic value
                        ps.setObject(i, getDummyValue(i));
                    }
                }

                // Execute query
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    long executionTime = System.currentTimeMillis() - startTime;

                    int columnCount = metadata.getColumnCount();
                    recordResult(queryName, "Method4_Rownum=0", true, executionTime, columnCount, null);
                    System.out.println(
                            "✓ Method 4 (ROWNUM=0): SUCCESS - " + columnCount + " columns, " + executionTime + "ms");
                }
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordResult(queryName, "Method4_Rownum=0", false, executionTime, 0, e.getMessage());
            System.out.println("✗ Method 4 (ROWNUM=0): ERROR - " + e.getMessage());
        }
    }

    private void setDummyParameter(PreparedStatement ps, int index, int sqlType) throws SQLException {
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.CHAR:
                ps.setString(index, "DUMMY");
                break;
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.SMALLINT:
                ps.setInt(index, 0);
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.FLOAT:
            case Types.DOUBLE:
                ps.setDouble(index, 0.0);
                break;
            case Types.DATE:
                ps.setDate(index, java.sql.Date.valueOf(LocalDate.now()));
                break;
            case Types.TIMESTAMP:
                ps.setTimestamp(index, Timestamp.valueOf(LocalDateTime.now()));
                break;
            default:
                ps.setObject(index, null);
        }
    }

    private Object getDummyValue(int index) {
        // Return different types of dummy values based on position
        switch (index % 4) {
            case 0:
                return 1; // Integer
            case 1:
                return "DUMMY"; // String
            case 2:
                return 100.0; // Double
            case 3:
                return java.sql.Date.valueOf(LocalDate.now()); // Date
            default:
                return null;
        }
    }

    private void recordResult(String queryName, String method, boolean success, long time, int columns, String error) {
        results.computeIfAbsent(queryName, k -> new HashMap<>())
                .put(method, new TestResult(success, time, columns, error));
    }

    private void printSummary() {
        System.out.println("\n=== SUMMARY ===\n");

        // Calculate success rates and average times for each method
        Map<String, Integer> successCount = new HashMap<>();
        Map<String, Long> totalTime = new HashMap<>();
        Map<String, Integer> queryCount = new HashMap<>();

        for (Map<String, TestResult> queryResults : results.values()) {
            for (Map.Entry<String, TestResult> entry : queryResults.entrySet()) {
                String method = entry.getKey();
                TestResult result = entry.getValue();

                queryCount.merge(method, 1, Integer::sum);
                if (result.success) {
                    successCount.merge(method, 1, Integer::sum);
                }
                totalTime.merge(method, result.executionTimeMs, Long::sum);
            }
        }

        System.out.println("Method Performance Comparison:");
        System.out.println("-------------------------------");

        for (String method : new String[] { "Method1_PS_NoParams", "Method2_PS_WithParams", "Method3_Where_1=0",
                "Method4_Rownum=0" }) {
            int success = successCount.getOrDefault(method, 0);
            int total = queryCount.getOrDefault(method, 0);
            long avgTime = total > 0 ? totalTime.get(method) / total : 0;

            System.out.printf("%s:\n", method);
            System.out.printf("  Success Rate: %d/%d (%.1f%%)\n", success, total, (success * 100.0 / total));
            System.out.printf("  Avg Time: %dms\n\n", avgTime);
        }

        System.out.println("\nRECOMMENDATION:");
        System.out.println("----------------");

        // Determine best method
        String bestMethod = null;
        int bestScore = -1;

        for (String method : new String[] { "Method1_PS_NoParams", "Method2_PS_WithParams" }) {
            int success = successCount.getOrDefault(method, 0);
            long avgTime = queryCount.get(method) > 0 ? totalTime.get(method) / queryCount.get(method) : Long.MAX_VALUE;

            // Score based on success rate and speed (lower time is better)
            int score = success * 1000 - (int) avgTime;

            if (score > bestScore) {
                bestScore = score;
                bestMethod = method;
            }
        }

        if (bestMethod != null) {
            if (bestMethod.equals("Method1_PS_NoParams")) {
                System.out.println("Use PreparedStatement.getMetaData() without parameters as primary method.");
                System.out.println("This avoids query execution entirely and is the fastest approach.");
            } else if (bestMethod.equals("Method2_PS_WithParams")) {
                System.out.println("Use PreparedStatement.getMetaData() with dummy parameters as primary method.");
                System.out.println("Some queries require parameters to return metadata.");
            }
        } else {
            System.out.println("No successful methods found - Oracle database may not be running.");
            System.out.println("Cannot determine best approach without a working database connection.");
        }

        System.out.println("\nFallback strategy:");
        System.out.println("1. Try PreparedStatement.getMetaData() without params");
        System.out.println("2. If null, try with dummy params");
        System.out.println("3. If still null, use WHERE ROWNUM = 0 (faster than WHERE 1=0 for Oracle)");
    }
}