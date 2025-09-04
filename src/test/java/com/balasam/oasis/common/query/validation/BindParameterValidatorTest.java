package com.balasam.oasis.common.query.validation;

import com.balasam.oasis.common.query.builder.QueryDefinitionBuilder;
import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.CriteriaDef;
import com.balasam.oasis.common.query.core.definition.ParamDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BindParameterValidator to ensure all bind parameters are defined.
 */
public class BindParameterValidatorTest {
    
    @BeforeEach
    void setUp() {
        // Clear registry before each test
        QueryDefinitionValidator.clearRegistry();
    }
    
    @Test
    void testValidQueryWithAllParametersDefined() {
        QueryDefinition query = QueryDefinitionBuilder.builder("validQuery")
                .sql("""
                    SELECT * FROM employees 
                    WHERE department_id = :departmentId
                    AND salary >= :minSalary
                    --statusFilter
                    """)
                .param(ParamDef.param("departmentId")
                        .type(Integer.class)
                        .build())
                .param(ParamDef.param("minSalary")
                        .type(BigDecimal.class)
                        .build())
                .criteria(CriteriaDef.criteria()
                        .name("statusFilter")
                        .sql("AND status = :status")
                        .bindParams("status")
                        .build())
                .param(ParamDef.param("status")
                        .type(String.class)
                        .build())
                .build();
        
        // Should not throw exception
        assertThatCode(() -> BindParameterValidator.validate(query))
                .doesNotThrowAnyException();
    }
    
    @Test
    void testQueryWithUndefinedParameter() {
        QueryDefinition query = QueryDefinitionBuilder.builder("invalidQuery")
                .sql("""
                    SELECT * FROM employees 
                    WHERE department_id = :departmentId
                    AND salary >= :minSalary
                    AND status = :undefinedParam
                    """)
                .param(ParamDef.param("departmentId")
                        .type(Integer.class)
                        .build())
                .param(ParamDef.param("minSalary")
                        .type(BigDecimal.class)
                        .build())
                // Note: undefinedParam is NOT defined
                .build();
        
        // Should throw exception for undefined parameter
        assertThatThrownBy(() -> BindParameterValidator.validate(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("undefined bind parameters")
                .hasMessageContaining("undefinedParam");
    }
    
    @Test
    void testQueryWithUndefinedParameterInCriteria() {
        QueryDefinition query = QueryDefinitionBuilder.builder("invalidQueryCriteria")
                .sql("""
                    SELECT * FROM employees 
                    WHERE 1=1
                    --statusFilter
                    """)
                .criteria(CriteriaDef.criteria()
                        .name("statusFilter")
                        .sql("AND status = :status AND type = :type")
                        .bindParams("status", "type")
                        .build())
                .param(ParamDef.param("status")
                        .type(String.class)
                        .build())
                // Note: type parameter is NOT defined
                .build();
        
        // Should throw exception for undefined parameter in criteria
        assertThatThrownBy(() -> BindParameterValidator.validate(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("undefined bind parameters")
                .hasMessageContaining("type");
    }
    
    @Test
    void testExtractBindParameters() {
        String sql = """
            SELECT * FROM employees e
            WHERE e.department_id = :departmentId
            AND e.salary BETWEEN :minSalary AND :maxSalary
            AND e.status IN (:statuses)
            AND (:includeInactive = 1 OR e.active = true)
            """;
        
        Set<String> params = BindParameterValidator.extractBindParameters(sql);
        
        assertThat(params)
                .containsExactlyInAnyOrder(
                        "departmentId", 
                        "minSalary", 
                        "maxSalary", 
                        "statuses", 
                        "includeInactive"
                );
    }
    
    @Test
    void testSystemParametersAreAllowed() {
        QueryDefinition query = QueryDefinitionBuilder.builder("paginatedQuery")
                .sql("""
                    SELECT * FROM (
                        SELECT /*+ FIRST_ROWS(n) */ a.*, ROWNUM rnum FROM (
                            SELECT * FROM employees
                        ) a WHERE ROWNUM <= :endRow
                    ) WHERE rnum >= :startRow
                    """)
                // startRow and endRow are system parameters for pagination
                // They should not need to be explicitly defined
                .build();
        
        // Should not throw exception - system parameters are allowed
        assertThatCode(() -> BindParameterValidator.validate(query))
                .doesNotThrowAnyException();
    }
    
    @Test
    void testFilterGeneratedParametersAreAllowed() {
        QueryDefinition query = QueryDefinitionBuilder.builder("filterQuery")
                .sql("""
                    SELECT * FROM employees
                    WHERE salary >= :salary_gte
                    AND department_id = :departmentId_eq
                    """)
                .attribute(AttributeDef.name("salary")
                        .type(BigDecimal.class)
                        .aliasName("salary")
                        .filterable(true)
                        .build())
                .attribute(AttributeDef.name("departmentId")
                        .type(Integer.class)
                        .aliasName("department_id")
                        .filterable(true)
                        .build())
                // Filter-generated parameters should be allowed
                .build();
        
        // Should not throw exception - filter-generated parameters are allowed
        assertThatCode(() -> BindParameterValidator.validate(query))
                .doesNotThrowAnyException();
    }
    
    @Test
    void testCriteriaWithUndeclaredBindParams() {
        QueryDefinition query = QueryDefinitionBuilder.builder("criteriaBindParamTest")
                .sql("""
                    SELECT * FROM employees
                    WHERE 1=1
                    --statusFilter
                    """)
                .criteria(CriteriaDef.criteria()
                        .name("statusFilter")
                        .sql("AND status = :status AND type = :type")
                        .bindParams("status") // type is used but not declared
                        .build())
                .param(ParamDef.param("status")
                        .type(String.class)
                        .build())
                .param(ParamDef.param("type")
                        .type(String.class)
                        .build())
                .build();
        
        // Should throw exception for undeclared bind param in criteria
        assertThatThrownBy(() -> BindParameterValidator.validate(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not declared in bindParams")
                .hasMessageContaining("type");
    }
    
    @Test
    void testValidationResult() {
        QueryDefinition validQuery = QueryDefinitionBuilder.builder("validQuery")
                .sql("SELECT * FROM employees WHERE id = :id")
                .param(ParamDef.param("id")
                        .type(Integer.class)
                        .build())
                .build();
        
        BindParameterValidator.ValidationResult result = 
                BindParameterValidator.validateWithDetails(validQuery);
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getQueryName()).isEqualTo("validQuery");
        assertThat(result.getErrorMessage()).isNull();
    }
    
    @Test
    void testValidationResultWithError() {
        QueryDefinition invalidQuery = QueryDefinitionBuilder.builder("invalidQuery")
                .sql("SELECT * FROM employees WHERE id = :id AND status = :undefined")
                .param(ParamDef.param("id")
                        .type(Integer.class)
                        .build())
                .build();
        
        BindParameterValidator.ValidationResult result = 
                BindParameterValidator.validateWithDetails(invalidQuery);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getQueryName()).isEqualTo("invalidQuery");
        assertThat(result.getErrorMessage()).contains("undefined");
    }
}