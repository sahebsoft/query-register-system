package com.balasam.oasis.common.query.validation;

import com.balasam.oasis.common.query.builder.QueryDefinitionBuilder;
import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.CriteriaDef;
import com.balasam.oasis.common.query.core.definition.ParamDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for duplicate detection in query definitions.
 */
public class DuplicateValidationTest {
    
    @BeforeEach
    void setUp() {
        // Clear registry before each test
        QueryDefinitionValidator.clearRegistry();
    }
    
    @Test
    void testDuplicateQueryName() {
        // Create first query
        QueryDefinition query1 = QueryDefinitionBuilder.builder("duplicateQuery")
                .sql("SELECT * FROM table1")
                .build();
        
        // Register first query
        QueryDefinitionValidator.validateAndRegister(query1);
        
        // Create second query with same name
        QueryDefinition query2 = QueryDefinitionBuilder.builder("duplicateQuery")
                .sql("SELECT * FROM table2")
                .build();
        
        // Should throw exception for duplicate query name
        assertThatThrownBy(() -> QueryDefinitionValidator.validateAndRegister(query2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate query definition")
                .hasMessageContaining("duplicateQuery");
    }
    
    @Test
    void testDuplicateAttributeNameDuringBuilding() {
        QueryDefinitionBuilder builder = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM employees");
        
        // Add first attribute
        builder.attribute(AttributeDef.name("salary")
                .type(BigDecimal.class)
                .aliasName("SALARY")
                .build());
        
        // Try to add duplicate attribute
        AttributeDef<?> duplicate = AttributeDef.name("salary")
                .type(BigDecimal.class)
                .aliasName("SALARY2")
                .build();
        
        // Should throw exception during building
        assertThatThrownBy(() -> builder.attribute(duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate attribute definition")
                .hasMessageContaining("salary");
    }
    
    @Test
    void testDuplicateParameterNameDuringBuilding() {
        QueryDefinitionBuilder builder = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM employees WHERE dept = :deptId");
        
        // Add first parameter
        builder.param(ParamDef.param("deptId")
                .type(Integer.class)
                .build());
        
        // Try to add duplicate parameter
        ParamDef<?> duplicate = ParamDef.param("deptId")
                .type(String.class) // Even with different type
                .build();
        
        // Should throw exception during building
        assertThatThrownBy(() -> builder.param(duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate parameter definition")
                .hasMessageContaining("deptId");
    }
    
    @Test
    void testDuplicateCriteriaNameDuringBuilding() {
        QueryDefinitionBuilder builder = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM employees --statusFilter");
        
        // Add first criteria
        builder.criteria(CriteriaDef.criteria()
                .name("statusFilter")
                .sql("AND status = 'ACTIVE'")
                .build());
        
        // Try to add duplicate criteria
        CriteriaDef duplicate = CriteriaDef.criteria()
                .name("statusFilter")
                .sql("AND status = 'INACTIVE'")
                .build();
        
        // Should throw exception during building
        assertThatThrownBy(() -> builder.criteria(duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate criteria definition")
                .hasMessageContaining("statusFilter");
    }
    
    @Test
    void testDuplicateAttributeAliasName() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM employees")
                .attribute(AttributeDef.name("firstName")
                        .type(String.class)
                        .aliasName("EMPLOYEE_NAME")
                        .build())
                .attribute(AttributeDef.name("lastName")
                        .type(String.class)
                        .aliasName("employee_name") // Same alias (case-insensitive)
                        .build())
                .build();
        
        // Should throw exception for duplicate alias name
        assertThatThrownBy(() -> QueryDefinitionValidator.validateNoDuplicates(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate attribute alias/column name")
                .hasMessageContaining("employee_name");
    }
    
    @Test
    void testTransientAttributesCanHaveNullAlias() {
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT salary FROM employees")
                .attribute(AttributeDef.name("salary")
                        .type(BigDecimal.class)
                        .aliasName("SALARY")
                        .build())
                .attribute(AttributeDef.name("annualSalary")
                        .type(BigDecimal.class)
                        .transient_(true)
                        .calculated((row, ctx) -> {
                            BigDecimal monthly = row.get("salary", BigDecimal.class);
                            return monthly.multiply(BigDecimal.valueOf(12));
                        })
                        .build())
                .attribute(AttributeDef.name("bonus")
                        .type(BigDecimal.class)
                        .transient_(true)
                        .calculated((row, ctx) -> {
                            BigDecimal annual = row.getVirtual("annualSalary", BigDecimal.class);
                            return annual.multiply(BigDecimal.valueOf(0.1));
                        })
                        .build())
                .build();
        
        // Should not throw exception - transient attributes can have null alias
        assertThatCode(() -> QueryDefinitionValidator.validateNoDuplicates(query))
                .doesNotThrowAnyException();
    }
    
    @Test
    void testNamingConflictWarning() {
        // This test verifies that naming conflicts between attribute/param/criteria
        // produce warnings but don't fail validation
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM employees WHERE dept = :status --status")
                .attribute(AttributeDef.name("status")
                        .type(String.class)
                        .aliasName("STATUS")
                        .build())
                .param(ParamDef.param("status")
                        .type(String.class)
                        .build())
                .criteria(CriteriaDef.criteria()
                        .name("status")
                        .sql("AND status = 'ACTIVE'")
                        .build())
                .build();
        
        // Should not throw exception - naming conflicts are warnings only
        assertThatCode(() -> QueryDefinitionValidator.validateNoDuplicates(query))
                .doesNotThrowAnyException();
        // Note: This will print warnings to console
    }
    
    @Test
    void testQueryRegistryOperations() {
        // Test registry is empty initially
        assertThat(QueryDefinitionValidator.getRegisteredQueries()).isEmpty();
        assertThat(QueryDefinitionValidator.isQueryNameRegistered("testQuery")).isFalse();
        
        // Register a query
        QueryDefinition query = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT * FROM test")
                .build();
        QueryDefinitionValidator.validateAndRegister(query);
        
        // Check registry contains the query
        assertThat(QueryDefinitionValidator.getRegisteredQueries()).hasSize(1);
        assertThat(QueryDefinitionValidator.isQueryNameRegistered("testQuery")).isTrue();
        assertThat(QueryDefinitionValidator.getRegisteredQueries().get("testQuery")).isEqualTo(query);
        
        // Clear registry
        QueryDefinitionValidator.clearRegistry();
        assertThat(QueryDefinitionValidator.getRegisteredQueries()).isEmpty();
    }
    
    @Test
    void testMultipleUniqueQueriesCanBeRegistered() {
        QueryDefinition query1 = QueryDefinitionBuilder.builder("query1")
                .sql("SELECT * FROM table1")
                .build();
        
        QueryDefinition query2 = QueryDefinitionBuilder.builder("query2")
                .sql("SELECT * FROM table2")
                .build();
        
        QueryDefinition query3 = QueryDefinitionBuilder.builder("query3")
                .sql("SELECT * FROM table3")
                .build();
        
        // All should register successfully
        assertThatCode(() -> {
            QueryDefinitionValidator.validateAndRegister(query1);
            QueryDefinitionValidator.validateAndRegister(query2);
            QueryDefinitionValidator.validateAndRegister(query3);
        }).doesNotThrowAnyException();
        
        // Check all are registered
        assertThat(QueryDefinitionValidator.getRegisteredQueries()).hasSize(3);
        assertThat(QueryDefinitionValidator.isQueryNameRegistered("query1")).isTrue();
        assertThat(QueryDefinitionValidator.isQueryNameRegistered("query2")).isTrue();
        assertThat(QueryDefinitionValidator.isQueryNameRegistered("query3")).isTrue();
    }
}