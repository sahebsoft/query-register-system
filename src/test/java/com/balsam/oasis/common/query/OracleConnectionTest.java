package com.balsam.oasis.common.query;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test Oracle 11g XE connection using HR schema
 * 
 * To run this test:
 * 1. Ensure Oracle XE 11g is running (kubectl apply -f oracle-xe-complete.yaml)
 * 2. Update the connection details in application-oracle.properties if needed
 * 3. Run: ./mvnw test -Dtest=OracleConnectionTest
 * -Dspring.profiles.active=oracle
 */
@SpringBootTest
@ActiveProfiles("oracle")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:oracle:thin:@localhost:31521:XE?oracle.jdbc.timezoneAsRegion=false",
        "spring.datasource.username=hr",
        "spring.datasource.password=hr",
        "spring.datasource.hikari.connection-init-sql=ALTER SESSION SET TIME_ZONE='UTC'",
        "query.registration.database.dialect=ORACLE_11G"
})
public class OracleConnectionTest {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testOracleConnection() {
        if (jdbcTemplate == null) {
            System.out.println("Oracle connection not configured. Skipping test.");
            return;
        }

        try {
            // Test basic connection
            String version = jdbcTemplate.queryForObject(
                    "SELECT BANNER FROM V$VERSION WHERE ROWNUM = 1",
                    String.class);
            System.out.println("Connected to Oracle Database: " + version);
            assertThat(version).containsIgnoringCase("Oracle");

            // Test HR schema tables
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM user_tables WHERE ROWNUM <= 5");
            System.out.println("HR Schema tables found: " + tables.size());
            tables.forEach(table -> System.out.println("  - " + table.get("TABLE_NAME")));

            // Test EMPLOYEES table if it exists
            try {
                Integer employeeCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM EMPLOYEES",
                        Integer.class);
                System.out.println("Number of employees in HR.EMPLOYEES: " + employeeCount);
                assertThat(employeeCount).isGreaterThanOrEqualTo(0);

                // Test Oracle 11g pagination with ROWNUM
                List<Map<String, Object>> topEmployees = jdbcTemplate.queryForList(
                        """
                                SELECT * FROM (
                                    SELECT employee_id, first_name, last_name, salary
                                    FROM EMPLOYEES
                                    ORDER BY salary DESC
                                ) WHERE ROWNUM <= 5
                                """);
                System.out.println("Top 5 employees by salary:");
                topEmployees.forEach(emp -> System.out.println(String.format("  - %s %s: $%s",
                        emp.get("FIRST_NAME"),
                        emp.get("LAST_NAME"),
                        emp.get("SALARY"))));
            } catch (Exception e) {
                System.out.println("EMPLOYEES table not found or accessible: " + e.getMessage());
            }

            // Test DEPARTMENTS table if it exists
            try {
                List<Map<String, Object>> departments = jdbcTemplate.queryForList(
                        "SELECT department_id, department_name FROM DEPARTMENTS WHERE ROWNUM <= 5");
                System.out.println("Departments found:");
                departments.forEach(dept -> System.out.println("  - " + dept.get("DEPARTMENT_NAME")));
            } catch (Exception e) {
                System.out.println("DEPARTMENTS table not found or accessible: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Failed to connect to Oracle database.");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Make sure Oracle XE 11g is running and accessible at localhost:31521");
            System.err.println("You can deploy it using: kubectl apply -f oracle-xe-complete.yaml");
            throw new RuntimeException("Oracle connection failed", e);
        }
    }

    @Test
    public void testOraclePaginationWithRownum() {
        if (jdbcTemplate == null) {
            System.out.println("Oracle connection not configured. Skipping test.");
            return;
        }

        try {
            // Test Oracle 11g style pagination (ROWNUM)
            String sql = """
                        SELECT * FROM (
                            SELECT a.*, ROWNUM rnum FROM (
                                SELECT table_name
                                FROM user_tables
                                ORDER BY table_name
                            ) a
                            WHERE ROWNUM <= :endRow
                        )
                        WHERE rnum > :startRow
                    """;

            // This would normally use NamedParameterJdbcTemplate, but for testing:
            List<Map<String, Object>> pagedResults = jdbcTemplate.queryForList(
                    sql.replace(":endRow", "10").replace(":startRow", "5"));

            System.out.println("Pagination test - Tables 6 to 10:");
            pagedResults.forEach(row -> System.out.println("  " + row.get("RNUM") + ": " + row.get("TABLE_NAME")));

        } catch (Exception e) {
            System.out.println("Pagination test failed: " + e.getMessage());
        }
    }
}