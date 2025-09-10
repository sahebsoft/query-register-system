package com.balsam.oasis.common.registry.engine.sql.util;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Centralized SQL type to Java type mapping utility.
 * Provides:
 * - SQL type → Java class mapping
 * - Dummy value generation for each type
 *
 * Used by MetadataCacheBuilder, RowMappers, and parameter initialization.
 */
public class SqlTypeMapper {

    /**
     * Map a SQL type (java.sql.Types constant) to its corresponding Java class.
     *
     * @param sqlType JDBC type from {@link java.sql.Types}
     * @return Java class (never null, defaults to Object.class)
     */
    public static Class<?> sqlTypeToJavaClass(int sqlType) {
        switch (sqlType) {

            // --- Numeric types ---
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return Integer.class;

            case Types.BIGINT:
                return Long.class;

            case Types.DECIMAL:
            case Types.NUMERIC:
                return BigDecimal.class;

            case Types.FLOAT: // single precision
            case Types.REAL:
                return Float.class;

            case Types.DOUBLE: // double precision
                return Double.class;

            // --- Character/string types ---
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
                return String.class;

            // --- Date/time types ---
            case Types.DATE:
                return LocalDate.class;

            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                return LocalTime.class;

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return LocalDateTime.class;

            // --- Boolean / bit ---
            case Types.BOOLEAN:
            case Types.BIT:
                return Boolean.class;

            // --- Binary / blob ---
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return byte[].class;

            // --- Less common types ---
            case Types.ARRAY:
                return Object[].class;

            case Types.JAVA_OBJECT:
            case Types.OTHER:
            case Types.REF:
            case Types.REF_CURSOR:
            case Types.DISTINCT:
            case Types.STRUCT:
            case Types.ROWID:
            case Types.SQLXML:
            case Types.DATALINK:
            case Types.NULL:
                return Object.class;

            default:
                // For unknown/unsupported types
                return Object.class;
        }
    }

    /**
     * Return a dummy/default value for the given SQL type.
     * Used for safe PreparedStatement parameter binding and metadata extraction.
     *
     * @param sqlType JDBC type
     * @return dummy Java value
     */
    public static Object getDummyValueForSqlType(int sqlType) {
        return getDummyValue(sqlTypeToJavaClass(sqlType));
    }

    /**
     * Return a dummy/default value for the given Java type.
     *
     * @param javaType Java class
     * @return dummy value
     */
    public static Object getDummyValue(Class<?> javaType) {
        if (javaType == null || javaType == Object.class)
            return null;

        // Strings
        if (javaType == String.class)
            return "DUMMY";

        // Numbers
        if (javaType == Integer.class || javaType == int.class)
            return 0;
        if (javaType == Long.class || javaType == long.class)
            return 0L;
        if (javaType == Float.class || javaType == float.class)
            return 0f;
        if (javaType == Double.class || javaType == double.class)
            return 0d;
        if (javaType == BigDecimal.class)
            return BigDecimal.ZERO;

        // Boolean
        if (javaType == Boolean.class || javaType == boolean.class)
            return false;

        // Temporal
        if (javaType == LocalDate.class)
            return LocalDate.now();
        if (javaType == LocalDateTime.class)
            return LocalDateTime.now();
        if (javaType == LocalTime.class)
            return LocalTime.now();

        // Binary
        if (javaType == byte[].class)
            return new byte[0];

        // Arrays
        if (javaType == Object[].class)
            return new Object[0];

        // Fallback
        return null;
    }

    // --- Bind dummy parameter directly ---
    public static void setDummyParameter(PreparedStatement ps, int index, int sqlType) throws SQLException {
        Class<?> javaType = sqlTypeToJavaClass(sqlType);
        Object dummyValue = getDummyValue(javaType);

        if (dummyValue == null) {
            ps.setObject(index, null);
            return;
        }

        if (dummyValue instanceof String) {
            ps.setString(index, (String) dummyValue);
        } else if (dummyValue instanceof Integer) {
            ps.setInt(index, (Integer) dummyValue);
        } else if (dummyValue instanceof Long) {
            ps.setLong(index, (Long) dummyValue);
        } else if (dummyValue instanceof BigDecimal) {
            ps.setBigDecimal(index, (BigDecimal) dummyValue);
        } else if (dummyValue instanceof Float) {
            ps.setFloat(index, (Float) dummyValue);
        } else if (dummyValue instanceof Double) {
            ps.setDouble(index, (Double) dummyValue);
        } else if (dummyValue instanceof Boolean) {
            ps.setBoolean(index, (Boolean) dummyValue);
        } else if (dummyValue instanceof LocalDate) {
            ps.setDate(index, java.sql.Date.valueOf((LocalDate) dummyValue));
        } else if (dummyValue instanceof LocalDateTime) {
            ps.setTimestamp(index, java.sql.Timestamp.valueOf((LocalDateTime) dummyValue));
        } else if (dummyValue instanceof LocalTime) {
            ps.setTime(index, java.sql.Time.valueOf((LocalTime) dummyValue));
        } else if (dummyValue instanceof byte[]) {
            ps.setBytes(index, (byte[]) dummyValue);
        } else {
            ps.setObject(index, dummyValue);
        }
    }
}
