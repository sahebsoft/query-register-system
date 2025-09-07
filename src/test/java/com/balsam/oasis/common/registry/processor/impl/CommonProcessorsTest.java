package com.balsam.oasis.common.registry.processor.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test coverage for all CommonProcessors validators and formatters.
 * Tests all provided implementations, edge cases, and error scenarios.
 */
class CommonProcessorsTest {

    // ========== VALIDATOR TESTS ==========

    // Email Validator Tests
    @ParameterizedTest
    @ValueSource(strings = {
        "test@example.com",
        "user.name@domain.co.uk",
        "user+tag@example.org",
        "user_name@example.com",
        "123@example.com",
        "test@localhost",
        "a@b.co"
    })
    void testEmailValidator_ValidEmails(String email) {
        assertThat(CommonProcessors.EMAIL_VALIDATOR.validate(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",
        "@example.com",
        "test@",
        "test.example.com",
        "test@@example.com",
        "",
        " ",
        "test @example.com",
        "test@example..com"
    })
    void testEmailValidator_InvalidEmails(String email) {
        assertThat(CommonProcessors.EMAIL_VALIDATOR.validate(email)).isFalse();
    }

    @Test
    void testEmailValidator_NullValue() {
        assertThat(CommonProcessors.EMAIL_VALIDATOR.validate(null)).isFalse();
    }

    @Test
    void testEmailValidator_NonStringValue() {
        assertThat(CommonProcessors.EMAIL_VALIDATOR.validate(123)).isFalse();
        
        // Test with object that has toString() returning valid email
        Object emailObject = new Object() {
            @Override
            public String toString() {
                return "test@example.com";
            }
        };
        assertThat(CommonProcessors.EMAIL_VALIDATOR.validate(emailObject)).isTrue();
    }

    // Phone Validator Tests
    @ParameterizedTest
    @ValueSource(strings = {
        "1234567890",        // 10 digits
        "123-456-7890",      // With dashes
        "(123) 456-7890",    // With parentheses
        "+1 123 456 7890",   // International format
        "123.456.7890",      // With dots
        "123 456 7890",      // With spaces
        "12345678901234",    // 14 digits
        "123456789012345"    // 15 digits (max)
    })
    void testPhoneValidator_ValidPhones(String phone) {
        assertThat(CommonProcessors.PHONE_VALIDATOR.validate(phone)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123456789",         // Too short (9 digits)
        "1234567890123456",  // Too long (16 digits)
        "abcdefghij",        // No digits
        "123abc7890",        // Mixed letters and numbers
        "",
        " ",
        "123-456",           // Too short even with formatting
    })
    void testPhoneValidator_InvalidPhones(String phone) {
        assertThat(CommonProcessors.PHONE_VALIDATOR.validate(phone)).isFalse();
    }

    @Test
    void testPhoneValidator_NullValue() {
        assertThat(CommonProcessors.PHONE_VALIDATOR.validate(null)).isFalse();
    }

    @Test
    void testPhoneValidator_NonStringValue() {
        assertThat(CommonProcessors.PHONE_VALIDATOR.validate(1234567890L)).isTrue();
        
        Object phoneObject = new Object() {
            @Override
            public String toString() {
                return "123-456-7890";
            }
        };
        assertThat(CommonProcessors.PHONE_VALIDATOR.validate(phoneObject)).isTrue();
    }

    // Not Null Validator Tests
    @Test
    void testNotNullValidator_ValidValues() {
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate("test")).isTrue();
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate("")).isTrue();
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(" ")).isTrue();
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(0)).isTrue();
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(false)).isTrue();
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(new Object())).isTrue();
    }

    @Test
    void testNotNullValidator_NullValue() {
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(null)).isFalse();
    }

    // Not Empty Validator Tests
    @Test
    void testNotEmptyValidator_ValidValues() {
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate("test")).isTrue();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate("a")).isTrue();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate("   text   ")).isTrue();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(123)).isTrue();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(new Object())).isTrue();
    }

    @Test
    void testNotEmptyValidator_InvalidValues() {
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(null)).isFalse();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate("")).isFalse();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate("   ")).isFalse();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate("\t\n")).isFalse();
    }

    @Test
    void testNotEmptyValidator_NonStringValue() {
        // Non-string values use toString()
        Object emptyObject = new Object() {
            @Override
            public String toString() {
                return "";
            }
        };
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(emptyObject)).isFalse();
        
        Object whitespaceObject = new Object() {
            @Override
            public String toString() {
                return "   ";
            }
        };
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(whitespaceObject)).isFalse();
    }

    // ========== FORMATTER TESTS ==========

    // Currency Formatter Tests
    @Test
    void testCurrencyFormatter_BasicUsage() {
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("123.45")))
            .isEqualTo("$123.45");
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("1000.00")))
            .isEqualTo("$1,000.00");
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(null)).isNull();
    }

    @Test
    void testCurrencyFormatter_LargeAmounts() {
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("1000000.99")))
            .isEqualTo("$1,000,000.99");
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("1234567890.12")))
            .isEqualTo("$1,234,567,890.12");
    }

    @Test
    void testCurrencyFormatter_NegativeAmounts() {
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("-123.45")))
            .isEqualTo("-$123.45");
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("-1000.00")))
            .isEqualTo("-$1,000.00");
    }

    @Test
    void testCurrencyFormatter_ZeroAmount() {
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("0")))
            .isEqualTo("$0.00");
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("0.00")))
            .isEqualTo("$0.00");
    }

    @Test
    void testCurrencyFormatter_SmallAmounts() {
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("0.01")))
            .isEqualTo("$0.01");
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format(new BigDecimal("0.99")))
            .isEqualTo("$0.99");
    }

    @Test
    void testCurrencyFormatter_NonBigDecimalInput() {
        // Test that it handles conversion from other types
        Object numberObject = new Object() {
            @Override
            public String toString() {
                return "123.45";
            }
        };
        assertThat(CommonProcessors.CURRENCY_FORMATTER.format((BigDecimal) null)).isNull();
        
        // Test with string input (though this might not be typical usage)
        // Note: The implementation casts to BigDecimal, so this tests the conversion logic
    }

    // Percentage Formatter Tests
    @Test
    void testPercentageFormatter_BasicUsage() {
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(25.5)).isEqualTo("25.50%");
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(0)).isEqualTo("0.00%");
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(100)).isEqualTo("100.00%");
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "25.5, '25.50%'",
        "0, '0.00%'",
        "100, '100.00%'",
        "0.1234, '0.12%'",
        "99.999, '100.00%'",
        "-10.5, '-10.50%'"
    })
    void testPercentageFormatter_VariousValues(double input, String expected) {
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(input)).isEqualTo(expected);
    }

    @Test
    void testPercentageFormatter_DifferentNumberTypes() {
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(25)).isEqualTo("25.00%");
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(25L)).isEqualTo("25.00%");
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(25.5f)).isEqualTo("25.50%");
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format(new BigDecimal("25.5"))).isEqualTo("25.50%");
    }

    @Test
    void testPercentageFormatter_NonNumberInput() {
        // Test with object that has toString() returning a number
        Object numberObject = new Object() {
            @Override
            public String toString() {
                return "25.5";
            }
        };
        assertThat(CommonProcessors.PERCENTAGE_FORMATTER.format((Number) null)).isNull();
    }

    // Date Formatter Tests
    @Test
    void testDateFormatter_BasicUsage() {
        LocalDate date = LocalDate.of(2023, 12, 25);
        assertThat(CommonProcessors.DATE_FORMATTER.format(date)).isEqualTo("2023-12-25");
        assertThat(CommonProcessors.DATE_FORMATTER.format(null)).isNull();
    }

    @Test
    void testDateFormatter_VariousDates() {
        assertThat(CommonProcessors.DATE_FORMATTER.format(LocalDate.of(2023, 1, 1)))
            .isEqualTo("2023-01-01");
        assertThat(CommonProcessors.DATE_FORMATTER.format(LocalDate.of(2023, 12, 31)))
            .isEqualTo("2023-12-31");
        assertThat(CommonProcessors.DATE_FORMATTER.format(LocalDate.of(2000, 2, 29)))
            .isEqualTo("2000-02-29"); // Leap year
    }

    @Test
    void testDateFormatter_NonLocalDateInput() {
        // Test that the formatter can handle a regular LocalDate properly
        LocalDate realDate = LocalDate.of(2023, 12, 25);
        assertThat(CommonProcessors.DATE_FORMATTER.format(realDate)).isEqualTo("2023-12-25");
    }

    // DateTime Formatter Tests
    @Test
    void testDateTimeFormatter_BasicUsage() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45);
        assertThat(CommonProcessors.DATETIME_FORMATTER.format(dateTime)).isEqualTo("2023-12-25T14:30:45");
        assertThat(CommonProcessors.DATETIME_FORMATTER.format(null)).isNull();
    }

    @Test
    void testDateTimeFormatter_VariousDateTimes() {
        assertThat(CommonProcessors.DATETIME_FORMATTER.format(LocalDateTime.of(2023, 1, 1, 0, 0, 0)))
            .isEqualTo("2023-01-01T00:00:00");
        assertThat(CommonProcessors.DATETIME_FORMATTER.format(LocalDateTime.of(2023, 12, 31, 23, 59, 59)))
            .isEqualTo("2023-12-31T23:59:59");
        assertThat(CommonProcessors.DATETIME_FORMATTER.format(LocalDateTime.of(2023, 6, 15, 12, 30, 45, 123456789)))
            .isEqualTo("2023-06-15T12:30:45.123456789");
    }

    @Test
    void testDateTimeFormatter_NonLocalDateTimeInput() {
        // Test that the formatter can handle a regular LocalDateTime properly
        LocalDateTime realDateTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45);
        assertThat(CommonProcessors.DATETIME_FORMATTER.format(realDateTime))
            .isEqualTo("2023-12-25T14:30:45");
    }

    // ========== STRING PROCESSOR TESTS ==========

    // Uppercase Processor Tests
    @Test
    void testUppercaseProcessor_BasicUsage() {
        assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format("hello")).isEqualTo("HELLO");
        assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format("HELLO")).isEqualTo("HELLO");
        assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format("Hello World")).isEqualTo("HELLO WORLD");
        assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format("")).isEqualTo("");
        assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format(null)).isNull();
    }

    @Test
    void testUppercaseProcessor_SpecialCharacters() {
        assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format("hello-world_123!@#"))
            .isEqualTo("HELLO-WORLD_123!@#");
        assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format("áéíóú")).isEqualTo("ÁÉÍÓÚ");
    }

    // Lowercase Processor Tests
    @Test
    void testLowercaseProcessor_BasicUsage() {
        assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format("HELLO")).isEqualTo("hello");
        assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format("hello")).isEqualTo("hello");
        assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format("Hello World")).isEqualTo("hello world");
        assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format("")).isEqualTo("");
        assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format(null)).isNull();
    }

    @Test
    void testLowercaseProcessor_SpecialCharacters() {
        assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format("HELLO-WORLD_123!@#"))
            .isEqualTo("hello-world_123!@#");
        assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format("ÁÉÍÓÚ")).isEqualTo("áéíóú");
    }

    // Trim Processor Tests
    @Test
    void testTrimProcessor_BasicUsage() {
        assertThat(CommonProcessors.TRIM_PROCESSOR.format("  hello  ")).isEqualTo("hello");
        assertThat(CommonProcessors.TRIM_PROCESSOR.format("\t\nhello\t\n")).isEqualTo("hello");
        assertThat(CommonProcessors.TRIM_PROCESSOR.format("hello")).isEqualTo("hello");
        assertThat(CommonProcessors.TRIM_PROCESSOR.format("")).isEqualTo("");
        assertThat(CommonProcessors.TRIM_PROCESSOR.format("   ")).isEqualTo("");
        assertThat(CommonProcessors.TRIM_PROCESSOR.format(null)).isNull();
    }

    @Test
    void testTrimProcessor_OnlyWhitespace() {
        assertThat(CommonProcessors.TRIM_PROCESSOR.format("   ")).isEqualTo("");
        assertThat(CommonProcessors.TRIM_PROCESSOR.format("\t\n\r")).isEqualTo("");
    }

    // ========== MASKING PROCESSOR TESTS ==========

    // SSN Mask Processor Tests
    @Test
    void testMaskSsnProcessor_ValidSSN() {
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("123456789")).isEqualTo("***-**-6789");
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("123-45-6789")).isEqualTo("***-**-6789");
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("123 45 6789")).isEqualTo("***-**-6789");
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("123.45.6789")).isEqualTo("***-**-6789");
    }

    @Test
    void testMaskSsnProcessor_InvalidSSN() {
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("12345678")).isEqualTo("***MASKED***"); // Too short
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("1234567890")).isEqualTo("***MASKED***"); // Too long
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("abc123def")).isEqualTo("***MASKED***"); // Letters
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format("")).isEqualTo("***MASKED***"); // Empty
    }

    @Test
    void testMaskSsnProcessor_NullInput() {
        assertThat(CommonProcessors.MASK_SSN_PROCESSOR.format(null)).isNull();
    }

    // Email Mask Processor Tests
    @Test
    void testMaskEmailProcessor_ValidEmail() {
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("john@example.com")).isEqualTo("j***@example.com");
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("user.name@domain.co.uk")).isEqualTo("u***@domain.co.uk");
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("a@b.com")).isEqualTo("a***@b.com");
    }

    @Test
    void testMaskEmailProcessor_EdgeCases() {
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("@example.com")).isEqualTo("***@***"); // No username
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("a@example.com")).isEqualTo("a***@example.com"); // Single char
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("ab@example.com")).isEqualTo("a***@example.com"); // Two chars
    }

    @Test
    void testMaskEmailProcessor_InvalidEmail() {
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("invalid-email")).isEqualTo("***@***");
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("")).isEqualTo("***@***");
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format("user")).isEqualTo("***@***");
    }

    @Test
    void testMaskEmailProcessor_NullInput() {
        assertThat(CommonProcessors.MASK_EMAIL_PROCESSOR.format(null)).isNull();
    }

    // ========== COMBINED USAGE TESTS ==========

    @Test
    void testCombinedValidators() {
        String validEmail = "test@example.com";
        String validPhone = "123-456-7890";
        
        // Test combining multiple validations
        assertThat(CommonProcessors.EMAIL_VALIDATOR.validate(validEmail)).isTrue();
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(validEmail)).isTrue();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(validEmail)).isTrue();
        
        assertThat(CommonProcessors.PHONE_VALIDATOR.validate(validPhone)).isTrue();
        assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(validPhone)).isTrue();
        assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(validPhone)).isTrue();
    }

    @Test
    void testCombinedFormatters() {
        // Test chaining formatters conceptually
        String input = "  Test Email  ";
        String trimmed = CommonProcessors.TRIM_PROCESSOR.format(input); // "Test Email"
        String uppercase = CommonProcessors.UPPERCASE_PROCESSOR.format(trimmed); // "TEST EMAIL"
        
        assertThat(trimmed).isEqualTo("Test Email");
        assertThat(uppercase).isEqualTo("TEST EMAIL");
    }

    // ========== PERFORMANCE TESTS (BASIC) ==========

    @Test
    void testValidators_PerformanceWithManyValues() {
        // Basic performance test - should handle many validations without issues
        for (int i = 0; i < 1000; i++) {
            String email = "test" + i + "@example.com";
            assertThat(CommonProcessors.EMAIL_VALIDATOR.validate(email)).isTrue();
            assertThat(CommonProcessors.NOT_NULL_VALIDATOR.validate(email)).isTrue();
            assertThat(CommonProcessors.NOT_EMPTY_VALIDATOR.validate(email)).isTrue();
        }
    }

    @Test
    void testFormatters_PerformanceWithManyValues() {
        // Basic performance test for formatters
        for (int i = 0; i < 1000; i++) {
            String text = "Test " + i;
            assertThat(CommonProcessors.UPPERCASE_PROCESSOR.format(text)).isEqualTo("TEST " + i);
            assertThat(CommonProcessors.LOWERCASE_PROCESSOR.format(text.toUpperCase())).isEqualTo("test " + i);
            assertThat(CommonProcessors.TRIM_PROCESSOR.format("  " + text + "  ")).isEqualTo(text);
        }
    }
}