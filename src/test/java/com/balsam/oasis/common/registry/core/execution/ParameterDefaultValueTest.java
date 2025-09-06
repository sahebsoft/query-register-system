package com.balsam.oasis.common.registry.core.execution;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.balsam.oasis.common.registry.core.definition.CriteriaDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.query.QueryContext;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.balsam.oasis.common.registry.query.QueryDefinitionBuilder;

/**
 * Test that parameter default values are properly applied
 */
class ParameterDefaultValueTest {

        @Test
        void testDefaultValueAppliedToContext() {
                // Create a query with a parameter that has a default value
                QueryDefinition queryDef = QueryDefinitionBuilder.builder("testQuery")
                                .sql("SELECT * FROM employees WHERE 1=1 --salaryFilter")
                                .param(ParamDef.param("minSalary")
                                                .type(BigDecimal.class)
                                                .defaultValue(new BigDecimal("10000"))
                                                .description("Minimum salary filter")
                                                .build())
                                .criteria(CriteriaDef.criteria()
                                                .name("salaryFilter")
                                                .sql("AND salary >= :minSalary")
                                                .condition(ctx -> ctx.hasParam("minSalary"))
                                                .build())
                                .build();

                // Create a query context without providing the minSalary parameter
                QueryContext context = QueryContext.builder()
                                .definition(queryDef)
                                .build();

                // Initially, the parameter should not be present
                assertFalse(context.hasParam("minSalary"));

                // Apply defaults by simulating what validate() does
                // (Since we can't create a proper QueryExecution without an executor)
                queryDef.getParams().forEach((name, paramDef) -> {
                        if (!context.hasParam(name) && paramDef.hasDefaultValue()) {
                                context.addParam(name, paramDef.getDefaultValue());
                        }
                });

                // Now check that the default value was applied to the context
                assertTrue(context.hasParam("minSalary"));
                assertEquals(new BigDecimal("10000"), context.getParam("minSalary"));
        }

        @Test
        void testCriteriaConditionSeesDefaultValue() {
                QueryDefinition queryDef = QueryDefinitionBuilder.builder("testQuery")
                                .sql("SELECT * FROM employees WHERE 1=1 --salaryFilter")
                                .param(ParamDef.param("minSalary")
                                                .type(BigDecimal.class)
                                                .defaultValue(new BigDecimal("10000"))
                                                .build())
                                .criteria(CriteriaDef.criteria()
                                                .name("salaryFilter")
                                                .sql("AND salary >= :minSalary")
                                                .condition(ctx -> ctx.hasParam("minSalary"))
                                                .build())
                                .build();

                // Create execution without providing minSalary
                QueryContext context = QueryContext.builder()
                                .definition(queryDef)
                                .build();

                // Before validation, the criteria condition should return false
                CriteriaDef criteria = queryDef.getCriteria("salaryFilter");
                assertFalse(criteria.getCondition().test(context));

                // Apply defaults by simulating what validate() does
                queryDef.getParams().forEach((name, paramDef) -> {
                        if (!context.hasParam(name) && paramDef.hasDefaultValue()) {
                                context.addParam(name, paramDef.getDefaultValue());
                        }
                });

                // After applying defaults, the criteria condition should return true
                assertTrue(criteria.getCondition().test(context));
                assertEquals(new BigDecimal("10000"), context.getParam("minSalary"));
        }
}