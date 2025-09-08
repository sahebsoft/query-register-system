package com.balsam.oasis.common.registry.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.balsam.oasis.common.registry.api.QueryRegistrar;
import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.NamingStrategy;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.QueryDefinition;

import jakarta.annotation.PostConstruct;

/**
 * Example demonstrating dynamic attributes feature.
 * 
 * With dynamic attributes enabled, the query will return:
 * 1. All defined AttributeDef attributes as-is
 * 2. All other columns from SQL with the specified naming strategy
 */
@Configuration
public class DynamicAttributesExample {

    @Autowired
    private QueryRegistrar queryRegistrar;

    @PostConstruct
    public void registerQueries() {
        queryRegistrar.register(createUserQueryWithDynamicAttributes());
    }

    public static QueryDefinition createUserQueryWithDynamicAttributes() {
        return QueryDefinitionBuilder.builder("userQueryDynamic")
                .sql("""
                        select * from employees
                        """)
                .description("User query with dynamic attributes - returns all columns")
                // Enable dynamic attributes with camelCase naming
                .includeDynamicAttributes(true)
                .dynamicAttributeNamingStrategy(NamingStrategy.CAMEL)

                .build();
    }

    public static QueryDefinition createProductQueryWithSnakeCase() {
        return QueryDefinitionBuilder.builder("productQueryDynamic")
                .sql("""
                        SELECT
                            ProductID,
                            ProductName,
                            CategoryID,
                            SupplierID,
                            UnitPrice,
                            UnitsInStock,
                            ReorderLevel
                        FROM products
                        WHERE 1=1
                        --filters
                        """)
                .description("Product query with snake_case dynamic attributes")

                // Define only one attribute
                .attribute(AttributeDef.name("productId")
                        .type(Integer.class)
                        .aliasName("ProductID")
                        .build())

                // Enable dynamic attributes with snake_case naming
                .includeDynamicAttributes(true)
                .dynamicAttributeNamingStrategy(NamingStrategy.SNAKE)

                .build();
    }

    public static QueryDefinition createQueryWithoutDynamicAttributes() {
        return QueryDefinitionBuilder.builder("standardQuery")
                .sql("""
                        SELECT
                            id,
                            name,
                            description,
                            status,
                            created_date
                        FROM items
                        """)
                .description("Standard query - only returns defined attributes")

                // Define specific attributes
                .attribute(AttributeDef.name("id")
                        .type(Long.class)
                        .build())

                .attribute(AttributeDef.name("name")
                        .type(String.class)
                        .build())

                // Dynamic attributes disabled (default)
                // Only 'id' and 'name' will be in the response
                .includeDynamicAttributes(false)

                .build();
    }

    /**
     * Example output scenarios:
     * 
     * 1. With CAMEL strategy and user_name, email_address columns:
     * - Defined: userId, userName (as defined)
     * - Dynamic: emailAddress, createdDate, lastLoginDate, etc.
     * 
     * 2. With SNAKE strategy and ProductName, UnitPrice columns:
     * - Defined: productId (as defined)
     * - Dynamic: product_name, unit_price, units_in_stock, etc.
     * 
     * 3. With AS_IS strategy:
     * - Dynamic attributes keep original column names
     * 
     * 4. With includeDynamicAttributes(false):
     * - Only defined attributes appear in response
     */
}