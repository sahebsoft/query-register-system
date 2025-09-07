package com.balsam.oasis.common.registry.util;

import com.balsam.oasis.common.registry.exception.QueryValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test coverage for TypeConverter utility class.
 * Tests all supported type conversions, edge cases, and error scenarios.
 */
class TypeConverterTest {

    // ========== NULL HANDLING TESTS ==========

    @Test
    void testConvert_NullValue_ReturnsNull() {
        assertThat(TypeConverter.convert(null, String.class)).isNull();
        assertThat(TypeConverter.convert(null, Integer.class)).isNull();
        assertThat(TypeConverter.convert(null, LocalDate.class)).isNull();
    }

    @Test
    void testConvertString_NullValue_ReturnsNull() {
        assertThat(TypeConverter.convertString(null, String.class)).isNull();
        assertThat(TypeConverter.convertString(null, Integer.class)).isNull();
    }

    // ========== SAME TYPE CONVERSION TESTS ==========

    @Test
    void testConvert_SameType_ReturnsSameInstance() {
        String value = "test";
        Integer intValue = 123;
        LocalDate dateValue = LocalDate.now();

        assertThat(TypeConverter.convert(value, String.class)).isSameAs(value);
        assertThat(TypeConverter.convert(intValue, Integer.class)).isSameAs(intValue);
        assertThat(TypeConverter.convert(dateValue, LocalDate.class)).isSameAs(dateValue);
    }

    // ========== STRING TO PRIMITIVE TYPE CONVERSIONS ==========

    @ParameterizedTest
    @CsvSource({
        "123, 123",
        "-456, -456",
        "0, 0",
        "2147483647, 2147483647",
        "-2147483648, -2147483648"
    })
    void testConvert_StringToInteger_Success(String input, int expected) {
        assertThat(TypeConverter.convert(input, Integer.class)).isEqualTo(expected);
        assertThat(TypeConverter.convert(input, int.class)).isEqualTo(expected);
        assertThat(TypeConverter.convertString(input, Integer.class)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12.5", "12L", "2147483648", "-2147483649"})
    void testConvert_StringToInteger_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, Integer.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Invalid Integer value");
    }

    @ParameterizedTest
    @CsvSource({
        "123456789012345, 123456789012345",
        "-123456789012345, -123456789012345",
        "0, 0",
        "9223372036854775807, 9223372036854775807",
        "-9223372036854775808, -9223372036854775808"
    })
    void testConvert_StringToLong_Success(String input, long expected) {
        assertThat(TypeConverter.convert(input, Long.class)).isEqualTo(expected);
        assertThat(TypeConverter.convert(input, long.class)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12.5", "9223372036854775808", "-9223372036854775809"})
    void testConvert_StringToLong_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, Long.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Invalid Long value");
    }

    @ParameterizedTest
    @CsvSource({
        "123.45, 123.45",
        "-123.45, -123.45",
        "0.0, 0.0",
        "1.7976931348623157E308, 1.7976931348623157E308",
        "4.9E-324, 4.9E-324"
    })
    void testConvert_StringToDouble_Success(String input, double expected) {
        assertThat(TypeConverter.convert(input, Double.class)).isEqualTo(expected);
        assertThat(TypeConverter.convert(input, double.class)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "not_a_number"})
    void testConvert_StringToDouble_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, Double.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Invalid Double value");
    }

    @ParameterizedTest
    @CsvSource({
        "123.45, 123.45",
        "-123.45, -123.45",
        "0.0, 0.0",
        "3.4028235E38, 3.4028235E38"
    })
    void testConvert_StringToFloat_Success(String input, float expected) {
        assertThat(TypeConverter.convert(input, Float.class)).isEqualTo(expected);
        assertThat(TypeConverter.convert(input, float.class)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "not_a_number"})
    void testConvert_StringToFloat_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, Float.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Invalid Float value");
    }

    @ParameterizedTest
    @CsvSource({
        "true, true",
        "True, true", 
        "TRUE, true",
        "1, true",
        "false, false",
        "False, false",
        "FALSE, false",
        "0, false"
    })
    void testConvert_StringToBoolean_Success(String input, boolean expected) {
        assertThat(TypeConverter.convert(input, Boolean.class)).isEqualTo(expected);
        assertThat(TypeConverter.convert(input, boolean.class)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "yes", "no", "2", "-1"})
    void testConvert_StringToBoolean_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, Boolean.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Invalid Boolean value")
            .hasMessageContaining("Expected: true, false, 1, or 0");
    }

    @ParameterizedTest
    @CsvSource({
        "123.456, 123.456",
        "-123.456, -123.456",
        "0, 0",
        "999999999999999999999.999999999999999999, 999999999999999999999.999999999999999999"
    })
    void testConvert_StringToBigDecimal_Success(String input, String expected) {
        BigDecimal result = TypeConverter.convert(input, BigDecimal.class);
        assertThat(result).isEqualTo(new BigDecimal(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "not_a_number"})
    void testConvert_StringToBigDecimal_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, BigDecimal.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Invalid BigDecimal value");
    }

    // ========== DATE/TIME CONVERSIONS ==========

    @Test
    void testConvert_StringToLocalDate_Success() {
        LocalDate expected = LocalDate.of(2023, 12, 25);
        LocalDate result = TypeConverter.convert("2023-12-25", LocalDate.class);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023/12/25", "25-12-2023", "invalid", "2023-13-01", "2023-02-30"})
    void testConvert_StringToLocalDate_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, LocalDate.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Cannot convert")
            .hasMessageContaining("to LocalDate")
            .hasMessageContaining("Expected: YYYY-MM-DD");
    }

    @Test
    void testConvert_StringToLocalDateTime_Success() {
        LocalDateTime expected = LocalDateTime.of(2023, 12, 25, 14, 30, 45);
        LocalDateTime result = TypeConverter.convert("2023-12-25T14:30:45", LocalDateTime.class);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-12-25 14:30:45", "invalid", "2023-12-25"})
    void testConvert_StringToLocalDateTime_Failure(String input) {
        assertThatThrownBy(() -> TypeConverter.convert(input, LocalDateTime.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Cannot convert")
            .hasMessageContaining("to LocalDateTime");
    }

    // ========== EMPTY STRING HANDLING ==========

    @Test
    void testConvert_EmptyString_ForNonStringTypes_ReturnsNull() {
        assertThat(TypeConverter.convert("", Integer.class)).isNull();
        assertThat(TypeConverter.convert("", Long.class)).isNull();
        assertThat(TypeConverter.convert("", Double.class)).isNull();
        assertThat(TypeConverter.convert("", Boolean.class)).isNull();
        assertThat(TypeConverter.convert("", BigDecimal.class)).isNull();
        assertThat(TypeConverter.convert("", LocalDate.class)).isNull();
        assertThat(TypeConverter.convert("", LocalDateTime.class)).isNull();
    }

    @Test
    void testConvert_EmptyString_ForStringType_ReturnsEmptyString() {
        assertThat(TypeConverter.convert("", String.class)).isEqualTo("");
    }

    @Test
    void testConvertString_EmptyString_ForNonStringTypes_ReturnsNull() {
        assertThat(TypeConverter.convertString("", Integer.class)).isNull();
        assertThat(TypeConverter.convertString("", LocalDate.class)).isNull();
    }

    @Test
    void testConvertString_EmptyString_ForStringType_ReturnsEmptyString() {
        assertThat(TypeConverter.convertString("", String.class)).isEqualTo("");
    }

    // ========== NUMBER TO NUMBER CONVERSIONS ==========

    @Test
    void testConvert_NumberToInteger() {
        assertThat(TypeConverter.convert(123L, Integer.class)).isEqualTo(123);
        assertThat(TypeConverter.convert(123.0, Integer.class)).isEqualTo(123);
        assertThat(TypeConverter.convert(123.9, Integer.class)).isEqualTo(123);
        assertThat(TypeConverter.convert(123.0f, Integer.class)).isEqualTo(123);
    }

    @Test
    void testConvert_NumberToLong() {
        assertThat(TypeConverter.convert(123, Long.class)).isEqualTo(123L);
        assertThat(TypeConverter.convert(123.0, Long.class)).isEqualTo(123L);
        assertThat(TypeConverter.convert(123.9, Long.class)).isEqualTo(123L);
    }

    @Test
    void testConvert_NumberToDouble() {
        assertThat(TypeConverter.convert(123, Double.class)).isEqualTo(123.0);
        assertThat(TypeConverter.convert(123L, Double.class)).isEqualTo(123.0);
        assertThat(TypeConverter.convert(123.5f, Double.class)).isCloseTo(123.5, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void testConvert_NumberToFloat() {
        assertThat(TypeConverter.convert(123, Float.class)).isEqualTo(123.0f);
        assertThat(TypeConverter.convert(123L, Float.class)).isEqualTo(123.0f);
        assertThat(TypeConverter.convert(123.5, Float.class)).isEqualTo(123.5f);
    }

    @Test
    void testConvert_NumberToBigDecimal() {
        assertThat(TypeConverter.convert(123, BigDecimal.class)).isEqualTo(new BigDecimal("123"));
        assertThat(TypeConverter.convert(123L, BigDecimal.class)).isEqualTo(new BigDecimal("123"));
        assertThat(TypeConverter.convert(123.45, BigDecimal.class)).isEqualTo(new BigDecimal("123.45"));
    }

    @Test
    void testConvert_NumberToPrimitiveTypes() {
        assertThat(TypeConverter.convert(123L, int.class)).isEqualTo(123);
        assertThat(TypeConverter.convert(123, long.class)).isEqualTo(123L);
        assertThat(TypeConverter.convert(123, double.class)).isEqualTo(123.0);
        assertThat(TypeConverter.convert(123, float.class)).isEqualTo(123.0f);
    }

    // ========== DATE/TIME SPECIAL CONVERSIONS ==========

    @Test
    void testConvert_SqlDateToLocalDate() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-12-25");
        LocalDate result = TypeConverter.convert(sqlDate, LocalDate.class);
        assertThat(result).isEqualTo(LocalDate.of(2023, 12, 25));
    }

    @Test
    void testConvert_LocalDateTimeToLocalDate() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45);
        LocalDate result = TypeConverter.convert(dateTime, LocalDate.class);
        assertThat(result).isEqualTo(LocalDate.of(2023, 12, 25));
    }

    @Test
    void testConvert_SqlTimestampToLocalDateTime() {
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 14:30:45");
        LocalDateTime result = TypeConverter.convert(timestamp, LocalDateTime.class);
        assertThat(result).isEqualTo(LocalDateTime.of(2023, 12, 25, 14, 30, 45));
    }

    @Test
    void testConvert_LocalDateToLocalDateTime() {
        LocalDate date = LocalDate.of(2023, 12, 25);
        LocalDateTime result = TypeConverter.convert(date, LocalDateTime.class);
        assertThat(result).isEqualTo(LocalDateTime.of(2023, 12, 25, 0, 0, 0));
    }

    // ========== ANY TYPE TO STRING CONVERSION ==========

    @Test
    void testConvert_AnyTypeToString() {
        assertThat(TypeConverter.convert(123, String.class)).isEqualTo("123");
        assertThat(TypeConverter.convert(123L, String.class)).isEqualTo("123");
        assertThat(TypeConverter.convert(123.45, String.class)).isEqualTo("123.45");
        assertThat(TypeConverter.convert(true, String.class)).isEqualTo("true");
        assertThat(TypeConverter.convert(LocalDate.of(2023, 12, 25), String.class)).isEqualTo("2023-12-25");
        
        // Custom objects use toString()
        Object customObject = new Object() {
            @Override
            public String toString() {
                return "custom_string";
            }
        };
        assertThat(TypeConverter.convert(customObject, String.class)).isEqualTo("custom_string");
    }

    // ========== ERROR CASES ==========

    @Test
    void testConvert_UnsupportedConversion_ThrowsException() {
        // Try to convert string to unsupported type
        assertThatThrownBy(() -> TypeConverter.convert("test", java.util.Date.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Cannot convert value of type String to Date");
    }

    @Test
    void testConvert_UnsupportedNumberConversion_ThrowsException() {
        assertThatThrownBy(() -> TypeConverter.convert(123, java.util.Date.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Cannot convert value of type Integer to Date");
    }

    @Test
    void testConvert_NumberToUnsupportedNumberType_ThrowsException() {
        // Try to convert Number to unsupported Number subtype
        assertThatThrownBy(() -> TypeConverter.convert(123, java.math.BigInteger.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("Cannot convert Number to BigInteger");
    }

    @Test
    void testConvertString_UnsupportedType_ThrowsException() {
        assertThatThrownBy(() -> TypeConverter.convertString("test", java.util.Date.class))
            .isInstanceOf(QueryValidationException.class)
            .hasMessageContaining("No converter available for type: Date");
    }

    // ========== STRING TYPE IDENTITY CONVERSION ==========

    @Test
    void testConvert_StringToString_ReturnsSameInstance() {
        String original = "test";
        String result = TypeConverter.convert(original, String.class);
        assertThat(result).isSameAs(original);
    }

    @Test
    void testConvertString_StringToString_ReturnsSameInstance() {
        String original = "test";
        String result = TypeConverter.convertString(original, String.class);
        assertThat(result).isSameAs(original);
    }
}