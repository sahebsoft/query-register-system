package com.balasam.oasis.common.query.example;

import java.math.BigDecimal;

import org.springframework.context.annotation.Configuration;

import com.balasam.oasis.common.query.builder.QueryDefinitionBuilder;
import com.balasam.oasis.common.query.core.definition.AttributeDef;
import com.balasam.oasis.common.query.core.definition.CriteriaDef;
import com.balasam.oasis.common.query.core.definition.ParamDef;
import com.balasam.oasis.common.query.core.definition.QueryDefinition;

/**
 * Example of queries that will FAIL validation and prevent application startup.
 * These are intentionally invalid to demonstrate the validation system.
 * 
 * Uncomment any of these @Bean methods to see the application fail to start
 * with a clear error message about what's wrong.
 */
@Configuration
public class InvalidQueryExample {

        /**
         * Example 1: Query with undefined bind parameter
         * The SQL uses :undefinedParam but it's not defined in params
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: undefined bind parameters:
        // [undefinedParam]
        public QueryDefinition queryWithUndefinedParameter() {
                return QueryDefinitionBuilder.builder("invalidQuery1")
                                .sql("""
                                                SELECT * FROM employees
                                                WHERE department_id = :departmentId
                                                AND salary >= :minSalary
                                                AND status = :undefinedParam  -- This parameter is NOT defined!
                                                """)
                                .param(ParamDef.param("departmentId")
                                                .type(Integer.class)
                                                .build())
                                .param(ParamDef.param("minSalary")
                                                .type(BigDecimal.class)
                                                .build())
                                // Note: undefinedParam is NOT defined
                                .build();
        }

        /**
         * Example 2: Query with undefined parameter in criteria
         * The criteria SQL uses :type but it's not defined
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: undefined bind parameters:
        // [type]
        public QueryDefinition queryWithUndefinedCriteriaParam() {
                return QueryDefinitionBuilder.builder("invalidQuery2")
                                .sql("""
                                                SELECT * FROM employees
                                                WHERE 1=1
                                                --statusFilter
                                                """)
                                .criteria(CriteriaDef.criteria()
                                                .name("statusFilter")
                                                .sql("AND status = :status AND type = :type") // :type is not defined!
                                                .build())
                                .param(ParamDef.param("status")
                                                .type(String.class)
                                                .build())
                                // Note: type parameter is NOT defined
                                .build();
        }

        /**
         * Example 3: Duplicate query name
         * If you have another query with name "employees" this will fail
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: Duplicate query definition:
        // 'employees'
        public QueryDefinition duplicateQueryName() {
                return QueryDefinitionBuilder.builder("employees") // Same name as OracleHRQueryConfig
                                .sql("SELECT * FROM employees")
                                .build();
        }

        /**
         * Example 4: Duplicate attribute names
         * Two attributes with the same name
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: Duplicate attribute definition:
        // 'salary'
        public QueryDefinition queryWithDuplicateAttributes() {
                return QueryDefinitionBuilder.builder("invalidQuery3")
                                .sql("SELECT salary, bonus FROM employees")
                                .attribute(AttributeDef.name("salary")
                                                .type(BigDecimal.class)
                                                .aliasName("SALARY")
                                                .build())
                                .attribute(AttributeDef.name("salary") // Duplicate name!
                                                .type(BigDecimal.class)
                                                .aliasName("BONUS")
                                                .build())
                                .build();
        }

        /**
         * Example 5: Duplicate parameter names
         * Two parameters with the same name
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: Duplicate parameter definition:
        // 'deptId'
        public QueryDefinition queryWithDuplicateParams() {
                return QueryDefinitionBuilder.builder("invalidQuery4")
                                .sql("SELECT * FROM employees WHERE department_id = :deptId")
                                .param(ParamDef.param("deptId")
                                                .type(Integer.class)
                                                .build())
                                .param(ParamDef.param("deptId") // Duplicate name!
                                                .type(String.class)
                                                .build())
                                .build();
        }

        /**
         * Example 6: Duplicate criteria names
         * Two criteria with the same name
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: Duplicate criteria definition:
        // 'statusFilter'
        public QueryDefinition queryWithDuplicateCriteria() {
                return QueryDefinitionBuilder.builder("invalidQuery5")
                                .sql("SELECT * FROM employees --statusFilter")
                                .criteria(CriteriaDef.criteria()
                                                .name("statusFilter")
                                                .sql("AND status = 'ACTIVE'")
                                                .build())
                                .criteria(CriteriaDef.criteria()
                                                .name("statusFilter") // Duplicate name!
                                                .sql("AND status = 'INACTIVE'")
                                                .build())
                                .build();
        }

        /**
         * Example 7: Criteria with undeclared bind params
         * Criteria uses parameters but doesn't declare them in bindParams
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: not declared in bindParams
        public QueryDefinition queryWithUndeclaredBindParams() {
                return QueryDefinitionBuilder.builder("invalidQuery6")
                                .sql("SELECT * FROM employees --statusFilter")
                                .criteria(CriteriaDef.criteria()
                                                .name("statusFilter")
                                                .sql("AND status = :status AND type = :type")
                                                .build())
                                .param(ParamDef.param("status")
                                                .type(String.class)
                                                .build())
                                .param(ParamDef.param("type")
                                                .type(String.class)
                                                .build())
                                .build();
        }

        /**
         * Example 8: Multiple attributes mapped to same database column
         * This can cause conflicts in result mapping
         */
        // @Bean // UNCOMMENT TO TEST - Will fail with: duplicate attribute alias/column
        // name
        public QueryDefinition queryWithDuplicateAliasNames() {
                return QueryDefinitionBuilder.builder("invalidQuery7")
                                .sql("SELECT name FROM employees")
                                .attribute(AttributeDef.name("firstName")
                                                .type(String.class)
                                                .aliasName("NAME") // Maps to NAME column
                                                .build())
                                .attribute(AttributeDef.name("lastName")
                                                .type(String.class)
                                                .aliasName("name") // Also maps to NAME column (case-insensitive)!
                                                .build())
                                .build();
        }
}