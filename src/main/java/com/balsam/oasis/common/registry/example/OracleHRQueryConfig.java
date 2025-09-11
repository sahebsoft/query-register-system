package com.balsam.oasis.common.registry.example;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.api.QueryExecutor;
import com.balsam.oasis.common.registry.domain.api.QueryRegistry;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.engine.sql.util.JavaTypeConverter;

import jakarta.annotation.PostConstruct;

/**
 * Oracle HR Schema query configurations
 * Demonstrates Oracle 11g compatibility with ROWNUM pagination
 */
@Configuration
public class OracleHRQueryConfig {

        @Autowired
        private QueryRegistry queryRegistry;

        @Autowired
        private QueryExecutor queryExecutor;

        @PostConstruct
        public void registerQueries() {

                queryRegistry.register(QueryDefinitionBuilder.builder("dynamic").sql("""
                                SELECT * from employees
                                                    """)
                                .parameter(ParamDef.name("asd", String.class).processor((val, ctx) -> "1")
                                                .defaultValue("123")
                                                .build())
                                .attribute(AttributeDef.name("test", String.class).calculated((row, context) -> {
                                        return "ASd";
                                }).build())
                                .dynamic().build());

                // Register all queries defined in this configuration
                queryRegistry.register(employeesQuery());
                queryRegistry.register(departmentStatsQuery());
                queryRegistry.register(QueryDefinitionBuilder.builder("testUnion").sql(
                                """
                                                select employee_id,email
                                                from employees
                                                where department_id <> 100 and job_id = :jobId
                                                union
                                                select employee_id,email
                                                from employees
                                                where department_id <> 110
                                                                                """)
                                .attribute(AttributeDef.name("EMPLOYEE_ID", Integer.class)
                                                .build())
                                .parameter(ParamDef.name("jobId").required(true).build())
                                .build());

        }

        private QueryDefinition employeesQuery() {
                return QueryDefinitionBuilder.builder("employees")
                                .sql("""
                                                SELECT
                                                    e.employee_id,
                                                    e.first_name,
                                                    e.last_name,
                                                    e.email,
                                                    e.phone_number,
                                                    e.hire_date,
                                                    e.job_id,
                                                    j.job_title,
                                                    e.salary,
                                                    e.commission_pct,
                                                    e.manager_id,
                                                    m.first_name || ' ' || m.last_name as manager_name,
                                                    e.department_id,
                                                    d.department_name,
                                                    l.city,
                                                    l.country_id
                                                FROM employees e
                                                LEFT JOIN jobs j ON e.job_id = j.job_id
                                                LEFT JOIN employees m ON e.manager_id = m.employee_id
                                                LEFT JOIN departments d ON e.department_id = d.department_id
                                                LEFT JOIN locations l ON d.location_id = l.location_id
                                                WHERE 1=1
                                                --departmentFilter
                                                --salaryFilter
                                                --hiredAfterFilter
                                                --departmentIdsFilter
                                                --employeeIdsFilter
                                                --jobIdsFilter
                                                --statusFilter
                                                """)
                                .description("Oracle HR Schema - Employee information with department and manager details")

                                // Employee attributes
                                .attribute(AttributeDef.name("employeeId", Integer.class)
                                                .aliasName("employee_id")
                                                .primaryKey(true)
                                                .label("Employee ID")
                                                .labelKey("employee.id.label")
                                                .width("100px")
                                                .build())
                                .attribute(AttributeDef.name("firstName", String.class)
                                                .aliasName("first_name")
                                                .label("First Name")
                                                .labelKey("employee.firstName.label")
                                                .width("150px")
                                                .flex("1")
                                                .build())
                                .attribute(AttributeDef.name("lastName", String.class)
                                                .aliasName("last_name")
                                                .label("Last Name")
                                                .labelKey("employee.lastName.label")
                                                .width("150px")
                                                .flex("1")
                                                .build())
                                .attribute(AttributeDef.name("email", String.class)
                                                .aliasName("email")
                                                .build())
                                .attribute(AttributeDef.name("phoneNumber", String.class)
                                                .aliasName("phone_number")
                                                .build())
                                .attribute(AttributeDef.name("hireDate", LocalDate.class)
                                                .aliasName("hire_date")
                                                .build())
                                // Job information
                                .attribute(AttributeDef.name("jobId", String.class)
                                                .aliasName("job_id")
                                                .build())
                                .attribute(AttributeDef.name("jobTitle", String.class)
                                                .aliasName("job_title")
                                                .build())
                                // Location information
                                .attribute(AttributeDef.name("city", String.class)
                                                .aliasName("city")
                                                .formatter(value -> value.toLowerCase())
                                                .build())
                                // Transient attributes (calculated fields)
                                .attribute(AttributeDef.name("totalCompensation", BigDecimal.class)
                                                .calculated((row, context) -> {
                                                        System.out.println("totalCompensation");
                                                        BigDecimal salary = row.getBigDecimal("salary");
                                                        BigDecimal commission = row.getBigDecimal("commissionPct");
                                                        if (salary == null)
                                                                return BigDecimal.ZERO;
                                                        if (commission == null)
                                                                return salary;
                                                        return salary.add(salary.multiply(commission));
                                                })
                                                .build())
                                .attribute(AttributeDef.name("testVirtual", String.class)
                                                .calculated((row, context) -> {
                                                        return "Hello World";
                                                }).build())
                                .attribute(AttributeDef.name("internalDebugInfo", String.class)
                                                .selected(false) // Hidden by default unless explicitly requested
                                                .calculated((row, context) -> "Debug: ID=" + row.get("employeeId"))
                                                .build())

                                // Parameters
                                .parameter(ParamDef.name("deptId")
                                                .build())

                                // Parameters for IN clause criteria
                                .parameter(ParamDef.name("departmentIds")
                                                .build())
                                .parameter(ParamDef.name("employeeIds")
                                                .build())
                                .parameter(ParamDef.name("jobIds")
                                                .build())
                                .parameter(ParamDef.name("minSalary")
                                                .build())
                                .parameter(ParamDef.name("hiredAfter")
                                                .processor((value, ctx) -> {
                                                        // Use TypeConverter for type safety
                                                        if (value == null)
                                                                return null;
                                                        return JavaTypeConverter
                                                                        .convert(value, LocalDate.class);
                                                })
                                                .build())
                                .parameter(ParamDef.name("hiredAfterDays")
                                                .processor((value, ctx) -> {
                                                        System.out.println("proccess days " + value);
                                                        if (value != null) {
                                                                // Convert Object to Long first
                                                                Long days = JavaTypeConverter
                                                                                .convert(value, Long.class);
                                                                ctx.addParam("hiredAfter",
                                                                                LocalDate.now().minusDays(days));
                                                                return days;
                                                        }
                                                        return null;
                                                })
                                                .build())

                                // Criteria
                                .criteria(CriteriaDef.name("departmentFilter")
                                                .sql("AND e.department_id = :deptId")
                                                .condition(ctx -> ctx.hasParam("deptId"))
                                                .build())
                                .criteria(CriteriaDef.name("salaryFilter")
                                                .sql("AND e.salary >= :minSalary")
                                                .condition(ctx -> ctx.hasParam("minSalary"))
                                                .build())
                                .criteria(CriteriaDef.name("hiredAfterFilter")
                                                .sql("AND e.hire_date > :hiredAfter")
                                                .condition(ctx -> ctx.hasParam("hiredAfter"))
                                                .build())

                                // IN clause criteria examples
                                .criteria(CriteriaDef.name("departmentIdsFilter")
                                                .sql("AND e.department_id IN (:departmentIds)")
                                                .condition(ctx -> ctx.hasParam("departmentIds"))
                                                .build())
                                .criteria(CriteriaDef.name("employeeIdsFilter")
                                                .sql("AND e.employee_id IN (:employeeIds)")
                                                .condition(ctx -> ctx.hasParam("employeeIds"))
                                                .build())
                                .criteria(CriteriaDef.name("jobIdsFilter")
                                                .sql("AND e.job_id IN (:jobIds)")
                                                .condition(ctx -> ctx.hasParam("jobIds"))
                                                .build())
                                .preProcessor((context) -> {
                                        System.out.println("@@@@@@@@@preProcessor@@@@@@@@");
                                })
                                .rowProcessor((row, context) -> {
                                        System.out.println("@@@@@@@@@rowProcessor@@@@@@@@");
                                        return row;
                                })
                                .postProcessor((queryResult, context) -> {
                                        System.out.println("@@@@@@@@@postProcessor@@@@@@@@");
                                        return queryResult.toBuilder()
                                                        .summary(Map.of("TEST", "Summary")).build();
                                })
                                .dynamic(NamingStrategy.CAMEL)
                                .defaultPageSize(20)
                                .maxPageSize(100)
                                .cache(true)
                                .build();
        }

        private QueryDefinition departmentStatsQuery() {
                return QueryDefinitionBuilder.builder("departmentStats")
                                .sql("""
                                                SELECT
                                                    d.department_id,
                                                    d.department_name,
                                                    COUNT(e.employee_id) as employee_count,
                                                    AVG(e.salary) as avg_salary,
                                                    MIN(e.salary) as min_salary,
                                                    MAX(e.salary) as max_salary,
                                                    SUM(e.salary) as total_salary,
                                                    l.city,
                                                    l.state_province,
                                                    c.country_name
                                                FROM departments d
                                                LEFT JOIN employees e ON d.department_id = e.department_id
                                                LEFT JOIN locations l ON d.location_id = l.location_id
                                                LEFT JOIN countries c ON l.country_id = c.country_id
                                                WHERE 1=1
                                                --countryFilter
                                                GROUP BY
                                                    d.department_id,
                                                    d.department_name,
                                                    l.city,
                                                    l.state_province,
                                                    c.country_name
                                                HAVING COUNT(e.employee_id) > 0
                                                """)
                                .description("Department statistics with employee counts and salary information")

                                .attribute(AttributeDef.name("departmentId", Integer.class)
                                                .aliasName("department_id")
                                                .primaryKey(true)
                                                .build())
                                .attribute(AttributeDef.name("departmentName", String.class)
                                                .aliasName("department_name")
                                                .build())
                                .attribute(AttributeDef.name("employeeCount", Integer.class)
                                                .aliasName("employee_count")
                                                .build())
                                .attribute(AttributeDef.name("avgSalary", BigDecimal.class)
                                                .aliasName("avg_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("minSalary", BigDecimal.class)
                                                .aliasName("min_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("maxSalary", BigDecimal.class)
                                                .aliasName("max_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("totalSalary", BigDecimal.class)
                                                .aliasName("total_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("city", String.class)
                                                .aliasName("city")
                                                .build())
                                .attribute(AttributeDef.name("stateProvince", String.class)
                                                .aliasName("state_province")
                                                .build())
                                .attribute(AttributeDef.name("countryName", String.class)
                                                .aliasName("country_name")
                                                .build())

                                // Proof of concept: Fetch all employees in this department
                                .attribute(AttributeDef.name("departmentEmployees", List.class)
                                                .calculated((row, context) -> {
                                                        System.out.println("Fetching employees for department");
                                                        Integer deptId = row.getRaw("DEPARTMENT_ID", Integer.class);
                                                        if (deptId == null) {
                                                                return List.of();
                                                        }

                                                        try {
                                                                // Debug: Check what department we're querying
                                                                System.out.println(
                                                                                "Fetching employees for department ID: "
                                                                                                + deptId);

                                                                // Execute query to get all employees in this department
                                                                // Use departmentIds parameter for IN clause instead of
                                                                // deptId
                                                                // Using select() to only get needed fields (performance
                                                                // optimization)
                                                                QueryResult result = queryExecutor.execute("employees")
                                                                                .withParam("departmentIds",
                                                                                                List.of(deptId))
                                                                                .select("employeeId", "firstName")
                                                                                .withPagination(0, 100)
                                                                                .execute();

                                                                System.out.println("Found " + result.getRows().size()
                                                                                + " employees for dept " + deptId
                                                                                + " (using select for performance)");

                                                                // Return simplified employee data
                                                                return result.getData();
                                                        } catch (Exception e) {
                                                                System.err.println(
                                                                                "Error fetching department employees: "
                                                                                                + e.getMessage());
                                                                return List.of();
                                                        }
                                                })
                                                .build())

                                .parameter(ParamDef.name("country")
                                                .build())

                                .criteria(CriteriaDef.name("countryFilter")
                                                .sql("AND c.country_name = :country")
                                                .condition(ctx -> ctx.hasParam("country"))
                                                .build())

                                .defaultPageSize(25)
                                .maxPageSize(100)
                                .build();
        }

}