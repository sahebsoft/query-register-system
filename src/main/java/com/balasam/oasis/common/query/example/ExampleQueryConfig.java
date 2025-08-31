package com.balasam.oasis.common.query.example;

import com.balasam.oasis.common.query.builder.QueryDefinitionBuilder;
import com.balasam.oasis.common.query.config.EnableQueryRegistration;
import com.balasam.oasis.common.query.core.definition.FilterOp;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.processor.impl.CommonProcessors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Example query configurations
 */
@Configuration
@EnableQueryRegistration
public class ExampleQueryConfig {

    @Bean
    public QueryDefinition userDashboardQuery() {
        return QueryDefinitionBuilder.builder("userDashboard")
                .sql("""
                        SELECT
                            u.id as id,
                            u.username as username,
                            u.email as email,
                            u.full_name as full_name,
                            u.status as status,
                            u.created_date as created_date,
                            u.last_login as last_login,
                            COUNT(o.id) as total_orders,
                            COALESCE(SUM(o.amount), 0) as lifetime_value,
                            COALESCE(AVG(o.amount), 0) as average_order_value
                        FROM users u
                        LEFT JOIN orders o ON o.user_id = u.id
                        WHERE 1=1
                        --statusFilter
                        --dateFilter
                        GROUP BY u.id, u.username, u.email, u.full_name, u.status, u.created_date, u.last_login
                        """)
                .description("User dashboard with order statistics")

                // Basic user attributes
                .attribute("id")
                .dbColumn("id")
                .type(Long.class)
                .primaryKey(true)
                .sortable(true)
                .build()

                .attribute("username")
                .dbColumn("username")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.LIKE, FilterOp.IN)
                .build()

                .attribute("email")
                .dbColumn("email")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .processor(CommonProcessors.LOWERCASE_PROCESSOR::process)
                .secure(ctx -> true) // In real app, check permissions
                .build()

                .attribute("fullName")
                .dbColumn("full_name")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.LIKE, FilterOp.STARTS_WITH)
                .build()

                .attribute("status")
                .dbColumn("status")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.IN)
                .allowedValues("ACTIVE", "INACTIVE", "SUSPENDED", "PENDING")
                .build()

                .attribute("createdDate")
                .dbColumn("created_date")
                .type(LocalDate.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.BETWEEN, FilterOp.GREATER_THAN, FilterOp.LESS_THAN)
                .build()

                .attribute("lastLogin")
                .dbColumn("last_login")
                .type(LocalDate.class)
                .filterable(true)
                .sortable(true)
                .build()

                // Order statistics
                .attribute("totalOrders")
                .dbColumn("total_orders")
                .type(Integer.class)
                .sortable(true)
                .calculated(true)
                .build()

                .attribute("lifetimeValue")
                .dbColumn("lifetime_value")
                .type(BigDecimal.class)
                .filterable(false) // Can't filter on calculated aggregate columns in WHERE clause
                .sortable(true)
                .calculated(true)
                // .formatter(CommonProcessors.CURRENCY_FORMATTER::format) // Disabled for now
                .build()

                .attribute("averageOrderValue")
                .dbColumn("average_order_value")
                .type(BigDecimal.class)
                .sortable(true)
                .calculated(true)
                // .formatter(CommonProcessors.CURRENCY_FORMATTER::format) // Disabled for now
                .build()

                // Virtual attributes
                .virtualAttribute("membershipTier")
                .type(String.class)
                .processor(obj -> {
                    Object[] args = (Object[]) obj;
                    com.balasam.oasis.common.query.core.result.Row row = (com.balasam.oasis.common.query.core.result.Row) args[1];
                    BigDecimal ltv = row.getBigDecimal("lifetimeValue");
                    if (ltv == null)
                        return "NONE";
                    double amount = ltv.doubleValue();
                    if (amount >= 10000)
                        return "PLATINUM";
                    if (amount >= 5000)
                        return "GOLD";
                    if (amount >= 1000)
                        return "SILVER";
                    if (amount >= 100)
                        return "BRONZE";
                    return "NONE";
                })
                .filterable(true)
                .allowedValues("NONE", "BRONZE", "SILVER", "GOLD", "PLATINUM")
                .build()

                // Parameters
                .param("minOrderDate")
                .type(LocalDate.class)
                .defaultValue(LocalDate.now().minusYears(1))
                .description("Minimum order date for statistics")
                .build()

                .param("includeInactive")
                .type(Boolean.class)
                .defaultValue(false)
                .description("Include inactive users")
                .build()

                // Criteria
                .criteria("statusFilter")
                .sql("AND u.status = :status")
                .condition(ctx -> ((com.balasam.oasis.common.query.core.execution.QueryContext) ctx).hasParam("status"))
                .build()

                .criteria("dateFilter")
                .sql("AND u.created_date >= :minDate")
                .condition(
                        ctx -> ((com.balasam.oasis.common.query.core.execution.QueryContext) ctx).hasParam("minDate"))
                .build()

                // Configuration
                .defaultPageSize(50)
                .maxPageSize(500)
                .cache(true)
                .build();
    }

    @Bean
    public QueryDefinition simpleUserQuery() {
        return QueryDefinitionBuilder.builder("users")
                .sql("SELECT * FROM users WHERE 1=1 --departmentFilter --findByKey")
                .description("Simple user query with findByKey support")

                .attribute("id")
                .dbColumn("id")
                .type(Long.class)
                .primaryKey(true)
                .sortable(true)
                .build()

                .attribute("username")
                .dbColumn("username")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.LIKE, FilterOp.IN)
                .build()

                .attribute("email")
                .dbColumn("email")
                .type(String.class)
                .filterable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.LIKE)
                .build()

                .attribute("status")
                .dbColumn("status")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.NOT_EQUALS, FilterOp.IN)
                .allowedValues("ACTIVE", "INACTIVE", "SUSPENDED", "PENDING")
                .build()

                .attribute("createdDate")
                .dbColumn("created_date")
                .type(LocalDate.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.BETWEEN, FilterOp.GREATER_THAN, FilterOp.LESS_THAN)
                .build()

                // Add criteria for department filter
                .criteria("departmentFilter")
                .sql("AND department_id = :deptId")
                .condition(ctx -> ((com.balasam.oasis.common.query.core.execution.QueryContext) ctx).hasParam("deptId"))
                .build()

                // Add findByKey criteria for single object retrieval
                .findByKey("findByKey")
                .sql("AND id = :id AND username = :username")
                .description("Find single user by id and username")
                .build()

                .build();
    }

}