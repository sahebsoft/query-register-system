package com.balasam.oasis.common.query.builder;

import com.balasam.oasis.common.query.core.definition.*;
import com.balasam.oasis.common.query.core.execution.QueryContext;
import com.balasam.oasis.common.query.core.result.QueryResult;
import com.balasam.oasis.common.query.core.result.Row;
import com.balasam.oasis.common.query.processor.PreProcessor;
import com.balasam.oasis.common.query.processor.RowProcessor;
import com.balasam.oasis.common.query.processor.PostProcessor;
import com.balasam.oasis.common.query.processor.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for QueryDefinitionBuilder
 * Tests all features and capabilities of the fluent builder API
 */
@DisplayName("QueryDefinitionBuilder Tests")
class QueryDefinitionBuilderTest {

    // ========================================
    // Basic Builder Tests
    // ========================================

    @Test
    @DisplayName("Test basic query definition creation with minimum required fields")
    void testBasicQueryDefinitionCreation() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("id")
                .dbColumn("user_id")
                .type(Long.class)
                .build()
                .build();

        assertThat(query).isNotNull();
        assertThat(query.getName()).isEqualTo("testQuery");
        assertThat(query.getSql()).isEqualTo("SELECT * FROM users");
        assertThat(query.getAttributes()).hasSize(1);
        assertThat(query.getAttributes().get("id")).isNotNull();
        assertThat(query.getAttributes().get("id").getDbColumn()).isEqualTo("user_id");
        assertThat(query.getAttributes().get("id").getType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("Test builder with description")
    void testBuilderWithDescription() {
        QueryDefinition query = QueryDefinitionBuilder.builder("userQuery")
                .sql("SELECT * FROM users")
                .description("Query to fetch user data with filters")
                .attribute("username")
                .type(String.class)
                .build()
                .build();

        assertThat(query.getDescription()).isEqualTo("Query to fetch user data with filters");
    }

    @Test
    @DisplayName("Test that query name is required")
    void testBuilderValidationRequiresName() {
        assertThatThrownBy(() -> QueryDefinitionBuilder.builder(null)
                .sql("SELECT * FROM users")
                .attribute("id").build()
                .build()).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Query name cannot be null");
    }

    @Test
    @DisplayName("Test that SQL is required")
    void testBuilderValidationRequiresSql() {
        assertThatThrownBy(() -> QueryDefinitionBuilder.builder("testQuery")
                .attribute("id").build()
                .build()).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SQL cannot be null");
    }

    @Test
    @DisplayName("Test that at least one attribute is required")
    void testBuilderValidationRequiresAttributes() {
        assertThatThrownBy(() -> QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .build()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one attribute is required");
    }

    // ========================================
    // Attribute Builder Tests
    // ========================================

    @Test
    @DisplayName("Test attribute with all properties")
    void testAttributeWithAllProperties() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("email")
                .dbColumn("email_address")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .calculated(false)
                .primaryKey(false)
                .defaultValue("default@example.com")
                .description("User email address")
                .build()
                .build();

        AttributeDef attr = query.getAttributes().get("email");
        assertThat(attr).isNotNull();
        assertThat(attr.getDbColumn()).isEqualTo("email_address");
        assertThat(attr.getType()).isEqualTo(String.class);
        assertThat(attr.isFilterable()).isTrue();
        assertThat(attr.isSortable()).isTrue();
        assertThat(attr.isCalculated()).isFalse();
        assertThat(attr.isPrimaryKey()).isFalse();
        assertThat(attr.getDefaultValue()).isEqualTo("default@example.com");
        assertThat(attr.getDescription()).isEqualTo("User email address");
    }

    @Test
    @DisplayName("Test attribute with filter operators")
    void testAttributeWithFilterOperators() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("status")
                .type(String.class)
                .filterable(true)
                .filterOperators(FilterOp.EQUALS, FilterOp.IN, FilterOp.NOT_EQUALS)
                .build()
                .build();

        AttributeDef attr = query.getAttributes().get("status");
        assertThat(attr.getAllowedOperators()).containsExactlyInAnyOrder(
                FilterOp.EQUALS, FilterOp.IN, FilterOp.NOT_EQUALS);
    }

    @Test
    @DisplayName("Test attribute with allowed values")
    void testAttributeWithAllowedValues() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("role")
                .type(String.class)
                .allowedValues("ADMIN", "USER", "GUEST", "MODERATOR")
                .defaultValue("USER")
                .build()
                .build();

        AttributeDef attr = query.getAttributes().get("role");
        assertThat(attr.getAllowedValues()).containsExactly("ADMIN", "USER", "GUEST", "MODERATOR");
        assertThat(attr.getDefaultValue()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Test attribute with security rule")
    void testAttributeWithSecurityRule() {
        Function<Object, Boolean> securityRule = ctx -> {
            // Simulate security check
            return ctx != null && ctx.toString().contains("ADMIN");
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("salary")
                .type(BigDecimal.class)
                .secure(securityRule)
                .build()
                .build();

        AttributeDef attr = query.getAttributes().get("salary");
        assertThat(attr.getSecurityRule()).isNotNull();
        assertThat(attr.getSecurityRule().apply("USER")).isFalse();
        assertThat(attr.getSecurityRule().apply("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("Test attribute with processor")
    void testAttributeWithProcessor() {
        Function<Object, Object> processor = value -> {
            // Processor handles all transformations
            if (value == null)
                return "$0.00";
            return "$" + value.toString().toUpperCase();
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products")
                .attribute("price")
                .type(BigDecimal.class)
                .processor((value, row, context) -> {
                    // Processor handles all transformations
                    if (value == null)
                        return "$0.00";
                    return "$" + value.toString().toUpperCase();
                })
                .build()
                .build();

        AttributeDef attr = query.getAttributes().get("price");
        assertThat(attr.getProcessor()).isNotNull();

        // Test the processor works
        assertThat(attr.getProcessor().process("test", null, null)).isEqualTo("$TEST");
        assertThat(attr.getProcessor().process(100, null, null)).isEqualTo("$100");
        assertThat(attr.getProcessor().process(null, null, null)).isEqualTo("$0.00");
    }

    @Test
    @DisplayName("Test primary key attribute")
    void testPrimaryKeyAttribute() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("id")
                .type(Long.class)
                .primaryKey(true)
                .build()
                .attribute("name")
                .type(String.class)
                .primaryKey(false)
                .build()
                .build();

        assertThat(query.getAttributes().get("id").isPrimaryKey()).isTrue();
        assertThat(query.getAttributes().get("name").isPrimaryKey()).isFalse();
    }

    // ========================================
    // Virtual Attribute Tests
    // ========================================

    @Test
    @DisplayName("Test virtual attribute creation")
    void testVirtualAttributeCreation() {
        Function<Object, Object> tierCalculator = value -> {
            // Simulate tier calculation
            return "GOLD";
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM customers")
                .attribute("customerId")
                .type(Long.class)
                .build()
                .virtualAttribute("membershipTier")
                .type(String.class)
                .processor(tierCalculator)
                .allowedValues("BRONZE", "SILVER", "GOLD", "PLATINUM")
                .build()
                .build();

        AttributeDef virtualAttr = query.getAttributes().get("membershipTier");
        assertThat(virtualAttr).isNotNull();
        assertThat(virtualAttr.isVirtual()).isTrue();
        assertThat(virtualAttr.isCalculated()).isTrue();
        assertThat(virtualAttr.getDbColumn()).isNull();
        assertThat(virtualAttr.getAllowedValues()).containsExactly("BRONZE", "SILVER", "GOLD", "PLATINUM");
    }

    @Test
    @DisplayName("Test virtual attribute with dependencies")
    void testVirtualAttributeWithDependencies() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders")
                .attribute("quantity").type(Integer.class).build()
                .attribute("price").type(BigDecimal.class).build()
                .virtualAttribute("totalAmount")
                .type(BigDecimal.class)
                .processor(value -> BigDecimal.valueOf(100))
                .build()
                .build();

        AttributeDef virtualAttr = query.getAttributes().get("totalAmount");
        assertThat(virtualAttr).isNotNull();
        assertThat(virtualAttr.isVirtual()).isTrue();
        assertThat(virtualAttr.isCalculated()).isTrue();
    }

    @Test
    @DisplayName("Test virtual attributes are automatically marked as calculated")
    void testVirtualAttributeAutoCalculated() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("id").type(Long.class).build()
                .virtualAttribute("displayName")
                .type(String.class)
                .processor(value -> "User Display")
                .build()
                .build();

        AttributeDef virtualAttr = query.getAttributes().get("displayName");
        assertThat(virtualAttr.isCalculated()).isTrue();
        assertThat(virtualAttr.isVirtual()).isTrue();
        assertThat(virtualAttr.isSortable()).isFalse(); // Virtual attributes can't be DB-sorted
    }

    // ========================================
    // Parameter Builder Tests
    // ========================================

    @Test
    @DisplayName("Test parameter with validation")
    void testParameterWithValidation() {
        Function<Object, Boolean> validator = value -> value != null && Integer.parseInt(value.toString()) > 0;

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products WHERE price > :minPrice")
                .attribute("id").type(Long.class).build()
                .param("minPrice")
                .type(BigDecimal.class)
                .validator(validator)
                .description("Minimum price filter")
                .build()
                .build();

        ParamDef param = query.getParams().get("minPrice");
        assertThat(param).isNotNull();
        assertThat(param.getType()).isEqualTo(BigDecimal.class);
        assertThat(param.getDescription()).isEqualTo("Minimum price filter");
    }

    @Test
    @DisplayName("Test parameter with default value")
    void testParameterWithDefaultValue() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders WHERE status = :status")
                .attribute("id").type(Long.class).build()
                .param("status")
                .type(String.class)
                .defaultValue("ACTIVE")
                .required(false)
                .build()
                .build();

        ParamDef param = query.getParams().get("status");
        assertThat(param.getDefaultValue()).isEqualTo("ACTIVE");
        assertThat(param.isRequired()).isFalse();
    }

    @Test
    @DisplayName("Test required parameter")
    void testRequiredParameter() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users WHERE department_id = :deptId")
                .attribute("id").type(Long.class).build()
                .param("deptId")
                .type(Long.class)
                .required(true)
                .build()
                .build();

        ParamDef param = query.getParams().get("deptId");
        assertThat(param.isRequired()).isTrue();
    }

    @Test
    @DisplayName("Test parameter with constraints")
    void testParameterWithConstraints() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products WHERE name LIKE :searchTerm")
                .attribute("id").type(Long.class).build()
                .param("searchTerm")
                .type(String.class)
                .lengthBetween(3, 50)
                .pattern("[a-zA-Z0-9 ]+")
                .build()
                .param("pageSize")
                .type(Integer.class)
                .range(1, 100)
                .defaultValue(20)
                .build()
                .build();

        ParamDef searchParam = query.getParams().get("searchTerm");
        assertThat(searchParam.hasProcessor()).isTrue();
        assertThat(searchParam.getType()).isEqualTo(String.class);

        ParamDef pageSizeParam = query.getParams().get("pageSize");
        assertThat(pageSizeParam.hasProcessor()).isTrue();
        assertThat(pageSizeParam.getDefaultValue()).isEqualTo(20);
    }

    // ========================================
    // Criteria Builder Tests
    // ========================================

    @Test
    @DisplayName("Test criteria with condition")
    void testCriteriaWithCondition() {
        Predicate<Object> condition = ctx -> {
            QueryContext context = (QueryContext) ctx;
            return context.hasParam("status");
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users WHERE 1=1 --statusFilter")
                .attribute("id").type(Long.class).build()
                .criteria("statusFilter")
                .sql("AND status = :status")
                .condition(condition)
                .description("Apply status filter when status param is provided")
                .build()
                .build();

        CriteriaDef criteria = query.getCriteria().get("statusFilter");
        assertThat(criteria).isNotNull();
        assertThat(criteria.getSql()).isEqualTo("AND status = :status");
        assertThat(criteria.getCondition()).isNotNull();
        assertThat(criteria.getBindParams()).contains("status");
        assertThat(criteria.getDescription()).isEqualTo("Apply status filter when status param is provided");
    }

    @Test
    @DisplayName("Test dynamic criteria")
    void testDynamicCriteria() {
        Function<Object, String> generator = ctx -> {
            // Simulate dynamic SQL generation
            return "AND created_date > CURRENT_DATE - INTERVAL 30 DAY";
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders WHERE 1=1 --recentFilter")
                .attribute("id").type(Long.class).build()
                .criteria("recentFilter")
                .dynamic(true)
                .generator(generator)
                .build()
                .build();

        CriteriaDef criteria = query.getCriteria().get("recentFilter");
        assertThat(criteria.isDynamic()).isTrue();
        assertThat(criteria.getGenerator()).isNotNull();
    }

    @Test
    @DisplayName("Test security-related criteria")
    void testSecurityRelatedCriteria() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM documents WHERE 1=1 --securityFilter")
                .attribute("id").type(Long.class).build()
                .criteria("securityFilter")
                .sql("AND department_id IN (SELECT id FROM departments WHERE manager_id = :userId)")
                .securityRelated(true)
                .appliedReason("User can only see documents from their departments")
                .build()
                .build();

        CriteriaDef criteria = query.getCriteria().get("securityFilter");
        assertThat(criteria.isSecurityRelated()).isTrue();
        assertThat(criteria.getAppliedReason()).isEqualTo("User can only see documents from their departments");
    }

    @Test
    @DisplayName("Test criteria priority")
    void testCriteriaPriority() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products WHERE 1=1 --priceFilter --categoryFilter --stockFilter")
                .attribute("id").type(Long.class).build()
                .criteria("priceFilter")
                .sql("AND price > :minPrice")
                .priority(1)
                .build()
                .criteria("categoryFilter")
                .sql("AND category_id = :categoryId")
                .priority(2)
                .build()
                .criteria("stockFilter")
                .sql("AND stock > 0")
                .priority(3)
                .build()
                .build();

        assertThat(query.getCriteria().get("priceFilter").getPriority()).isEqualTo(1);
        assertThat(query.getCriteria().get("categoryFilter").getPriority()).isEqualTo(2);
        assertThat(query.getCriteria().get("stockFilter").getPriority()).isEqualTo(3);
    }

    // ========================================
    // Processor Tests
    // ========================================

    @Test
    @DisplayName("Test pre-processors")
    void testPreProcessors() {
        PreProcessor preProcessor = context -> {
            // Simulate pre-processing
            context.addParam("processedFlag", true);
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("id").type(Long.class).build()
                .preProcessor(preProcessor)
                .build();

        assertThat(query.getPreProcessors()).hasSize(1);
        assertThat(query.hasPreProcessors()).isTrue();
    }

    @Test
    @DisplayName("Test row processors")
    void testRowProcessors() {
        RowProcessor rowProcessor = (row, context) -> {
            // Simulate row processing
            row.set("processed", true);
            return row;
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders")
                .attribute("id").type(Long.class).build()
                .rowProcessor(rowProcessor)
                .build();

        assertThat(query.getRowProcessors()).hasSize(1);
        assertThat(query.hasRowProcessors()).isTrue();
    }

    @Test
    @DisplayName("Test post-processors")
    void testPostProcessors() {
        PostProcessor postProcessor = (result, context) -> {
            // Simulate post-processing
            return result.toBuilder()
                    .success(true)
                    .build();
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products")
                .attribute("id").type(Long.class).build()
                .postProcessor(postProcessor)
                .build();

        assertThat(query.getPostProcessors()).hasSize(1);
        assertThat(query.hasPostProcessors()).isTrue();
    }

    // ========================================
    // Cache Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test cache configuration")
    void testCacheConfiguration() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM static_data")
                .attribute("id").type(Long.class).build()
                .cache(true)
                .build();

        assertThat(query.getCacheConfig()).isNotNull();
        assertThat(query.getCacheConfig().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test cache TTL")
    void testCacheTTL() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM reference_data")
                .attribute("id").type(Long.class).build()
                .cache(true)
                .cacheTTL(Duration.ofMinutes(30))
                .build();

        assertThat(query.getCacheConfig().getTtl()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Test cache key generator")
    void testCacheKeyGenerator() {
        Function<Object, String> keyGenerator = ctx -> "custom-key-" + ctx.hashCode();

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM cached_data")
                .attribute("id").type(Long.class).build()
                .cache(true)
                .cacheKey(keyGenerator)
                .build();

        assertThat(query.getCacheConfig().getKeyGenerator()).isNotNull();
        assertThat(query.getCacheConfig().getKeyGenerator().apply("test")).startsWith("custom-key-");
    }

    // ========================================
    // Query Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test pagination settings")
    void testPaginationSettings() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM large_table")
                .attribute("id").type(Long.class).build()
                .defaultPageSize(25)
                .maxPageSize(500)
                .paginationEnabled(true)
                .build();

        assertThat(query.getDefaultPageSize()).isEqualTo(25);
        assertThat(query.getMaxPageSize()).isEqualTo(500);
        assertThat(query.isPaginationEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test query timeout")
    void testQueryTimeout() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM slow_view")
                .attribute("id").type(Long.class).build()
                .queryTimeout(30)
                .build();

        assertThat(query.getQueryTimeout()).isEqualTo(30);
    }

    @Test
    @DisplayName("Test audit and metrics")
    void testAuditAndMetrics() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM sensitive_data")
                .attribute("id").type(Long.class).build()
                .auditEnabled(true)
                .metricsEnabled(true)
                .build();

        assertThat(query.isAuditEnabled()).isTrue();
        assertThat(query.isMetricsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test validation rules")
    void testValidationRules() {
        ValidationRule rule1 = ValidationRule.builder()
                .name("dateRangeCheck")
                .rule(ctx -> true)
                .errorMessage("Date range is invalid")
                .build();

        Predicate<Object> rule2Logic = ctx -> {
            // Simulate validation
            return ctx != null;
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM events")
                .attribute("id").type(Long.class).build()
                .validationRule(rule1)
                .validationRule("contextCheck", rule2Logic, "Context is required")
                .build();

        assertThat(query.getValidationRules()).hasSize(2);
        assertThat(query.getValidationRules().get(0).getName()).isEqualTo("dateRangeCheck");
        assertThat(query.getValidationRules().get(1).getErrorMessage()).isEqualTo("Context is required");
    }

    // ========================================
    // Complex Query Tests
    // ========================================

    @Test
    @DisplayName("Test complex query with all features")
    void testComplexQueryWithAllFeatures() {
        QueryDefinition query = QueryDefinitionBuilder.builder("complexAnalytics")
                .sql("""
                        WITH monthly_stats AS (
                            SELECT
                                user_id,
                                DATE_TRUNC('month', order_date) as month,
                                SUM(amount) as monthly_total
                            FROM orders
                            WHERE 1=1
                            --dateFilter
                            GROUP BY user_id, month
                        )
                        SELECT
                            u.id,
                            u.name,
                            u.email,
                            u.status,
                            ms.monthly_total,
                            COUNT(o.id) as order_count
                        FROM users u
                        LEFT JOIN monthly_stats ms ON u.id = ms.user_id
                        LEFT JOIN orders o ON u.id = o.user_id
                        WHERE 1=1
                        --statusFilter
                        --regionFilter
                        GROUP BY u.id, u.name, u.email, u.status, ms.monthly_total
                        --orderBy
                        --limit
                        """)
                .description("Complex analytics query with CTEs and aggregations")

                // Regular attributes
                .attribute("id")
                .dbColumn("id")
                .type(Long.class)
                .primaryKey(true)
                .build()

                .attribute("name")
                .dbColumn("name")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .filterOperators(FilterOp.LIKE, FilterOp.STARTS_WITH)
                .build()

                .attribute("email")
                .dbColumn("email")
                .type(String.class)
                .filterable(true)
                .secure(ctx -> true)
                .processor(email -> email.toString().replaceAll("(?<=.{3}).(?=.*@)", "*"))
                .build()

                .attribute("status")
                .dbColumn("status")
                .type(String.class)
                .filterable(true)
                .sortable(true)
                .allowedValues("ACTIVE", "INACTIVE", "SUSPENDED")
                .defaultValue("ACTIVE")
                .build()

                .attribute("monthlyTotal")
                .dbColumn("monthly_total")
                .type(BigDecimal.class)
                .calculated(true)
                .sortable(true)
                .processor(value -> String.format("$%,.2f", value))
                .build()

                .attribute("orderCount")
                .dbColumn("order_count")
                .type(Integer.class)
                .calculated(true)
                .sortable(true)
                .build()

                // Virtual attributes
                .virtualAttribute("customerTier")
                .type(String.class)
                .processor(value -> {
                    // Complex tier calculation
                    return "GOLD";
                })
                .allowedValues("BRONZE", "SILVER", "GOLD", "PLATINUM")
                .filterable(true)
                .build()

                .virtualAttribute("riskScore")
                .type(Integer.class)
                .processor(value -> 75)
                .build()

                // Parameters
                .param("startDate")
                .type(LocalDate.class)
                .required(true)
                .description("Start date for analysis")
                .build()

                .param("endDate")
                .type(LocalDate.class)
                .required(true)
                .validator(value -> true)
                .description("End date for analysis")
                .build()

                .param("region")
                .type(String.class)
                .defaultValue("US")
                .description("Region filter")
                .build()

                .param("minOrderCount")
                .type(Integer.class)
                .defaultValue(0)
                .range(0, 1000)
                .build()

                // Criteria
                .criteria("dateFilter")
                .sql("AND order_date BETWEEN :startDate AND :endDate")
                .condition(
                        ctx -> ((QueryContext) ctx).hasParam("startDate") && ((QueryContext) ctx).hasParam("endDate"))
                .priority(1)
                .build()

                .criteria("statusFilter")
                .sql("AND u.status = :status")
                .condition(ctx -> ((QueryContext) ctx).hasParam("status"))
                .priority(2)
                .build()

                .criteria("regionFilter")
                .sql("AND u.region = :region")
                .condition(ctx -> ((QueryContext) ctx).hasParam("region"))
                .dynamic(false)
                .priority(3)
                .build()

                // Processors
                .preProcessor(context -> {
                    // Validate date range
                    context.addParam("validated", true);
                })

                .rowProcessor((row, context) -> {
                    // Enrich row data
                    row.set("enriched", true);
                    return row;
                })

                .postProcessor((result, context) -> {
                    // Add summary statistics
                    return result;
                })

                // Validation rules
                .validationRule("dateRangeValidation",
                        ctx -> true,
                        "End date must be after start date")

                // Cache configuration
                .cache(true)
                .cacheTTL(Duration.ofMinutes(15))
                .cacheKey(ctx -> "analytics-" + ctx.hashCode())

                // Query configuration
                .defaultPageSize(50)
                .maxPageSize(1000)
                .paginationEnabled(true)
                .auditEnabled(true)
                .metricsEnabled(true)
                .queryTimeout(60)

                .build();

        // Verify the complex query was built successfully
        assertThat(query).isNotNull();
        assertThat(query.getName()).isEqualTo("complexAnalytics");
        assertThat(query.getAttributes()).hasSize(8); // 6 regular + 2 virtual
        assertThat(query.getParams()).hasSize(4);
        assertThat(query.getCriteria()).hasSize(3);
        assertThat(query.getPreProcessors()).hasSize(1);
        assertThat(query.getRowProcessors()).hasSize(1);
        assertThat(query.getPostProcessors()).hasSize(1);
        assertThat(query.getValidationRules()).hasSize(1);
        assertThat(query.getCacheConfig().isEnabled()).isTrue();
        assertThat(query.getQueryTimeout()).isEqualTo(60);
    }

    @Test
    @DisplayName("Test query with multiple attributes")
    void testQueryWithMultipleAttributes() {
        QueryDefinition query = QueryDefinitionBuilder.builder("multiAttributeQuery")
                .sql("SELECT * FROM comprehensive_view")
                .attribute("id").type(Long.class).primaryKey(true).build()
                .attribute("name").type(String.class).filterable(true).sortable(true).build()
                .attribute("email").type(String.class).filterable(true).processor(v -> "***").build()
                .attribute("phone").type(String.class).processor(v -> "***").build()
                .attribute("address").type(String.class).build()
                .attribute("city").type(String.class).filterable(true).build()
                .attribute("state").type(String.class).filterable(true).allowedValues("CA", "NY", "TX").build()
                .attribute("zipCode").type(String.class).build()
                .attribute("country").type(String.class).defaultValue("US").build()
                .attribute("createdDate").type(LocalDate.class).sortable(true).build()
                .attribute("lastModified").type(LocalDate.class).sortable(true).build()
                .attribute("status").type(String.class).filterable(true).sortable(true).build()
                .attribute("balance").type(BigDecimal.class).sortable(true).build()
                .attribute("creditLimit").type(BigDecimal.class).build()
                .attribute("isActive").type(Boolean.class).filterable(true).defaultValue(true).build()
                .build();

        assertThat(query.getAttributes()).hasSize(15);
        assertThat(query.getAttributes().values().stream()
                .filter(AttributeDef::isFilterable)
                .count()).isEqualTo(6);
        assertThat(query.getAttributes().values().stream()
                .filter(AttributeDef::isSortable)
                .count()).isEqualTo(5);
        assertThat(query.getAttributes().values().stream()
                .filter(attr -> attr.hasProcessor())
                .count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Test query with multiple criteria")
    void testQueryWithMultipleCriteria() {
        QueryDefinition query = QueryDefinitionBuilder.builder("multiCriteriaQuery")
                .sql("""
                        SELECT * FROM transactions t
                        WHERE 1=1
                        --amountFilter
                        --dateRangeFilter
                        --statusFilter
                        --categoryFilter
                        --merchantFilter
                        --userFilter
                        --regionFilter
                        --fraudFilter
                        """)
                .attribute("id").type(Long.class).build()

                .criteria("amountFilter")
                .sql("AND t.amount BETWEEN :minAmount AND :maxAmount")
                .priority(1)
                .build()

                .criteria("dateRangeFilter")
                .sql("AND t.transaction_date BETWEEN :startDate AND :endDate")
                .priority(2)
                .build()

                .criteria("statusFilter")
                .sql("AND t.status IN (:statuses)")
                .priority(3)
                .build()

                .criteria("categoryFilter")
                .sql("AND t.category_id = :categoryId")
                .priority(4)
                .build()

                .criteria("merchantFilter")
                .sql("AND t.merchant_id IN (SELECT id FROM merchants WHERE name LIKE :merchantName)")
                .priority(5)
                .build()

                .criteria("userFilter")
                .sql("AND t.user_id = :userId")
                .securityRelated(true)
                .priority(6)
                .build()

                .criteria("regionFilter")
                .sql("AND t.region_code = :regionCode")
                .dynamic(true)
                .generator(ctx -> "AND t.region_code IN (SELECT code FROM regions WHERE active = true)")
                .priority(7)
                .build()

                .criteria("fraudFilter")
                .sql("AND t.fraud_score > :fraudThreshold")
                .condition(ctx -> ((QueryContext) ctx).hasParam("includeFraudCheck"))
                .priority(8)
                .build()

                .build();

        assertThat(query.getCriteria()).hasSize(8);
        assertThat(query.getCriteria().values().stream()
                .filter(CriteriaDef::isSecurityRelated)
                .count()).isEqualTo(1);
        assertThat(query.getCriteria().values().stream()
                .filter(CriteriaDef::isDynamic)
                .count()).isEqualTo(1);

        // Verify priorities are set correctly
        List<CriteriaDef> sortedCriteria = query.getCriteria().values().stream()
                .sorted((c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority()))
                .toList();
        assertThat(sortedCriteria.get(0).getName()).isEqualTo("amountFilter");
        assertThat(sortedCriteria.get(7).getName()).isEqualTo("fraudFilter");
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    @DisplayName("Test criteria placeholder validation")
    void testCriteriaPlaceholderValidation() {
        assertThatThrownBy(() -> QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users WHERE 1=1")
                .attribute("id").type(Long.class).build()
                .criteria("missingPlaceholder")
                .sql("AND status = :status")
                .build()
                .build()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SQL does not contain placeholder for criteria: --missingPlaceholder");
    }

    @Test
    @DisplayName("Test parameter reference validation")
    void testParameterReferenceValidation() {
        // Required parameter not referenced in SQL should throw exception
        assertThatThrownBy(() -> QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("id").type(Long.class).build()
                .param("unreferencedParam")
                .type(String.class)
                .required(true)
                .build()
                .build()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Required parameter not referenced in SQL: unreferencedParam");

        // Optional parameter not referenced should be OK
        assertThatNoException().isThrownBy(() -> QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute("id").type(Long.class).build()
                .param("optionalParam")
                .type(String.class)
                .required(false)
                .build()
                .build());
    }

    @Test
    @DisplayName("Test builder chaining")
    void testBuilderChaining() {
        // Test that all builder methods return the correct type for chaining
        QueryDefinition query = QueryDefinitionBuilder.builder("chainTest")
                .sql("SELECT * FROM test WHERE param2 = :param2 --criteria1")
                .description("Test chaining")
                // Test chaining multiple attributes
                .attribute("field1").type(String.class).build()
                .attribute("field2").type(Integer.class).filterable(true).build()
                .attribute("field3").type(Boolean.class).sortable(true).build()
                // Test chaining virtual attributes
                .virtualAttribute("virtual1").type(String.class).processor(v -> "test").build()
                // Test chaining parameters
                .param("param1").type(String.class).build()
                .param("param2").type(Integer.class).required(true).build()
                // Test chaining criteria
                .criteria("criteria1").sql("AND test = :test").build()
                // Test chaining processors
                .preProcessor(ctx -> {
                })
                .rowProcessor((row, ctx) -> row)
                .postProcessor((result, ctx) -> result)
                // Test chaining validation rules
                .validationRule("rule1", v -> true, "Error")
                // Test chaining cache config
                .cache(true)
                .cacheTTL(Duration.ofMinutes(10))
                .cacheKey(ctx -> "key")
                // Test chaining query config
                .defaultPageSize(20)
                .maxPageSize(200)
                .paginationEnabled(true)
                .auditEnabled(true)
                .metricsEnabled(true)
                .queryTimeout(30)
                // Finally build
                .build();

        assertThat(query).isNotNull();
        assertThat(query.getName()).isEqualTo("chainTest");
        assertThat(query.getDescription()).isEqualTo("Test chaining");
        assertThat(query.getAttributes()).hasSize(4); // 3 regular + 1 virtual
        assertThat(query.getParams()).hasSize(2);
        assertThat(query.getCriteria()).hasSize(1);
        assertThat(query.getCacheConfig().isEnabled()).isTrue();
        assertThat(query.getDefaultPageSize()).isEqualTo(20);
    }
}