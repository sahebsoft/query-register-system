package com.balsam.oasis.common.registry.example;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.engine.query.QueryRegistryImpl;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Oracle HR Schema query configurations
 * Demonstrates Oracle 11g compatibility with ROWNUM pagination
 */
@Configuration
@RequiredArgsConstructor
public class OracleHRQueryConfig {

        private final QueryRegistryImpl queryRegistry;

        @PostConstruct
        public void registerQueries() {

                queryRegistry.register(QueryDefinitionBuilder.builder("dynamic").sql("""
                                SELECT * from employees
                                                    """)
                                .parameter(ParamDef.name("deptId", Integer.class)
                                                .build())
                                .attribute(AttributeDef.name("asdasd").aliasName("XX").build())
                                .attribute(AttributeDef.name("fullName", String.class)
                                                .calculated((row, context) -> String.format("%s %s",
                                                                row.getRaw("FIRST_NAME"), // Raw SQL access since no
                                                                                          // attributes defined
                                                                row.getRaw("LAST_NAME")))
                                                .build())
                                .rowProcessor((row, context) -> {
                                        row.set("DEPT_NAME", "zzz");
                                        return row;
                                })
                                .build());

                // Register all queries defined in this configuration
                queryRegistry.register(employeesQuery());
                queryRegistry.register(departmentStatsQuery());
                queryRegistry.register(QueryDefinitionBuilder.builder("testUnion").sql(
                                """
                                                select employee_id,email
                                                from employees
                                                where department_id <> 100
                                                union
                                                select employee_id,email
                                                from employees
                                                where department_id <> 110
                                                                                """)
                                .attribute(AttributeDef.name("EMPLOYEE_ID", Integer.class)
                                                .build())
                                .parameter(ParamDef.name("jobId", String.class).required(false).build())
                                .build());

                // Register select/LOV queries
                queryRegistry.register(employeesSelectQuery());
                queryRegistry.register(departmentsSelectQuery());
                queryRegistry.register(jobsSelectQuery());
                queryRegistry.register(managersSelectQuery());
        }

        private QueryDefinitionBuilder employeesQuery() {
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
                                                --findByKey
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
                                // Salary information
                                .attribute(AttributeDef.name("salary", BigDecimal.class)
                                                .formatter(value -> String.format("$%.2f", value))
                                                .aliasName("salary")
                                                .build())
                                // Location information
                                .attribute(AttributeDef.name("city", String.class)
                                                .aliasName("CITY").formatter(String::toUpperCase)
                                                .build())
                                // Transient attributes (calculated fields)
                                .attribute(AttributeDef.name("totalCompensation", BigDecimal.class)
                                                .calculated((row, context) -> {
                                                        // Use attribute names to access data
                                                        BigDecimal salary = (BigDecimal) row.get("salary");
                                                        BigDecimal commission = (BigDecimal) row
                                                                        .getRaw("COMMISSION_PCT"); // Commission not in
                                                                                                   // attributes
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
                                .parameter(ParamDef.name("deptId", BigDecimal.class).build())
                                .parameter(ParamDef.name("empId", BigDecimal.class).build())

                                // Parameters for IN clause criteria
                                .parameter(ParamDef.name("departmentIds", String.class)
                                                .build())
                                .parameter(ParamDef.name("employeeIds", List.class).build())
                                .parameter(ParamDef.name("jobIds", List.class).build())
                                .parameter(ParamDef.name("minSalary", BigDecimal.class).build())
                                .parameter(ParamDef.name("hiredAfter", LocalDate.class)
                                                .build())

                                // Criteria
                                .criteria(CriteriaDef.name("departmentFilter")
                                                .sql("AND e.department_id = :deptId")
                                                .condition(ctx -> ctx.hasParam("deptId"))
                                                .build())
                                .criteria(CriteriaDef.name("findByKey").sql("AND e.employee_id = :empId")
                                                .condition(ctx -> ctx.hasParam("empId")).build())
                                .criteria(CriteriaDef.name("salaryFilter").sql("AND e.salary >= :minSalary")
                                                .condition(ctx -> ctx.hasParam("minSalary")).build())
                                .criteria(CriteriaDef.name("hiredAfterFilter")
                                                .sql("AND e.hire_date > :hiredAfter")
                                                .condition(ctx -> ctx.hasParam("hiredAfter")).build())
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
                                        return row;
                                })
                                .postProcessor((queryData, context) -> {
                                        System.out.println("@@@@@@@@@postProcessor@@@@@@@@");
                                        return queryData;
                                })
                                .defaultPageSize(20)
                                .maxPageSize(100)
                                .cache(true)
                                .build();
        }

        private QueryDefinitionBuilder departmentStatsQuery() {
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

                                // Removed N+1 query problem - use employee count instead
                                // If you need employee details, fetch them separately in batch
                                .attribute(AttributeDef.name("departmentSize", String.class)
                                                .calculated((row, context) -> {
                                                        Integer count = (Integer) row.getRaw("EMPLOYEE_COUNT");
                                                        if (count == null || count == 0) {
                                                                return "No employees";
                                                        } else if (count <= 5) {
                                                                return "Small team (" + count + " employees)";
                                                        } else if (count <= 20) {
                                                                return "Medium team (" + count + " employees)";
                                                        } else {
                                                                return "Large team (" + count + " employees)";
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

        // Select/LOV Query Definitions

        private QueryDefinitionBuilder employeesSelectQuery() {
                return QueryDefinitionBuilder.builder("employeesLov")
                                .sql("""
                                                SELECT
                                                    e.employee_id,
                                                    e.first_name || ' ' || e.last_name as full_name,
                                                    e.email,
                                                    d.department_name
                                                FROM employees e
                                                LEFT JOIN departments d ON e.department_id = d.department_id
                                                WHERE 1=1
                                                --departmentFilter
                                                --searchFilter
                                                """)
                                .selectProps("employeeId", "fullName")
                                .attribute(AttributeDef.name("employeeId", Integer.class).aliasName("employee_id")
                                                .build())
                                .attribute(AttributeDef.name("fullName", String.class).aliasName("full_name")
                                                .visible(false).build())
                                .attribute(AttributeDef.name("email", String.class).aliasName("email").build())
                                .attribute(AttributeDef.name("department_name", String.class)
                                                .aliasName("department_name").build())
                                .criteria(CriteriaDef.name("departmentFilter")
                                                .sql("AND d.department_id = :departmentId")
                                                .condition(ctx -> ctx.hasParam("departmentId")).build())
                                .criteria(CriteriaDef.name("searchFilter")
                                                .sql("AND LOWER(e.first_name || ' ' || e.last_name) LIKE LOWER(:search)")
                                                .condition(ctx -> ctx.hasParam("search"))
                                                .build())
                                .parameter(ParamDef.name("departmentId")
                                                .build())
                                .parameter(ParamDef.name("search")
                                                .build())
                                .paginationEnabled(true)
                                .defaultPageSize(100)
                                .build();
        }

        private QueryDefinitionBuilder departmentsSelectQuery() {
                return QueryDefinitionBuilder.builder("departments")
                                .sql("""
                                                SELECT
                                                    d.department_id,
                                                    d.department_name,
                                                    l.city,
                                                    l.state_province,
                                                    c.country_name
                                                FROM departments d
                                                LEFT JOIN locations l ON d.location_id = l.location_id
                                                LEFT JOIN countries c ON l.country_id = c.country_id
                                                WHERE 1=1
                                                --locationFilter
                                                """)
                                .description("Department select with location information")
                                .selectProps("departmentId", "departmentName")
                                .attribute(AttributeDef.name("departmentId", Integer.class)
                                                .aliasName("department_id")
                                                .build())
                                .attribute(AttributeDef.name("departmentName", String.class)
                                                .aliasName("department_name")
                                                .build())
                                .attribute(AttributeDef.name("city", String.class)
                                                .aliasName("city")
                                                .build())
                                .attribute(AttributeDef.name("state_province", String.class)
                                                .aliasName("state_province")
                                                .build())
                                .attribute(AttributeDef.name("country_name", String.class)
                                                .aliasName("country_name")
                                                .build())
                                .criteria(CriteriaDef.name("locationFilter")
                                                .sql("AND l.location_id = :locationId")
                                                .condition(ctx -> ctx.hasParam("locationId"))
                                                .build())
                                .parameter(ParamDef.name("locationId")
                                                .build())
                                .build();
        }

        private QueryDefinitionBuilder jobsSelectQuery() {
                return QueryDefinitionBuilder.builder("jobs")
                                .sql("""
                                                SELECT
                                                    job_id,
                                                    job_title,
                                                    min_salary,
                                                    max_salary
                                                FROM jobs
                                                ORDER BY job_title
                                                """)
                                .description("Job titles for dropdowns")
                                .selectProps("job_id", "job_title")
                                .attribute(AttributeDef.name("job_id", String.class)
                                                .aliasName("job_id")
                                                .build())
                                .attribute(AttributeDef.name("job_title", String.class)
                                                .aliasName("job_title")
                                                .build())
                                .attribute(AttributeDef.name("min_salary", BigDecimal.class)
                                                .aliasName("min_salary")
                                                .build())
                                .attribute(AttributeDef.name("max_salary", BigDecimal.class)
                                                .aliasName("max_salary")
                                                .build())
                                .build();
        }

        private QueryDefinitionBuilder managersSelectQuery() {
                return QueryDefinitionBuilder.builder("managers")
                                .sql("""
                                                SELECT DISTINCT
                                                    m.employee_id,
                                                    m.first_name || ' ' || m.last_name as full_name,
                                                    m.email,
                                                    d.department_name
                                                FROM employees e
                                                INNER JOIN employees m ON e.manager_id = m.employee_id
                                                LEFT JOIN departments d ON m.department_id = d.department_id
                                                WHERE 1=1
                                                --searchFilter
                                                """)
                                .description("Managers only for selection")
                                .selectProps("employee_id", "full_name")
                                .attribute(AttributeDef.name("employee_id", Integer.class)
                                                .aliasName("employee_id")
                                                .build())
                                .attribute(AttributeDef.name("full_name", String.class)
                                                .aliasName("full_name")
                                                .build())
                                .attribute(AttributeDef.name("email", String.class)
                                                .aliasName("email")
                                                .build())
                                .attribute(AttributeDef.name("department_name", String.class)
                                                .aliasName("department_name")
                                                .build())
                                .criteria(CriteriaDef.name("searchFilter")
                                                .sql("AND LOWER(m.first_name || ' ' || m.last_name) LIKE LOWER(:search)")
                                                .condition(ctx -> ctx.hasParam("search"))
                                                .build())
                                .parameter(ParamDef.name("search")
                                                .build())
                                .defaultPageSize(50)
                                .build();
        }

}