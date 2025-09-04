package com.balsam.oasis.common.query.rest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.balsam.oasis.common.query.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.query.core.definition.AttributeDef;
import com.balsam.oasis.common.query.core.definition.FilterOp;
import com.balsam.oasis.common.query.core.definition.ParamDef;
import com.balsam.oasis.common.query.core.definition.QueryDefinition;
import com.balsam.oasis.common.query.core.definition.SortDir;
import com.balsam.oasis.common.query.core.execution.QueryContext;
import com.balsam.oasis.common.query.rest.QueryRequest;
import com.balsam.oasis.common.query.rest.QueryRequestParser;

/**
 * Test for QueryRequestParser to verify parameter parsing without prefixes.
 */
public class QueryRequestParserTest {

        private QueryRequestParser parser;
        private QueryDefinition queryDefinition;

        @BeforeEach
        void setUp() {
                parser = new QueryRequestParser();

                // Create a test query definition with various parameter types
                // Use a unique name for each test run to avoid registration conflicts
                String uniqueName = "testQuery_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
                queryDefinition = QueryDefinitionBuilder.builder(uniqueName)
                                .sql("SELECT * FROM users WHERE 1=1")
                                .param(ParamDef.param("minSalary")
                                                .type(BigDecimal.class)
                                                .defaultValue(BigDecimal.ZERO)
                                                .required(false)
                                                .build())
                                .param(ParamDef.param("department")
                                                .type(String.class)
                                                .required(false)
                                                .build())
                                .param(ParamDef.param("statuses")
                                                .type(List.class)
                                                .required(false)
                                                .build())
                                .attribute(AttributeDef.name("id")
                                                .type(Long.class)
                                                .aliasName("user_id")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("name")
                                                .type(String.class)
                                                .aliasName("user_name")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                .attribute(AttributeDef.name("salary")
                                                .type(BigDecimal.class)
                                                .aliasName("salary")
                                                .filterable(true)
                                                .sortable(true)
                                                .build())
                                .build();
        }

        @Test
        void testParseParametersWithoutPrefix() {
                // Arrange
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("minSalary", "50000");
                params.add("department", "IT");
                params.add("_start", "0");
                params.add("_end", "10");

                // Act
                QueryRequest request = parser.parse(params, 0, 10, "full", queryDefinition);

                // Assert
                assertThat(request.getParams())
                                .hasSize(2)
                                .containsEntry("minSalary", new BigDecimal("50000"))
                                .containsEntry("department", "IT");

                assertThat(request.getStart()).isEqualTo(0);
                assertThat(request.getEnd()).isEqualTo(10);
                assertThat(request.getMetadataLevel()).isEqualTo("full");
        }

        @Test
        void testParseListParametersWithoutPrefix() {
                // Arrange
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("statuses", "ACTIVE,PENDING,COMPLETED");

                // Act
                QueryRequest request = parser.parse(params, 0, 10, "full", queryDefinition);

                // Assert
                assertThat(request.getParams()).hasSize(1);
                assertThat(request.getParams().get("statuses"))
                                .isInstanceOf(List.class)
                                .asList()
                                .hasSize(3)
                                .containsExactly("ACTIVE", "PENDING", "COMPLETED");
        }

        @Test
        void testParseFiltersUnchanged() {
                // Arrange
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("filter.name", "John");
                params.add("filter.salary.gte", "50000");
                params.add("filter.department.in", "IT,HR,Finance");

                // Act
                QueryRequest request = parser.parse(params, 0, 10, "full", queryDefinition);

                // Assert
                assertThat(request.getFilters()).hasSize(3);

                QueryContext.Filter nameFilter = request.getFilters().get("name");
                assertThat(nameFilter.getAttribute()).isEqualTo("name");
                assertThat(nameFilter.getOperator()).isEqualTo(FilterOp.EQUALS);
                assertThat(nameFilter.getValue()).isEqualTo("John");

                QueryContext.Filter salaryFilter = request.getFilters().get("salary");
                assertThat(salaryFilter.getAttribute()).isEqualTo("salary");
                assertThat(salaryFilter.getOperator()).isEqualTo(FilterOp.GREATER_THAN_OR_EQUAL);
                assertThat(salaryFilter.getValue()).isEqualTo(new BigDecimal("50000"));

                QueryContext.Filter deptFilter = request.getFilters().get("department");
                assertThat(deptFilter.getAttribute()).isEqualTo("department");
                assertThat(deptFilter.getOperator()).isEqualTo(FilterOp.IN);
                assertThat(deptFilter.getValues()).containsExactly("IT", "HR", "Finance");
        }

        @Test
        void testParseSortUnchanged() {
                // Arrange
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("sort", "salary.desc,name.asc");

                // Act
                QueryRequest request = parser.parse(params, 0, 10, "full", queryDefinition);

                // Assert
                assertThat(request.getSorts()).hasSize(2);

                QueryContext.SortSpec sort1 = request.getSorts().get(0);
                assertThat(sort1.getAttribute()).isEqualTo("salary");
                assertThat(sort1.getDirection()).isEqualTo(SortDir.DESC);

                QueryContext.SortSpec sort2 = request.getSorts().get(1);
                assertThat(sort2.getAttribute()).isEqualTo("name");
                assertThat(sort2.getDirection()).isEqualTo(SortDir.ASC);
        }

        @Test
        void testParseMixedParameters() {
                // Arrange - mixing direct params, filters, and sort
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("minSalary", "30000");
                params.add("department", "Sales");
                params.add("filter.name.like", "%Smith%");
                params.add("sort", "id.asc");
                params.add("_meta", "minimal");

                // Act
                QueryRequest request = parser.parse(params, 0, 50, "minimal", queryDefinition);

                // Assert
                assertThat(request.getParams())
                                .hasSize(2)
                                .containsEntry("minSalary", new BigDecimal("30000"))
                                .containsEntry("department", "Sales");

                assertThat(request.getFilters()).hasSize(1);
                QueryContext.Filter filter = request.getFilters().get("name");
                assertThat(filter.getOperator()).isEqualTo(FilterOp.LIKE);
                assertThat(filter.getValue()).isEqualTo("%Smith%");

                assertThat(request.getSorts()).hasSize(1);
                QueryContext.SortSpec sort = request.getSorts().get(0);
                assertThat(sort.getAttribute()).isEqualTo("id");
                assertThat(sort.getDirection()).isEqualTo(SortDir.ASC);

                assertThat(request.getMetadataLevel()).isEqualTo("minimal");
        }

        @Test
        void testIgnoreUnknownParameters() {
                // Arrange - include unknown parameters that should be ignored
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("minSalary", "40000");
                params.add("unknownParam", "shouldBeIgnored");
                params.add("anotherUnknown", "alsoIgnored");

                // Act
                QueryRequest request = parser.parse(params, 0, 10, "full", queryDefinition);

                // Assert - only minSalary should be included
                assertThat(request.getParams())
                                .hasSize(1)
                                .containsEntry("minSalary", new BigDecimal("40000"))
                                .doesNotContainKeys("unknownParam", "anotherUnknown");
        }

        @Test
        void testEmptyParametersIgnored() {
                // Arrange - empty parameters should be skipped
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("minSalary", "");
                params.add("department", " ");
                params.add("statuses", "ACTIVE");

                // Act
                QueryRequest request = parser.parse(params, 0, 10, "full", queryDefinition);

                // Assert - only non-empty statuses should be included
                assertThat(request.getParams())
                                .hasSize(1)
                                .containsKey("statuses")
                                .doesNotContainKeys("minSalary", "department");
        }
}