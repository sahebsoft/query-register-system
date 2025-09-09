package com.balsam.oasis.common.registry.engine.sql.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.balsam.oasis.common.registry.exception.QueryValidationException;

/**
 * Utility class for type conversions in the Query Registration System.
 * Provides centralized type conversion logic with consistent error handling.
 *
 * @author Query Registration System
 * @since 1.0
 */
public final class TypeConverter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = new HashMap<>();

    static {
        CONVERTERS.put(String.class, s -> s);
        CONVERTERS.put(Integer.class, TypeConverter::toInteger);
        CONVERTERS.put(int.class, TypeConverter::toInteger);
        CONVERTERS.put(Long.class, TypeConverter::toLong);
        CONVERTERS.put(long.class, TypeConverter::toLong);
        CONVERTERS.put(Double.class, TypeConverter::toDouble);
        CONVERTERS.put(double.class, TypeConverter::toDouble);
        CONVERTERS.put(Float.class, TypeConverter::toFloat);
        CONVERTERS.put(float.class, TypeConverter::toFloat);
        CONVERTERS.put(Boolean.class, TypeConverter::toBoolean);
        CONVERTERS.put(boolean.class, TypeConverter::toBoolean);
        CONVERTERS.put(BigDecimal.class, TypeConverter::toBigDecimal);
        CONVERTERS.put(LocalDate.class, TypeConverter::toLocalDate);
        CONVERTERS.put(LocalDateTime.class, TypeConverter::toLocalDateTime);
    }

    private TypeConverter() {
        // Utility class, prevent instantiation
    }

    /**
     * Converts a value to the specified target type.
     *
     * @param value      The value to convert
     * @param targetType The target type class
     * @param <T>        The target type
     * @return The converted value
     * @throws QueryValidationException if conversion fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }

        // Handle String to type conversions
        if (value instanceof String strValue) {

            // Skip empty strings for non-String types
            if (strValue.isEmpty() && !targetType.equals(String.class)) {
                return null;
            }

            Function<String, ?> converter = CONVERTERS.get(targetType);
            if (converter != null) {
                return (T) converter.apply(strValue);
            }
        }

        // Handle numeric conversions (both wrapper and primitive types)
        if ((Number.class.isAssignableFrom(targetType) ||
                targetType == int.class || targetType == long.class ||
                targetType == double.class || targetType == float.class) &&
                value instanceof Number) {
            return convertNumber((Number) value, targetType);
        }

        // Handle java.sql.Date to LocalDate
        if (targetType.equals(LocalDate.class) && value instanceof java.sql.Date) {
            return targetType.cast(((java.sql.Date) value).toLocalDate());
        }

        // Handle LocalDateTime to LocalDate
        if (targetType.equals(LocalDate.class) && value instanceof LocalDateTime) {
            return targetType.cast(((LocalDateTime) value).toLocalDate());
        }

        // Handle java.sql.Timestamp to LocalDateTime
        if (targetType.equals(LocalDateTime.class) && value instanceof java.sql.Timestamp) {
            return targetType.cast(((java.sql.Timestamp) value).toLocalDateTime());
        }

        // Handle LocalDate to LocalDateTime
        if (targetType.equals(LocalDateTime.class) && value instanceof LocalDate) {
            return targetType.cast(((LocalDate) value).atStartOfDay());
        }

        // Default to toString conversion for String target
        if (targetType.equals(String.class)) {
            return targetType.cast(value.toString());
        }

        throw new QueryValidationException(
                String.format("Cannot convert value of type %s to %s",
                        value.getClass().getSimpleName(),
                        targetType.getSimpleName()));
    }

    /**
     * Converts a string value to the specified target type.
     *
     * @param value      The string value to convert
     * @param targetType The target type class
     * @param <T>        The target type
     * @return The converted value
     * @throws QueryValidationException if conversion fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertString(String value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        // Skip empty strings for non-String types
        if (value.isEmpty() && !targetType.equals(String.class)) {
            return null;
        }

        Function<String, ?> converter = CONVERTERS.get(targetType);
        if (converter != null) {
            return (T) converter.apply(value);
        }

        throw new QueryValidationException(
                String.format("No converter available for type: %s", targetType.getSimpleName()));
    }

    private static Integer toInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new QueryValidationException(
                    String.format("Invalid Integer value '%s': %s", value, e.getMessage()));
        }
    }

    private static Long toLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new QueryValidationException(
                    String.format("Invalid Long value '%s': %s", value, e.getMessage()));
        }
    }

    private static Double toDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new QueryValidationException(
                    String.format("Invalid Double value '%s': %s", value, e.getMessage()));
        }
    }

    private static Float toFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new QueryValidationException(
                    String.format("Invalid Float value '%s': %s", value, e.getMessage()));
        }
    }

    private static Boolean toBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return Boolean.FALSE;
        }
        throw new QueryValidationException(
                String.format("Invalid Boolean value '%s'. Expected: true, false, 1, or 0", value));
    }

    private static BigDecimal toBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new QueryValidationException(
                    String.format("Invalid BigDecimal value '%s': %s", value, e.getMessage()));
        }
    }

    private static LocalDate toLocalDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new QueryValidationException(
                    String.format("Cannot convert '%s' to LocalDate: Invalid date format. Expected: YYYY-MM-DD",
                            value));
        }
    }

    private static LocalDateTime toLocalDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new QueryValidationException(
                    String.format("Cannot convert '%s' to LocalDateTime: Invalid datetime format. Expected: ISO format",
                            value));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertNumber(Number number, Class<T> targetType) {
        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return (T) Integer.valueOf(number.intValue());
        } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return (T) Long.valueOf(number.longValue());
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return (T) Double.valueOf(number.doubleValue());
        } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
            return (T) Float.valueOf(number.floatValue());
        } else if (targetType.equals(BigDecimal.class)) {
            return (T) new BigDecimal(number.toString());
        }

        throw new QueryValidationException(
                String.format("Cannot convert Number to %s", targetType.getSimpleName()));
    }
}