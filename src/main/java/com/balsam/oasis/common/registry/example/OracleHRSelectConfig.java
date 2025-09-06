package com.balsam.oasis.common.registry.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.select.SelectDefinition;
import com.balsam.oasis.common.registry.select.SelectDefinitionBuilder;
import com.balsam.oasis.common.registry.select.SelectRegistry;

/**
 * Example configuration for Select definitions using Oracle HR schema.
 * Demonstrates complete separation from Query API.
 */
@Configuration
public class OracleHRSelectConfig {

    private final SelectRegistry selectRegistry;

    public OracleHRSelectConfig(SelectRegistry selectRegistry) {
        this.selectRegistry = selectRegistry;
    }

    /**
     * Employee select with custom search criteria
     */
    @Bean
    public SelectDefinition employeeSelect() {
        SelectDefinition select = SelectDefinitionBuilder.builder("employees")
                .sql("""
                        SELECT
                            e.employee_id,
                            e.first_name || ' ' || e.last_name as full_name,
                            e.email,
                            e.phone_number,
                            d.department_name,
                            e.job_id,
                            e.salary
                        FROM employees e
                        LEFT JOIN departments d ON e.department_id = d.department_id
                        WHERE 1=1
                        --searchFilter
                        --departmentFilter
                        --salaryFilter
                        """)
                .description("Employee select for dropdowns with search and department filtering")

                // Value and label attributes
                .value(AttributeDef.name("employee_id")
                        .type(Integer.class)
                        .aliasName("employee_id")
                        .build())
                .label(AttributeDef.name("full_name")
                        .type(String.class)
                        .aliasName("full_name")
                        .build())

                // Additional attributes
                .addition(AttributeDef.name("email")
                        .type(String.class)
                        .aliasName("email")
                        .build())
                .addition(AttributeDef.name("phone_number")
                        .type(String.class)
                        .aliasName("phone_number")
                        .build())
                .addition(AttributeDef.name("department_name")
                        .type(String.class)
                        .aliasName("department_name")
                        .build())
                .addition(AttributeDef.name("job_id")
                        .type(String.class)
                        .aliasName("job_id")
                        .build())
                .addition(AttributeDef.name("salary")
                        .type(BigDecimal.class)
                        .aliasName("salary")
                        .build())

                // Custom search criteria
                .searchCriteria(CriteriaDef.criteria()
                        .name("searchFilter")
                        .sql("AND (LOWER(e.first_name || ' ' || e.last_name) LIKE LOWER(:search) " +
                                "OR LOWER(e.email) LIKE LOWER(:search) " +
                                "OR LOWER(e.phone_number) LIKE LOWER(:search))")
                        .build())

                // Additional filters
                .criteria(CriteriaDef.criteria()
                        .name("departmentFilter")
                        .sql("AND d.department_id = :departmentId")
                        .condition(ctx -> ctx.hasParam("departmentId"))
                        .build())
                .criteria(CriteriaDef.criteria()
                        .name("salaryFilter")
                        .sql("AND e.salary >= :minSalary")
                        .condition(ctx -> ctx.hasParam("minSalary"))
                        .build())

                // Parameters
                .param(ParamDef.param("search")
                        .type(String.class)
                        .description("Search term for filtering")
                        .build())
                .param(ParamDef.param("departmentId")
                        .type(Integer.class)
                        .description("Filter by department ID")
                        .build())
                .param(ParamDef.param("minSalary")
                        .type(BigDecimal.class)
                        .description("Minimum salary filter")
                        .build())

                .defaultPageSize(100)
                .maxPageSize(500)
                .build();

        selectRegistry.register(select);
        return select;
    }

    /**
     * Department select without custom search (will auto-wrap with label LIKE)
     */
    @Bean
    public SelectDefinition departmentSelect() {
        SelectDefinition select = SelectDefinitionBuilder.builder("departments")
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

                // Value and label
                .value(AttributeDef.name("department_id")
                        .type(Integer.class)
                        .aliasName("department_id")
                        .build())
                .label(AttributeDef.name("department_name")
                        .type(String.class)
                        .aliasName("department_name")
                        .build())

                // Additional attributes
                .addition(AttributeDef.name("city")
                        .type(String.class)
                        .aliasName("city")
                        .build())
                .addition(AttributeDef.name("state_province")
                        .type(String.class)
                        .aliasName("state_province")
                        .build())
                .addition(AttributeDef.name("country_name")
                        .type(String.class)
                        .aliasName("country_name")
                        .build())

                // No searchCriteria defined - will auto-wrap with:
                // SELECT * FROM (...) WHERE LOWER(department_name) LIKE LOWER(:search)

                // Optional location filter
                .criteria(CriteriaDef.criteria()
                        .name("locationFilter")
                        .sql("AND l.location_id = :locationId")
                        .condition(ctx -> ctx.hasParam("locationId"))
                        .build())

                .param(ParamDef.param("locationId")
                        .type(Integer.class)
                        .description("Filter by location")
                        .build())

                .build();

        selectRegistry.register(select);
        return select;
    }

    /**
     * Job select - simple example
     */
    @Bean
    public SelectDefinition jobSelect() {
        SelectDefinition select = SelectDefinitionBuilder.builder("jobs")
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

                .value(AttributeDef.name("job_id")
                        .type(String.class)
                        .aliasName("job_id")
                        .build())
                .label(AttributeDef.name("job_title")
                        .type(String.class)
                        .aliasName("job_title")
                        .build())

                .addition(AttributeDef.name("min_salary")
                        .type(BigDecimal.class)
                        .aliasName("min_salary")
                        .build())
                .addition(AttributeDef.name("max_salary")
                        .type(BigDecimal.class)
                        .aliasName("max_salary")
                        .build())

                // No search criteria - will auto-wrap
                // No additional filters - simple select

                .build();

        selectRegistry.register(select);
        return select;
    }

    /**
     * Manager select - demonstrates reusing employee data
     */
    @Bean
    public SelectDefinition managerSelect() {
        SelectDefinition select = SelectDefinitionBuilder.builder("managers")
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

                .value(AttributeDef.name("employee_id")
                        .type(Integer.class)
                        .aliasName("employee_id")
                        .build())
                .label(AttributeDef.name("full_name")
                        .type(String.class)
                        .aliasName("full_name")
                        .build())

                .addition(AttributeDef.name("email")
                        .type(String.class)
                        .aliasName("email")
                        .build())
                .addition(AttributeDef.name("department_name")
                        .type(String.class)
                        .aliasName("department_name")
                        .build())

                .searchCriteria(CriteriaDef.criteria()
                        .name("searchFilter")
                        .sql("AND LOWER(m.first_name || ' ' || m.last_name) LIKE LOWER(:search)")
                        .build())

                .param(ParamDef.param("search")
                        .type(String.class)
                        .build())

                .defaultPageSize(50)
                .build();

        selectRegistry.register(select);
        return select;
    }
}