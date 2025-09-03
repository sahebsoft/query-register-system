package com.balasam.oasis.common.query.example;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.balasam.oasis.common.query.builder.QueryDefinitionBuilder;
import com.balasam.oasis.common.query.config.EnableQueryRegistration;
import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.CriteriaDef;
import com.balasam.oasis.common.query.core.definition.ParamDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;

/**
 * Oracle HR Schema query configurations
 * Demonstrates Oracle 11g compatibility with ROWNUM pagination
 */
@Configuration
@EnableQueryRegistration
public class OracleHRQueryConfig {

    @Bean
    public QueryDefinition employeesQuery() {
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
                        """)
                .description("Oracle HR Schema - Employee information with department and manager details")
                
                // Employee attributes
                .attribute(AttributeDef.name("employeeId")
                        .type(Integer.class)
                        .aliasName("employee_id")
                        .primaryKey(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("firstName")
                        .type(String.class)
                        .aliasName("first_name")
                        .filterable(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("lastName")
                        .type(String.class)
                        .aliasName("last_name")
                        .filterable(true)
                        .sortable(true)
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
                
                // Salary information
                .attribute(AttributeDef.name("salary")
                        .type(BigDecimal.class)
                        .aliasName("salary")
                        .filterable(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("commissionPct")
                        .type(BigDecimal.class)
                        .aliasName("commission_pct")
                        .filterable(true)
                        .sortable(true)
                        .build())
                
                // Manager information
                .attribute(AttributeDef.name("managerId")
                        .type(Integer.class)
                        .aliasName("manager_id")
                        .filterable(true)
                        .build())
                .attribute(AttributeDef.name("managerName")
                        .type(String.class)
                        .aliasName("manager_name")
                        .filterable(true)
                        .sortable(true)
                        .build())
                
                // Department information
                .attribute(AttributeDef.name("departmentId")
                        .type(Integer.class)
                        .aliasName("department_id")
                        .filterable(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("departmentName")
                        .type(String.class)
                        .aliasName("department_name")
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
                .attribute(AttributeDef.name("countryId")
                        .type(String.class)
                        .aliasName("country_id")
                        .filterable(true)
                        .build())
                
                // Virtual attributes
                .attribute(AttributeDef.name("totalCompensation")
                        .type(BigDecimal.class)
                        .virtual(true)
                        .virtual(true)
                        .processor((value, row, context) -> {
                            BigDecimal salary = row.getBigDecimal("salary");
                            BigDecimal commission = row.getBigDecimal("commissionPct");
                            if (salary == null) return BigDecimal.ZERO;
                            if (commission == null) return salary;
                            return salary.add(salary.multiply(commission));
                        })
                        .sortable(true)
                        .build())
                
                // Parameters
                .param(ParamDef.param("deptId")
                        .type(Integer.class)
                        .description("Filter by department ID")
                        .build())
                .param(ParamDef.param("minSalary")
                        .type(BigDecimal.class)
                        .defaultValue(new BigDecimal("0"))
                        .description("Minimum salary filter")
                        .build())
                .param(ParamDef.param("hiredAfter")
                        .type(LocalDate.class)
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
                
                // Configuration
                .defaultPageSize(20)
                .maxPageSize(100)
                .cache(true)
                .build();
    }

    @Bean
    public QueryDefinition departmentStatsQuery() {
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
                        .virtual(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("avgSalary")
                        .type(BigDecimal.class)
                        .aliasName("avg_salary")
                        .virtual(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("minSalary")
                        .type(BigDecimal.class)
                        .aliasName("min_salary")
                        .virtual(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("maxSalary")
                        .type(BigDecimal.class)
                        .aliasName("max_salary")
                        .virtual(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("totalSalary")
                        .type(BigDecimal.class)
                        .aliasName("total_salary")
                        .virtual(true)
                        .sortable(true)
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