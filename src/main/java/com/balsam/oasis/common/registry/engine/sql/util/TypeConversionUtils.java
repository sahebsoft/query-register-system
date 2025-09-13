package com.balsam.oasis.common.registry.engine.sql.util;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consolidated type conversion utilities.
 * Combines functionality from JavaTypeConverter, SqlTypeMapper, and SqlTypeExtractor.
 */
public class TypeConversionUtils {
    
    private static final Logger log = LoggerFactory.getLogger(TypeConversionUtils.class);
    
    // SQL type to Java class mapping
    private static final Map<Integer, Class<?>> SQL_TYPE_MAP = new HashMap<>();
    private static final Map<String, Class<?>> TYPE_NAME_MAP = new HashMap<>();
    
    static {
        // Numeric types
        SQL_TYPE_MAP.put(Types.TINYINT, Byte.class);
        SQL_TYPE_MAP.put(Types.SMALLINT, Short.class);
        SQL_TYPE_MAP.put(Types.INTEGER, Integer.class);
        SQL_TYPE_MAP.put(Types.BIGINT, Long.class);
        SQL_TYPE_MAP.put(Types.FLOAT, Float.class);
        SQL_TYPE_MAP.put(Types.REAL, Float.class);
        SQL_TYPE_MAP.put(Types.DOUBLE, Double.class);
        SQL_TYPE_MAP.put(Types.NUMERIC, BigDecimal.class);
        SQL_TYPE_MAP.put(Types.DECIMAL, BigDecimal.class);
        
        // String types
        SQL_TYPE_MAP.put(Types.CHAR, String.class);
        SQL_TYPE_MAP.put(Types.VARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.LONGVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.NCHAR, String.class);
        SQL_TYPE_MAP.put(Types.NVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.LONGNVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.CLOB, String.class);
        SQL_TYPE_MAP.put(Types.NCLOB, String.class);
        
        // Date/Time types
        SQL_TYPE_MAP.put(Types.DATE, Date.class);
        SQL_TYPE_MAP.put(Types.TIME, Time.class);
        SQL_TYPE_MAP.put(Types.TIMESTAMP, Timestamp.class);
        SQL_TYPE_MAP.put(Types.TIME_WITH_TIMEZONE, Time.class);
        SQL_TYPE_MAP.put(Types.TIMESTAMP_WITH_TIMEZONE, Timestamp.class);
        
        // Boolean type
        SQL_TYPE_MAP.put(Types.BOOLEAN, Boolean.class);
        SQL_TYPE_MAP.put(Types.BIT, Boolean.class);
        
        // Binary types
        SQL_TYPE_MAP.put(Types.BINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.VARBINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.LONGVARBINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.BLOB, byte[].class);
        
        // Type name mapping
        TYPE_NAME_MAP.put("NUMBER", BigDecimal.class);
        TYPE_NAME_MAP.put("VARCHAR2", String.class);
        TYPE_NAME_MAP.put("CHAR", String.class);
        TYPE_NAME_MAP.put("DATE", Timestamp.class);
        TYPE_NAME_MAP.put("TIMESTAMP", Timestamp.class);
        TYPE_NAME_MAP.put("CLOB", String.class);
        TYPE_NAME_MAP.put("BLOB", byte[].class);
    }
    
    /**
     * Get Java class for SQL type
     */
    public static Class<?> getJavaType(int sqlType) {
        Class<?> javaType = SQL_TYPE_MAP.get(sqlType);
        if (javaType == null) {
            log.warn("Unknown SQL type: {}, defaulting to Object", sqlType);
            return Object.class;
        }
        return javaType;
    }
    
    /**
     * Get Java class for SQL type name
     */
    public static Class<?> getJavaType(String typeName) {
        if (typeName == null) {
            return Object.class;
        }
        
        String upperTypeName = typeName.toUpperCase();
        Class<?> javaType = TYPE_NAME_MAP.get(upperTypeName);
        
        if (javaType == null) {
            // Try standard JDBC type
            try {
                JDBCType jdbcType = JDBCType.valueOf(upperTypeName);
                javaType = getJavaType(jdbcType.getVendorTypeNumber());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown type name: {}, defaulting to Object", typeName);
                return Object.class;
            }
        }
        
        return javaType;
    }
    
    /**
     * Convert value to target type
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        // String conversions
        if (targetType == String.class) {
            return (T) value.toString();
        }
        
        // Numeric conversions
        if (Number.class.isAssignableFrom(targetType)) {
            return convertToNumber(value, targetType);
        }
        
        // Boolean conversions
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) convertToBoolean(value);
        }
        
        // Date/Time conversions
        if (Date.class.isAssignableFrom(targetType)) {
            return convertToDate(value, targetType);
        }
        
        if (LocalDate.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalDate(value);
        }
        
        if (LocalDateTime.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalDateTime(value);
        }
        
        if (LocalTime.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalTime(value);
        }
        
        // Default: try casting
        try {
            return targetType.cast(value);
        } catch (ClassCastException e) {
            log.warn("Cannot convert {} to {}", value.getClass(), targetType);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T convertToNumber(Object value, Class<T> targetType) {
        Number number;
        
        if (value instanceof Number) {
            number = (Number) value;
        } else if (value instanceof String) {
            try {
                number = new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
        
        if (targetType == Byte.class || targetType == byte.class) {
            return (T) Byte.valueOf(number.byteValue());
        } else if (targetType == Short.class || targetType == short.class) {
            return (T) Short.valueOf(number.shortValue());
        } else if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(number.intValue());
        } else if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(number.longValue());
        } else if (targetType == Float.class || targetType == float.class) {
            return (T) Float.valueOf(number.floatValue());
        } else if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(number.doubleValue());
        } else if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return (T) value;
            }
            return (T) new BigDecimal(number.toString());
        }
        
        return null;
    }
    
    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value instanceof String) {
            String str = value.toString().toLowerCase();
            return "true".equals(str) || "yes".equals(str) || "y".equals(str) || "1".equals(str);
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T convertToDate(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        
        long millis;
        
        if (value instanceof java.util.Date) {
            millis = ((java.util.Date) value).getTime();
        } else if (value instanceof Number) {
            millis = ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                millis = Timestamp.valueOf(value.toString()).getTime();
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
        
        if (targetType == Date.class) {
            return (T) new Date(millis);
        } else if (targetType == Time.class) {
            return (T) new Time(millis);
        } else if (targetType == Timestamp.class) {
            return (T) new Timestamp(millis);
        } else if (targetType == java.util.Date.class) {
            return (T) new java.util.Date(millis);
        }
        
        return null;
    }
    
    private static LocalDate convertToLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        } else if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalDate();
        } else if (value instanceof String) {
            return LocalDate.parse(value.toString());
        }
        return null;
    }
    
    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        } else if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime()).toLocalDateTime();
        } else if (value instanceof String) {
            return LocalDateTime.parse(value.toString());
        }
        return null;
    }
    
    private static LocalTime convertToLocalTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else if (value instanceof Time) {
            return ((Time) value).toLocalTime();
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalTime();
        } else if (value instanceof String) {
            return LocalTime.parse(value.toString());
        }
        return null;
    }
    
    /**
     * Extract Java type from ResultSetMetaData
     */
    public static Class<?> extractJavaType(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        int sqlType = metaData.getColumnType(columnIndex);
        String typeName = metaData.getColumnTypeName(columnIndex);
        
        // Try type name first (more specific for Oracle types)
        Class<?> javaType = getJavaType(typeName);
        if (javaType != Object.class) {
            return javaType;
        }
        
        // Fall back to SQL type
        return getJavaType(sqlType);
    }
    
    /**
     * Get value from ResultSet with type conversion
     */
    public static Object getResultSetValue(ResultSet rs, int columnIndex, Class<?> targetType) throws SQLException {
        Object value = rs.getObject(columnIndex);
        
        if (value == null || rs.wasNull()) {
            return null;
        }
        
        if (targetType != null && targetType != Object.class) {
            return convertValue(value, targetType);
        }
        
        return value;
    }
    
    /**
     * Get SQL type name for Java class
     */
    public static String getSqlTypeName(Class<?> javaType) {
        if (javaType == String.class) {
            return "VARCHAR";
        } else if (javaType == Integer.class || javaType == int.class) {
            return "INTEGER";
        } else if (javaType == Long.class || javaType == long.class) {
            return "BIGINT";
        } else if (javaType == BigDecimal.class) {
            return "NUMERIC";
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return "BOOLEAN";
        } else if (javaType == Date.class || javaType == Timestamp.class) {
            return "TIMESTAMP";
        } else if (javaType == byte[].class) {
            return "BLOB";
        }
        
        return "VARCHAR";
    }
    
    /**
     * Get dummy value for testing/metadata extraction
     */
    public static Object getDummyValue(Class<?> javaType) {
        if (javaType == null || javaType == Object.class) {
            return null;
        }
        
        // Strings
        if (javaType == String.class) {
            return "DUMMY";
        }
        
        // Numbers
        if (javaType == Integer.class || javaType == int.class) {
            return 0;
        }
        if (javaType == Long.class || javaType == long.class) {
            return 0L;
        }
        if (javaType == BigDecimal.class) {
            return BigDecimal.ZERO;
        }
        if (javaType == Double.class || javaType == double.class) {
            return 0.0;
        }
        if (javaType == Float.class || javaType == float.class) {
            return 0.0f;
        }
        if (javaType == Short.class || javaType == short.class) {
            return (short) 0;
        }
        if (javaType == Byte.class || javaType == byte.class) {
            return (byte) 0;
        }
        
        // Boolean
        if (javaType == Boolean.class || javaType == boolean.class) {
            return false;
        }
        
        // Date/Time
        if (javaType == Date.class) {
            return new Date(0L);
        }
        if (javaType == Timestamp.class) {
            return new Timestamp(0L);
        }
        if (javaType == Time.class) {
            return new Time(0L);
        }
        if (javaType == LocalDate.class) {
            return LocalDate.of(2000, 1, 1);
        }
        if (javaType == LocalDateTime.class) {
            return LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        if (javaType == LocalTime.class) {
            return LocalTime.of(0, 0);
        }
        
        // Binary
        if (javaType == byte[].class) {
            return new byte[0];
        }
        
        return null;
    }
    
    /**
     * Set dummy parameter in PreparedStatement for metadata extraction
     */
    public static void setDummyParameter(java.sql.PreparedStatement ps, int index, int sqlType) 
            throws SQLException {
        Class<?> javaType = getJavaType(sqlType);
        Object dummyValue = getDummyValue(javaType);
        
        if (dummyValue == null) {
            ps.setObject(index, null);
        } else {
            ps.setObject(index, dummyValue);
        }
    }
    
    /**
     * Extract value from ResultSet by column index
     */
    public static Object extractValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        return getResultSetValue(rs, columnIndex, getJavaType(sqlType));
    }
    
    /**
     * Extract value from ResultSet by column name
     */
    public static Object extractValue(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName);
    }
}