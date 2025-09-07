package com.balsam.oasis.common.registry.example;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.balsam.oasis.common.registry.query.QueryRegistrar;
import com.balsam.oasis.common.registry.select.SelectDefinitionBuilder;

/**
 * Example configuration for Select definitions using Oracle HR schema.
 * Now using consolidated QueryDefinition architecture with selectType=true.
 */
@Configuration
public class OracleHRSelectConfig {

        private final QueryRegistrar queryRegistrar;

        public OracleHRSelectConfig(QueryRegistrar queryRegistrar) {
                this.queryRegistrar = queryRegistrar;
        }

        /**
         * Employee select with custom search criteria
         */
        @Bean
        public QueryDefinition employeeSelect() {
                QueryDefinition select = SelectDefinitionBuilder.builder("employeesLov")
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
                                                --departmentFilter
                                                --salaryFilter
                                                """)
                                .description("Employee select for dropdowns with search and department filtering")

                                // Value and label definitions (only aliasName and type required)
                                .value("employee_id", Integer.class)
                                .label("full_name")

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
                                .addition(AttributeDef.name("aaaa").type(Integer.class).transient_(true)
                                                .calculated((row, context) -> {
                                                        return 0;
                                                }).build())

                                // Custom search criteria

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
                                .param(ParamDef.param("departmentId")
                                                .type(Integer.class)
                                                .description("Filter by department ID")
                                                .build())
                                .param(ParamDef.param("minSalary")
                                                .type(BigDecimal.class)
                                                .description("Minimum salary filter")
                                                .build())

                                .preProcessor((context) -> {
                                        System.out.println("@@@@@@@@@preProcessor@@@@@@@@");
                                        context.setParam("departmentId", 110);
                                })
                                .rowProcessor((row, context) -> {
                                        System.out.println("@@@@@@@@@rowProcessor@@@@@@@@");
                                        System.out.println(row);
                                        row.set("aaaa", 1);
                                        row.set("salary", BigDecimal.TEN);
                                        return row;
                                })
                                .postProcessor((queryResult, context) -> {
                                        System.out.println("@@@@@@@@@postProcessor@@@@@@@@");
                                        return queryResult.toBuilder().rows(List.of()).data(null)
                                                        .summary(Map.of("TEST", "Summary")).build();
                                })
                                .defaultPageSize(20)
                                .maxPageSize(500)
                                .build();

                queryRegistrar.register(select);
                return select;
        }

        /**
         * Department select without custom search (will auto-wrap with label LIKE)
         */
        @Bean
        public QueryDefinition departmentSelect() {
                QueryDefinition select = SelectDefinitionBuilder.builder("departments")
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
                                .value("department_id", Integer.class)
                                .label("department_name")

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

                queryRegistrar.register(select);
                return select;
        }

        /**
         * Job select - simple example
         */
        @Bean
        public QueryDefinition jobSelect() {
                QueryDefinition select = SelectDefinitionBuilder.builder("jobs")
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

                                .value("job_id", String.class)
                                .label("job_title")

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

                queryRegistrar.register(select);
                return select;
        }

        /**
         * Manager select - demonstrates reusing employee data
         */
        @Bean
        public QueryDefinition managerSelect() {
                QueryDefinition select = SelectDefinitionBuilder.builder("managers")
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

                                .value("employee_id", Integer.class)
                                .label("full_name")

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

                queryRegistrar.register(select);
                return select;
        }
}