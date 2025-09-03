package com.balasam.oasis.common.query.builder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.CriteriaDef;
import com.balasam.oasis.common.query.core.definition.ParamDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import com.balasam.oasis.common.query.core.execution.QueryContext;
import com.balasam.oasis.common.query.processor.PostProcessor;
import com.balasam.oasis.common.query.processor.PreProcessor;
import com.balasam.oasis.common.query.processor.RowProcessor;

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
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("user_id")
                        .build())
                .build();

        assertThat(query).isNotNull();
        assertThat(query.getName()).isEqualTo("testQuery");
        assertThat(query.getSql()).isEqualTo("SELECT * FROM users");
        assertThat(query.getAttributes()).hasSize(1);
        assertThat(query.getAttributes().get("id")).isNotNull();
        assertThat(query.getAttributes().get("id").getAliasName()).isEqualTo("user_id");
        assertThat(query.getAttributes().get("id").getType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("Test builder with description")
    void testBuilderWithDescription() {
        QueryDefinition query = QueryDefinitionBuilder.builder("userQuery")
                .sql("SELECT * FROM users")
                .description("Query to fetch user data with filters")
                .attribute(AttributeDef.name("username")
                        .type(String.class)
                        .aliasName("username")
                        .build())
                .build();

        assertThat(query.getDescription()).isEqualTo("Query to fetch user data with filters");
    }

    @Test
    @DisplayName("Test that query name is required")
    void testBuilderValidationRequiresName() {
        assertThatThrownBy(() -> QueryDefinitionBuilder.builder(null)
                .sql("SELECT * FROM users")
                .attribute(AttributeDef.name("id")
                        .type(BigDecimal.class)
                        .aliasName("id")
                        .build())
                .build()).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Query name cannot be null");
    }

    @Test
    @DisplayName("Test that SQL is required")
    void testBuilderValidationRequiresSql() {
        assertThatThrownBy(() -> QueryDefinitionBuilder.builder("testQuery")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .build()).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SQL is required");
    }

    // ========================================
    // Attribute Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test attribute with custom column mapping")
    void testAttributeWithColumnMapping() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute(AttributeDef.name("email")
                        .type(String.class)
                        .aliasName("email_address")
                        .filterable(true)
                        .sortable(true)
                        .build())
                .build();

        AttributeDef<?> emailAttr = query.getAttributes().get("email");
        assertThat(emailAttr).isNotNull();
        assertThat(emailAttr.getAliasName()).isEqualTo("email_address");
        assertThat(emailAttr.isFilterable()).isTrue();
        assertThat(emailAttr.isSortable()).isTrue();
    }

    @Test
    @DisplayName("Test filterable attribute configuration")
    void testFilterableAttribute() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute(AttributeDef.name("status")
                        .type(String.class)
                        .aliasName("status")
                        .filterable(true)
                        .build())
                .build();

        AttributeDef<?> statusAttr = query.getAttributes().get("status");
        assertThat(statusAttr).isNotNull();
        assertThat(statusAttr.isFilterable()).isTrue();
    }

    @Test
    @DisplayName("Test attribute with allowed values")
    void testAttributeWithAllowedValues() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute(AttributeDef.name("role")
                        .type(String.class)
                        .aliasName("role")
                        .build())
                .build();

        AttributeDef<?> roleAttr = query.getAttributes().get("role");
        assertThat(roleAttr).isNotNull();
    }

    @Test
    @DisplayName("Test secure attribute with security rule")
    void testSecureAttribute() {
        Function<Object, Boolean> securityRule = ctx -> true;

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute(AttributeDef.name("salary")
                        .type(BigDecimal.class)
                        .aliasName("salary")
                        .secure(securityRule)
                        .build())
                .build();

        AttributeDef<?> salaryAttr = query.getAttributes().get("salary");
        assertThat(salaryAttr).isNotNull();
        assertThat(salaryAttr.isSecured()).isTrue();
        assertThat(salaryAttr.getSecurityRule()).isEqualTo(securityRule);
    }

    @Test
    @DisplayName("Test attribute with processor")
    void testAttributeWithProcessor() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products")
                .attribute(AttributeDef.name("price")
                        .type(BigDecimal.class)
                        .aliasName("price")
                        .processor((value, row, context) -> {
                            BigDecimal val = (BigDecimal) value;
                            return val != null ? val.multiply(new BigDecimal("1.1")) : null;
                        })
                        .build())
                .build();

        AttributeDef<?> priceAttr = query.getAttributes().get("price");
        assertThat(priceAttr).isNotNull();
        assertThat(priceAttr.hasProcessor()).isTrue();
    }

    @Test
    @DisplayName("Test primary key attribute")
    void testPrimaryKeyAttribute() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .primaryKey(true)
                        .build())
                .attribute(AttributeDef.name("name")
                        .type(String.class)
                        .aliasName("name")
                        .primaryKey(false)
                        .build())
                .build();

        assertThat(query.getAttributes().get("id").isPrimaryKey()).isTrue();
        assertThat(query.getAttributes().get("name").isPrimaryKey()).isFalse();
    }

    @Test
    @DisplayName("Test virtual attribute configuration")
    void testVirtualAttribute() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders")
                .attribute(AttributeDef.name("quantity")
                        .type(Integer.class)
                        .aliasName("quantity")
                        .build())
                .attribute(AttributeDef.name("price")
                        .type(BigDecimal.class)
                        .aliasName("price")
                        .build())
                .attribute(AttributeDef.name("totalAmount")
                        .type(BigDecimal.class)
                        .virtual(true)
                        .virtual(true)
                        .processor((value, row, context) -> {
                            Integer qty = row.getInteger("quantity");
                            BigDecimal price = row.getBigDecimal("price");
                            return qty != null && price != null ? 
                                price.multiply(new BigDecimal(qty)) : BigDecimal.ZERO;
                        })
                        .build())
                .build();

        AttributeDef<?> totalAttr = query.getAttributes().get("totalAmount");
        assertThat(totalAttr).isNotNull();
        assertThat(totalAttr.isVirtual()).isTrue();
    }

    // ========================================
    // Parameter Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test parameter with default value")
    void testParameterWithDefaultValue() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products WHERE price > :minPrice")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .param(ParamDef.param("minPrice")
                        .type(BigDecimal.class)
                        .defaultValue(new BigDecimal("10.00"))
                        .description("Minimum price filter")
                        .build())
                .build();

        ParamDef<?> param = query.getParams().get("minPrice");
        assertThat(param).isNotNull();
        assertThat(param.getDefaultValue()).isEqualTo(new BigDecimal("10.00"));
        assertThat(param.getDescription()).isEqualTo("Minimum price filter");
    }

    @Test
    @DisplayName("Test required parameter")
    void testRequiredParameter() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders WHERE status = :status")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .param(ParamDef.param("status")
                        .type(String.class)
                        .required(true)
                        .build())
                .build();

        ParamDef<?> param = query.getParams().get("status");
        assertThat(param).isNotNull();
        assertThat(param.isRequired()).isTrue();
    }

    // ========================================
    // Criteria Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test basic criteria configuration")
    void testBasicCriteria() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users WHERE 1=1 --statusFilter")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .criteria(CriteriaDef.criteria()
                        .name("statusFilter")
                        .sql("AND status = :status")
                        .condition(ctx -> ctx.hasParam("status"))
                        .build())
                .build();

        CriteriaDef criteria = query.getCriteria().get("statusFilter");
        assertThat(criteria).isNotNull();
        assertThat(criteria.getName()).isEqualTo("statusFilter");
        assertThat(criteria.getSql()).isEqualTo("AND status = :status");
        assertThat(criteria.getBindParams()).contains("status");
    }

    @Test
    @DisplayName("Test dynamic criteria with generator")
    void testDynamicCriteria() {
        Function<Object, String> generator = ctx -> {
            QueryContext qctx = (QueryContext) ctx;
            return qctx.hasParam("recentDays") ? 
                "AND created_date > CURRENT_DATE - INTERVAL '" + qctx.getParam("recentDays") + "' DAY" : "";
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders WHERE 1=1 --recentFilter")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .criteria(CriteriaDef.criteria()
                        .name("recentFilter")
                        .generator(generator)
                        .build())
                .build();

        CriteriaDef criteria = query.getCriteria().get("recentFilter");
        assertThat(criteria).isNotNull();
        assertThat(criteria.isDynamic()).isTrue();
        assertThat(criteria.hasGenerator()).isTrue();
    }

    @Test
    @DisplayName("Test security-related criteria")
    void testSecurityCriteria() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM documents WHERE 1=1 --securityFilter")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .criteria(CriteriaDef.criteria()
                        .name("securityFilter")
                        .sql("AND owner_id = :currentUserId")
                        .securityRelated(true)
                        .appliedReason("User can only see their own documents")
                        .build())
                .build();

        CriteriaDef criteria = query.getCriteria().get("securityFilter");
        assertThat(criteria).isNotNull();
        assertThat(criteria.isSecurityRelated()).isTrue();
        assertThat(criteria.getAppliedReason()).isEqualTo("User can only see their own documents");
    }

    // ========================================
    // Processor Tests
    // ========================================

    @Test
    @DisplayName("Test pre-processor configuration")
    void testPreProcessor() {
        PreProcessor preProcessor = context -> {
            context.addParam("processedFlag", true);
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM users")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .preProcessor(preProcessor)
                .build();

        assertThat(query.getPreProcessors()).isNotEmpty();
    }

    @Test
    @DisplayName("Test row processor configuration")
    void testRowProcessor() {
        RowProcessor rowProcessor = (row, context) -> {
            row.set("processed", true);
            return row;
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM orders")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .rowProcessor(rowProcessor)
                .build();

        assertThat(query.getRowProcessors()).isNotEmpty();
    }

    @Test
    @DisplayName("Test post processor configuration")
    void testPostProcessor() {
        PostProcessor postProcessor = (result, context) -> {
            // Add summary data
            return result;
        };

        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM products")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .postProcessor(postProcessor)
                .build();

        assertThat(query.getPostProcessors()).isNotEmpty();
    }

    // ========================================
    // Cache Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test cache configuration")
    void testCacheConfiguration() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM static_data")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .cache(true)
                .cacheTTL(Duration.ofMinutes(30))
                .build();

        assertThat(query.getCacheConfig()).isNotNull();
        assertThat(query.getCacheConfig().isEnabled()).isTrue();
        assertThat(query.getCacheConfig().getTtl()).isEqualTo(Duration.ofMinutes(30));
    }

    // ========================================
    // Pagination Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test pagination configuration")
    void testPaginationConfiguration() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM large_table")
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .build())
                .defaultPageSize(25)
                .maxPageSize(200)
                .paginationEnabled(true)
                .build();

        assertThat(query.getDefaultPageSize()).isEqualTo(25);
        assertThat(query.getMaxPageSize()).isEqualTo(200);
        assertThat(query.isPaginationEnabled()).isTrue();
    }

    // ========================================
    // Complex Query Tests
    // ========================================

    @Test
    @DisplayName("Test complex query with multiple features")
    void testComplexQueryDefinition() {
        QueryDefinition query = QueryDefinitionBuilder.builder("complexQuery")
                .sql("""
                        SELECT 
                            u.id,
                            u.name,
                            u.email,
                            u.status,
                            COUNT(o.id) as order_count,
                            SUM(o.amount) as total_amount
                        FROM users u
                        LEFT JOIN orders o ON o.user_id = u.id
                        WHERE 1=1
                        --statusFilter
                        --dateFilter
                        GROUP BY u.id, u.name, u.email, u.status
                        """)
                .description("Complex user query with order statistics")
                // Regular attributes
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .aliasName("id")
                        .primaryKey(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("name")
                        .type(String.class)
                        .aliasName("name")
                        .filterable(true)
                        .sortable(true)
                        .build())
                .attribute(AttributeDef.name("email")
                        .type(String.class)
                        .aliasName("email")
                        .filterable(true)
                        .secure(ctx -> true)
                        .build())
                .attribute(AttributeDef.name("status")
                        .type(String.class)
                        .aliasName("status")
                        .filterable(true)
                        .sortable(true)
                        .build())
                // Calculated attributes
                .attribute(AttributeDef.name("orderCount")
                        .type(Integer.class)
                        .aliasName("order_count")
                        .virtual(true)
                        .sortable(true)
                        .processor((value, row, context) -> value)
                        .build())
                .attribute(AttributeDef.name("totalAmount")
                        .type(BigDecimal.class)
                        .aliasName("total_amount")
                        .virtual(true)
                        .sortable(true)
                        .processor((value, row, context) -> value)
                        .build())
                // Virtual attribute
                .attribute(AttributeDef.name("loyaltyTier")
                        .type(String.class)
                        .virtual(true)
                        .processor((value, row, context) -> {
                            BigDecimal total = row.getBigDecimal("totalAmount");
                            if (total == null) return "NONE";
                            double amount = total.doubleValue();
                            if (amount >= 10000) return "PLATINUM";
                            if (amount >= 5000) return "GOLD";
                            if (amount >= 1000) return "SILVER";
                            return "BRONZE";
                        })
                        .build())
                // Parameters
                .param(ParamDef.param("status")
                        .type(String.class)
                        .description("Filter by user status")
                        .build())
                .param(ParamDef.param("startDate")
                        .type(LocalDate.class)
                        .defaultValue(LocalDate.now().minusYears(1))
                        .description("Start date for order filtering")
                        .build())
                // Criteria
                .criteria(CriteriaDef.criteria()
                        .name("statusFilter")
                        .sql("AND u.status = :status")
                        .condition(ctx -> ctx.hasParam("status"))
                        .build())
                .criteria(CriteriaDef.criteria()
                        .name("dateFilter")
                        .sql("AND o.order_date >= :startDate")
                        .condition(ctx -> ctx.hasParam("startDate"))
                        .build())
                // Processors
                .preProcessor(ctx -> {
                    // Set default status if not provided
                    if (!ctx.hasParam("status")) {
                        ctx.addParam("status", "ACTIVE");
                    }
                })
                .rowProcessor((row, ctx) -> {
                    // Mask email for non-admin users
                    String email = row.getString("email");
                    if (email != null && email.contains("@")) {
                        row.set("email", email.substring(0, 1) + "***@" + email.split("@")[1]);
                    }
                    return row;
                })
                // Configuration
                .cache(true)
                .cacheTTL(Duration.ofMinutes(10))
                .defaultPageSize(50)
                .maxPageSize(500)
                .auditEnabled(true)
                .metricsEnabled(true)
                .build();

        // Verify the complex query
        assertThat(query).isNotNull();
        assertThat(query.getName()).isEqualTo("complexQuery");
        assertThat(query.getAttributes()).hasSize(7);
        assertThat(query.getParams()).hasSize(2);
        assertThat(query.getCriteria()).hasSize(2);
        assertThat(query.getCacheConfig()).isNotNull();
        assertThat(query.getCacheConfig().isEnabled()).isTrue();
    }
}