package com.balsam.oasis.common.registry.example;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.balsam.oasis.common.registry.api.QueryRegistrar;
import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.definition.QueryDefinition;

import jakarta.annotation.PostConstruct;

/**
 * Oracle HR Schema query configurations
 * Demonstrates Oracle 11g compatibility with ROWNUM pagination
 */
@Configuration
public class OracleHRQueryConfig {

        @Autowired
        private QueryRegistrar queryRegistrar;

        @PostConstruct
        public void registerQueries() {
                // Register all queries defined in this configuration
                queryRegistrar.register(employeesQuery());
                queryRegistrar.register(departmentStatsQuery());

                queryRegistrar.register(QueryDefinitionBuilder.builder("testUnion").sql(
                                """
                                                select employee_id,email
                                                from employees
                                                where department_id <> 100 and job_id = :jobId
                                                union
                                                select employee_id,email
                                                from employees
                                                where department_id <> 110
                                                                                """)
                                .attribute(AttributeDef.name("EMPLOYEE_ID").type(Integer.class)
                                                .build())
                                .param(ParamDef.param("jobId").type(String.class).required(true).build())
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
                                .attribute(AttributeDef.name("employeeId")
                                                .type(Integer.class)
                                                .aliasName("employee_id")
                                                .primaryKey(true)
                                                .sortable(true)
                                                .filterable(true)
                                                .label("Employee ID")
                                                .labelKey("employee.id.label")
                                                .width("100px")
                                                .build())
                                .attribute(AttributeDef.name("firstName")
                                                .type(String.class)
                                                .aliasName("first_name")
                                                .filterable(true)
                                                .sortable(true)
                                                .label("First Name")
                                                .labelKey("employee.firstName.label")
                                                .width("150px")
                                                .flex("1")
                                                .build())
                                .attribute(AttributeDef.name("lastName")
                                                .type(String.class)
                                                .aliasName("last_name")
                                                .filterable(true)
                                                .sortable(true)
                                                .label("Last Name")
                                                .labelKey("employee.lastName.label")
                                                .width("150px")
                                                .flex("1")
                                                .build())
                                .attribute(AttributeDef.name("email")
                                                .type(String.class)
                                                .aliasName("email")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("phoneNumber")
                                                .type(String.class)
                                                .aliasName("phone_number")
                                                .filterable(true)
                                                .build())
                                .attribute(AttributeDef.name("hireDate")
                                                .type(LocalDate.class)
                                                .aliasName("hire_date")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                // Job information
                                .attribute(AttributeDef.name("jobId")
                                                .type(String.class)
                                                .aliasName("job_id")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("jobTitle")
                                                .type(String.class)
                                                .aliasName("job_title")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                // Location information
                                .attribute(AttributeDef.name("city")
                                                .type(String.class)
                                                .aliasName("city")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                // Transient attributes (calculated fields)
                                .attribute(AttributeDef.name("totalCompensation")
                                                .type(BigDecimal.class)
                                                .calculated((row, context) -> {
                                                        BigDecimal salary = row.getBigDecimal("salary");
                                                        BigDecimal commission = row.getBigDecimal("commissionPct");
                                                        if (salary == null)
                                                                return BigDecimal.ZERO;
                                                        if (commission == null)
                                                                return salary;
                                                        return salary.add(salary.multiply(commission));
                                                })
                                                .sortProperty("salary") // Sort by salary when sorting by
                                                                        // totalCompensation
                                                .build())
                                .attribute(AttributeDef.name("testVirtual").type(String.class)
                                                .calculated((row, context) -> {
                                                        return "Hello World";
                                                }).build())

                                // Parameters
                                .param(ParamDef.param("deptId")
                                                .type(Integer.class)
                                                .description("Filter by department ID")
                                                .build())

                                // Parameters for IN clause criteria
                                .param(ParamDef.param("departmentIds")
                                                .type(List.class)
                                                .description("List of department IDs for IN clause")
                                                .build())
                                .param(ParamDef.param("employeeIds")
                                                .type(List.class)
                                                .description("List of employee IDs for IN clause")
                                                .build())
                                .param(ParamDef.param("jobIds")
                                                .type(List.class)
                                                .description("List of job IDs for IN clause")
                                                .build())
                                .param(ParamDef.param("minSalary")
                                                .type(BigDecimal.class)
                                                .defaultValue(new BigDecimal("10000"))
                                                .description("Minimum salary filter")
                                                .build())
                                .param(ParamDef.param("hiredAfter")
                                                .type(LocalDate.class)
                                                .processor((value) -> {
                                                        // Use TypeConverter for type safety
                                                        if (value == null)
                                                                return null;
                                                        return com.balsam.oasis.common.registry.util.TypeConverter
                                                                        .convert(value, LocalDate.class);
                                                })
                                                .description("Filter employees hired after this date")
                                                .build())
                                .param(ParamDef.param("hiredAfterDays")
                                                .type(Long.class)
                                                .processor((value, ctx) -> {
                                                        System.out.println("proccess days " + value);
                                                        if (value != null) {
                                                                // Convert Object to Long first
                                                                Long days = com.balsam.oasis.common.registry.util.TypeConverter
                                                                                .convert(value, Long.class);
                                                                ctx.addParam("hiredAfter",
                                                                                LocalDate.now().minusDays(days));
                                                                return days;
                                                        }
                                                        return null;
                                                })
                                                .description("Filter employees hired after this date")
                                                .build())

                                // Criteria
                                .criteria(CriteriaDef.criteria()
                                                .name("departmentFilter")
                                                .sql("AND e.department_id = :deptId")
                                                .condition(ctx -> ctx.hasParam("deptId"))
                                                .build())
                                .criteria(CriteriaDef.criteria()
                                                .name("salaryFilter")
                                                .sql("AND e.salary >= :minSalary")
                                                .condition(ctx -> ctx.hasParam("minSalary"))
                                                .build())
                                .criteria(CriteriaDef.criteria()
                                                .name("hiredAfterFilter")
                                                .sql("AND e.hire_date > :hiredAfter")
                                                .condition(ctx -> ctx.hasParam("hiredAfter"))
                                                .build())

                                // IN clause criteria examples
                                .criteria(CriteriaDef.criteria()
                                                .name("departmentIdsFilter")
                                                .sql("AND e.department_id IN (:departmentIds)")
                                                .condition(ctx -> ctx.hasParam("departmentIds"))
                                                .description("Filter by multiple department IDs")
                                                .build())
                                .criteria(CriteriaDef.criteria()
                                                .name("employeeIdsFilter")
                                                .sql("AND e.employee_id IN (:employeeIds)")
                                                .condition(ctx -> ctx.hasParam("employeeIds"))
                                                .description("Filter by specific employee IDs")
                                                .build())
                                .criteria(CriteriaDef.criteria()
                                                .name("jobIdsFilter")
                                                .sql("AND e.job_id IN (:jobIds)")
                                                .condition(ctx -> ctx.hasParam("jobIds"))
                                                .description("Filter by multiple job IDs")
                                                .build())
                                .preProcessor((context) -> {
                                        System.out.println("@@@@@@@@@preProcessor@@@@@@@@");
                                })
                                .rowProcessor((row, context) -> {
                                        System.out.println("@@@@@@@@@rowProcessor@@@@@@@@");
                                        System.out.println(row.getString("testVirtual"));
                                        System.out.println(row.get("testVirtual", String.class));
                                        System.out.println(row.getRaw("COUNTRY_ID", String.class));

                                        row.set("aaaa", 1);
                                        row.set("bbbb", 1);
                                        row.set("salary", BigDecimal.TEN);
                                        return row;
                                })
                                .postProcessor((queryResult, context) -> {
                                        System.out.println("@@@@@@@@@postProcessor@@@@@@@@");
                                        return queryResult.toBuilder()
                                                        .summary(Map.of("TEST", "Summary")).build();
                                })
                                .includeDynamicAttributes(true)
                                .dynamicAttributeNamingStrategy(NamingStrategy.PASCAL)
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

                                .attribute(AttributeDef.name("departmentId")
                                                .type(Integer.class)
                                                .aliasName("department_id")
                                                .primaryKey(true)
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("departmentName")
                                                .type(String.class)
                                                .aliasName("department_name")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("employeeCount")
                                                .type(Integer.class)
                                                .aliasName("employee_count")
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("avgSalary")
                                                .type(BigDecimal.class)
                                                .aliasName("avg_salary")
                                                .sortable(true)
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("minSalary")
                                                .type(BigDecimal.class)
                                                .aliasName("min_salary")
                                                .sortable(true)
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("maxSalary")
                                                .type(BigDecimal.class)
                                                .aliasName("max_salary")
                                                .sortable(true)
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("totalSalary")
                                                .type(BigDecimal.class)
                                                .aliasName("total_salary")
                                                .sortable(true)
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("city")
                                                .type(String.class)
                                                .aliasName("city")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("stateProvince")
                                                .type(String.class)
                                                .aliasName("state_province")
                                                .filterable(true)
                                                .build())
                                .attribute(AttributeDef.name("countryName")
                                                .type(String.class)
                                                .aliasName("country_name")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())

                                .param(ParamDef.param("country")
                                                .type(String.class)
                                                .description("Filter by country name")
                                                .build())

                                .criteria(CriteriaDef.criteria()
                                                .name("countryFilter")
                                                .sql("AND c.country_name = :country")
                                                .condition(ctx -> ctx.hasParam("country"))
                                                .build())

                                .defaultPageSize(25)
                                .maxPageSize(100)
                                .build();
        }

}