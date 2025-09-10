package com.balsam.oasis.common.registry.example;

import java.math.BigDecimal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.balsam.oasis.common.registry.builder.QueryDefinition;
import com.balsam.oasis.common.registry.builder.SelectDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.api.QueryRegistry;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;

/**
 * Example configuration for Select definitions using Oracle HR schema.
 * Now using consolidated QueryDefinition architecture with selectType=true.
 */
@Configuration
public class OracleHRSelectConfig {

        private final QueryRegistry queryRegistry;

        public OracleHRSelectConfig(QueryRegistry queryRegistry) {
                this.queryRegistry = queryRegistry;
        }

        /**
         * Employee select with custom search criteria
         */
        @Bean
        public QueryDefinition employeeSelect() {
                QueryDefinition select = SelectDefinitionBuilder.builder("employeesLov")
                                .sql("""
                                                SELECT
                                                    *
                                                FROM employees e
                                                LEFT JOIN departments d ON e.department_id = d.department_id
                                                WHERE 1=1
                                                --departmentFilter
                                                """)
                                .description("Employee select for dropdowns with search and department filtering")
                                .valueAttribute("EMPLOYEE_ID", Integer.class)
                                .labelAttribute("FIRST_NAME")
                                .criteria(CriteriaDef.name("departmentFilter")
                                                .sql("AND d.department_id = :departmentId")
                                                .condition(ctx -> ctx.hasParam("departmentId"))
                                                .build())
                                .parameter(ParamDef.name("departmentId")
                                                .type(Integer.class)
                                                .build())
                                .postProcessor((queryResult, context) -> {
                                        System.out.println("@@@@@@@@@postProcessor@@@@@@@@");
                                        return queryResult;
                                })
                                .paginationEnabled(false)
                                .dynamic()
                                .build();

                queryRegistry.register(select);
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
                                .valueAttribute("department_id", Integer.class)
                                .labelAttribute("department_name")

                                // Additional attributes
                                .attribute(AttributeDef.name("city")
                                                .type(String.class)
                                                .aliasName("city")
                                                .build())
                                .attribute(AttributeDef.name("state_province")
                                                .type(String.class)
                                                .aliasName("state_province")
                                                .build())
                                .attribute(AttributeDef.name("country_name")
                                                .type(String.class)
                                                .aliasName("country_name")
                                                .build())

                                // No searchCriteria defined - will auto-wrap with:
                                // SELECT * FROM (...) WHERE LOWER(department_name) LIKE LOWER(:search)

                                // Optional location filter
                                .criteria(CriteriaDef.name("locationFilter")
                                                .sql("AND l.location_id = :locationId")
                                                .condition(ctx -> ctx.hasParam("locationId"))
                                                .build())

                                .parameter(ParamDef.name("locationId")
                                                .type(Integer.class)
                                                .build())

                                .build();

                queryRegistry.register(select);
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

                                .valueAttribute("job_id", String.class)
                                .labelAttribute("job_title")

                                .attribute(AttributeDef.name("min_salary")
                                                .type(BigDecimal.class)
                                                .aliasName("min_salary")
                                                .build())
                                .attribute(AttributeDef.name("max_salary")
                                                .type(BigDecimal.class)
                                                .aliasName("max_salary")
                                                .build())

                                // No search criteria - will auto-wrap
                                // No additional filters - simple select

                                .build();

                queryRegistry.register(select);
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

                                .valueAttribute("employee_id", Integer.class)
                                .labelAttribute("full_name")

                                .attribute(AttributeDef.name("email")
                                                .type(String.class)
                                                .aliasName("email")
                                                .build())
                                .attribute(AttributeDef.name("department_name")
                                                .type(String.class)
                                                .aliasName("department_name")
                                                .build())

                                .criteria(CriteriaDef.name("searchFilter")
                                                .sql("AND LOWER(m.first_name || ' ' || m.last_name) LIKE LOWER(:search)")
                                                .build())

                                .parameter(ParamDef.name("search")
                                                .type(String.class)
                                                .build())

                                .defaultPageSize(50)
                                .build();

                queryRegistry.register(select);
                return select;
        }
}