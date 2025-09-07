package com.balsam.oasis.common.registry.base;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.result.Row;
import com.balsam.oasis.common.registry.query.QueryContext;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.balsam.oasis.common.registry.query.QueryDefinitionBuilder;

/**
 * Simple test for BaseRowMapper functionality using QueryRowMapperImpl
 */
class BaseRowMapperSimpleTest {

    @Test
    void testBaseRowMapperFunctionality() throws SQLException {
        // Create a simple query definition
        QueryDefinition definition = QueryDefinitionBuilder.builder("testQuery")
                .sql("SELECT id, name FROM test_table")
                .attribute(AttributeDef.name("id").type(Integer.class).build())
                .attribute(AttributeDef.name("name").type(String.class).build())
                .build();

        // Create query context
        QueryContext context = QueryContext.builder()
                .definition(definition)
                .build();

        // Mock ResultSet
        ResultSet rs = mockResultSet(Map.of(
                "id", 123,
                "name", "Test Name"));

        // Create row mapper (using existing QueryRowMapperImpl which extends
        // BaseRowMapper)
        com.balsam.oasis.common.registry.query.QueryRowMapperImpl mapper = new com.balsam.oasis.common.registry.query.QueryRowMapperImpl();

        // Test mapping
        Row result = mapper.mapRow(rs, 0, context);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result.getInteger("id")).isEqualTo(123);
        assertThat(result.getString("name")).isEqualTo("Test Name");
    }

    private ResultSet mockResultSet(Map<String, Object> data) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(data.size());

        int columnIndex = 1;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            when(metadata.getColumnName(columnIndex)).thenReturn(columnName);
            when(metadata.getColumnLabel(columnIndex)).thenReturn(columnName);

            // Mock getObject calls
            when(rs.getObject(columnName)).thenReturn(value);
            when(rs.getObject(columnIndex)).thenReturn(value);

            // Mock type-specific calls
            if (value instanceof String) {
                when(rs.getString(columnName)).thenReturn((String) value);
                when(rs.getString(columnIndex)).thenReturn((String) value);
                when(metadata.getColumnType(columnIndex)).thenReturn(Types.VARCHAR);
            } else if (value instanceof Integer) {
                when(rs.getInt(columnName)).thenReturn((Integer) value);
                when(rs.getInt(columnIndex)).thenReturn((Integer) value);
                when(metadata.getColumnType(columnIndex)).thenReturn(Types.INTEGER);
            } else if (value instanceof BigDecimal) {
                when(rs.getBigDecimal(columnName)).thenReturn((BigDecimal) value);
                when(rs.getBigDecimal(columnIndex)).thenReturn((BigDecimal) value);
                when(metadata.getColumnType(columnIndex)).thenReturn(Types.DECIMAL);
            }

            columnIndex++;
        }

        return rs;
    }
}