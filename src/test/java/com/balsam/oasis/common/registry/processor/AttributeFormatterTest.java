package com.balsam.oasis.common.registry.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test coverage for AttributeFormatter interface and its static factory methods.
 * Tests all provided formatters, custom implementations, and edge cases.
 */
class AttributeFormatterTest {

    // ========== BASIC FUNCTIONAL INTERFACE TESTS ==========

    @Test
    void testCustomFormatter_BasicUsage() {
        AttributeFormatter<String> upperCaseFormatter = value -> value == null ? null : value.toUpperCase();
        
        assertThat(upperCaseFormatter.format("hello")).isEqualTo("HELLO");
        assertThat(upperCaseFormatter.format("world")).isEqualTo("WORLD");
        assertThat(upperCaseFormatter.format(null)).isNull();
        assertThat(upperCaseFormatter.format("")).isEqualTo("");
    }

    @Test
    void testCustomFormatter_NumberToString() {
        AttributeFormatter<Number> numberFormatter = value -> 
            value == null ? "N/A" : String.format("Value: %,.2f", value.doubleValue());
        
        assertThat(numberFormatter.format(123.456)).isEqualTo("Value: 123.46");
        assertThat(numberFormatter.format(1000)).isEqualTo("Value: 1,000.00");
        assertThat(numberFormatter.format(null)).isEqualTo("N/A");
    }

    // ========== NULL SAFE FORMATTER TESTS ==========

    @Test
    void testNullSafeFormatter_WithNullValue() {
        AttributeFormatter<String> baseFormatter = String::toUpperCase;
        AttributeFormatter<String> nullSafeFormatter = AttributeFormatter.nullSafe(baseFormatter, "NO_VALUE");
        
        assertThat(nullSafeFormatter.format("hello")).isEqualTo("HELLO");
        assertThat(nullSafeFormatter.format(null)).isEqualTo("NO_VALUE");
    }

    @Test
    void testNullSafeFormatter_WithEmptyString() {
        AttributeFormatter<String> baseFormatter = value -> "Formatted: " + value;
        AttributeFormatter<String> nullSafeFormatter = AttributeFormatter.nullSafe(baseFormatter, "EMPTY");
        
        assertThat(nullSafeFormatter.format("test")).isEqualTo("Formatted: test");
        assertThat(nullSafeFormatter.format("")).isEqualTo("Formatted: ");
        assertThat(nullSafeFormatter.format(null)).isEqualTo("EMPTY");
    }

    @Test
    void testNullSafeFormatter_WithNumber() {
        AttributeFormatter<Number> baseFormatter = value -> value.toString();
        AttributeFormatter<Number> nullSafeFormatter = AttributeFormatter.nullSafe(baseFormatter, "0");
        
        assertThat(nullSafeFormatter.format(123)).isEqualTo("123");
        assertThat(nullSafeFormatter.format(null)).isEqualTo("0");
    }

    // ========== CURRENCY FORMATTER TESTS ==========

    @Test
    void testCurrencyFormatter_BasicUsage() {
        AttributeFormatter<Number> dollarFormatter = AttributeFormatter.currency("$");
        
        assertThat(dollarFormatter.format(123.456)).isEqualTo("$123.46");
        assertThat(dollarFormatter.format(1000)).isEqualTo("$1,000.00");
        assertThat(dollarFormatter.format(0)).isEqualTo("$0.00");
        assertThat(dollarFormatter.format(null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "123.456, '$123.46'",
        "1000, '$1,000.00'",
        "1000000, '$1,000,000.00'",
        "0.01, '$0.01'",
        "0, '$0.00'",
        "-100.50, '$-100.50'"
    })
    void testCurrencyFormatter_VariousValues(double input, String expected) {
        AttributeFormatter<Number> formatter = AttributeFormatter.currency("$");
        assertThat(formatter.format(input)).isEqualTo(expected);
    }

    @Test
    void testCurrencyFormatter_DifferentSymbols() {
        AttributeFormatter<Number> euroFormatter = AttributeFormatter.currency("€");
        AttributeFormatter<Number> yenFormatter = AttributeFormatter.currency("¥");
        
        assertThat(euroFormatter.format(123.45)).isEqualTo("€123.45");
        assertThat(yenFormatter.format(123.45)).isEqualTo("¥123.45");
    }

    @Test
    void testCurrencyFormatter_WithBigDecimal() {
        AttributeFormatter<Number> formatter = AttributeFormatter.currency("$");
        BigDecimal value = new BigDecimal("123.456");
        
        assertThat(formatter.format(value)).isEqualTo("$123.46");
    }

    @Test
    void testCurrencyFormatter_WithInteger() {
        AttributeFormatter<Number> formatter = AttributeFormatter.currency("$");
        
        assertThat(formatter.format(123)).isEqualTo("$123.00");
    }

    @Test
    void testCurrencyFormatter_WithLong() {
        AttributeFormatter<Number> formatter = AttributeFormatter.currency("$");
        
        assertThat(formatter.format(123L)).isEqualTo("$123.00");
    }

    // ========== PERCENTAGE FORMATTER TESTS ==========

    @Test
    void testPercentageFormatter_BasicUsage() {
        AttributeFormatter<Number> percentageFormatter = AttributeFormatter.percentage();
        
        assertThat(percentageFormatter.format(0.1234)).isEqualTo("0.1%");
        assertThat(percentageFormatter.format(50.0)).isEqualTo("50.0%");
        assertThat(percentageFormatter.format(100)).isEqualTo("100.0%");
        assertThat(percentageFormatter.format(null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "0.1234, '0.1%'",
        "0.5, '0.5%'",
        "1.0, '1.0%'",
        "50.0, '50.0%'",
        "100.0, '100.0%'",
        "0, '0.0%'",
        "0.95, '1.0%'",  // Tests rounding
        "0.94, '0.9%'"   // Tests rounding
    })
    void testPercentageFormatter_VariousValues(double input, String expected) {
        AttributeFormatter<Number> formatter = AttributeFormatter.percentage();
        assertThat(formatter.format(input)).isEqualTo(expected);
    }

    @Test
    void testPercentageFormatter_WithInteger() {
        AttributeFormatter<Number> formatter = AttributeFormatter.percentage();
        
        assertThat(formatter.format(25)).isEqualTo("25.0%");
    }

    @Test
    void testPercentageFormatter_WithBigDecimal() {
        AttributeFormatter<Number> formatter = AttributeFormatter.percentage();
        BigDecimal value = new BigDecimal("0.12345");
        
        assertThat(formatter.format(value)).isEqualTo("0.1%");
    }

    @Test
    void testPercentageFormatter_WithNegativeValues() {
        AttributeFormatter<Number> formatter = AttributeFormatter.percentage();
        
        assertThat(formatter.format(-25.0)).isEqualTo("-25.0%");
        assertThat(formatter.format(-0.5)).isEqualTo("-0.5%");
    }

    // ========== MASK FORMATTER TESTS ==========

    @Test
    void testMaskFormatter_BasicUsage() {
        AttributeFormatter<String> maskFormatter = AttributeFormatter.mask("***");
        
        assertThat(maskFormatter.format("sensitive_data")).isEqualTo("***");
        assertThat(maskFormatter.format("another_value")).isEqualTo("***");
        assertThat(maskFormatter.format(null)).isEqualTo("***");
        assertThat(maskFormatter.format("")).isEqualTo("***");
    }

    @Test
    void testMaskFormatter_WithNumbers() {
        AttributeFormatter<Integer> maskFormatter = AttributeFormatter.mask("HIDDEN");
        
        assertThat(maskFormatter.format(123)).isEqualTo("HIDDEN");
        assertThat(maskFormatter.format(0)).isEqualTo("HIDDEN");
        assertThat(maskFormatter.format(null)).isEqualTo("HIDDEN");
    }

    @Test
    void testMaskFormatter_EmptyMask() {
        AttributeFormatter<String> emptyMaskFormatter = AttributeFormatter.mask("");
        
        assertThat(emptyMaskFormatter.format("anything")).isEqualTo("");
        assertThat(emptyMaskFormatter.format(null)).isEqualTo("");
    }

    @Test
    void testMaskFormatter_NullMask() {
        AttributeFormatter<String> nullMaskFormatter = AttributeFormatter.mask(null);
        
        assertThat(nullMaskFormatter.format("anything")).isNull();
        assertThat(nullMaskFormatter.format(null)).isNull();
    }

    // ========== TRUNCATE FORMATTER TESTS ==========

    @Test
    void testTruncateFormatter_BasicUsage() {
        AttributeFormatter<String> truncateFormatter = AttributeFormatter.truncate(5);
        
        assertThat(truncateFormatter.format("hello")).isEqualTo("hello");
        assertThat(truncateFormatter.format("hello world")).isEqualTo("hello...");
        assertThat(truncateFormatter.format("hi")).isEqualTo("hi");
        assertThat(truncateFormatter.format("")).isEqualTo("");
        assertThat(truncateFormatter.format(null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "hello, 5, hello",
        "hello world, 5, 'hello...'",
        "testing truncation, 7, 'testing...'",
        "short, 10, short",
        "'', 5, ''",
        "exactly, 7, exactly"
    })
    void testTruncateFormatter_VariousLengths(String input, int maxLength, String expected) {
        AttributeFormatter<String> formatter = AttributeFormatter.truncate(maxLength);
        assertThat(formatter.format(input)).isEqualTo(expected);
    }

    @Test
    void testTruncateFormatter_ZeroLength() {
        AttributeFormatter<String> formatter = AttributeFormatter.truncate(0);
        
        assertThat(formatter.format("anything")).isEqualTo("...");
        assertThat(formatter.format("")).isEqualTo("");
    }

    @Test
    void testTruncateFormatter_NegativeLength() {
        AttributeFormatter<String> formatter = AttributeFormatter.truncate(-1);
        
        assertThat(formatter.format("anything")).isEqualTo("...");
        assertThat(formatter.format("")).isEqualTo("");
    }

    @Test
    void testTruncateFormatter_LargeLength() {
        AttributeFormatter<String> formatter = AttributeFormatter.truncate(1000);
        String longString = "a".repeat(500);
        
        assertThat(formatter.format(longString)).isEqualTo(longString);
        assertThat(formatter.format("short")).isEqualTo("short");
    }

    // ========== COMBINED FORMATTER TESTS ==========

    @Test
    void testCombinedFormatters_NullSafeWithCurrency() {
        AttributeFormatter<Number> baseFormatter = AttributeFormatter.currency("$");
        AttributeFormatter<Number> nullSafeFormatter = AttributeFormatter.nullSafe(baseFormatter, "FREE");
        
        assertThat(nullSafeFormatter.format(123.45)).isEqualTo("$123.45");
        assertThat(nullSafeFormatter.format(null)).isEqualTo("FREE");
    }

    @Test
    void testCombinedFormatters_NullSafeWithPercentage() {
        AttributeFormatter<Number> baseFormatter = AttributeFormatter.percentage();
        AttributeFormatter<Number> nullSafeFormatter = AttributeFormatter.nullSafe(baseFormatter, "N/A");
        
        assertThat(nullSafeFormatter.format(0.25)).isEqualTo("0.3%");
        assertThat(nullSafeFormatter.format(null)).isEqualTo("N/A");
    }

    @Test
    void testCombinedFormatters_NullSafeWithTruncate() {
        AttributeFormatter<String> baseFormatter = AttributeFormatter.truncate(3);
        AttributeFormatter<String> nullSafeFormatter = AttributeFormatter.nullSafe(baseFormatter, "EMPTY");
        
        assertThat(nullSafeFormatter.format("hello world")).isEqualTo("hel...");
        assertThat(nullSafeFormatter.format("hi")).isEqualTo("hi");
        assertThat(nullSafeFormatter.format(null)).isEqualTo("EMPTY");
    }

    // ========== EDGE CASES AND ERROR SCENARIOS ==========

    @Test
    void testFormatters_WithSpecialNumbers() {
        AttributeFormatter<Number> currencyFormatter = AttributeFormatter.currency("$");
        AttributeFormatter<Number> percentageFormatter = AttributeFormatter.percentage();
        
        // Test with Double.NaN, Double.POSITIVE_INFINITY, etc.
        assertThat(currencyFormatter.format(Double.NaN)).isEqualTo("$NaN");
        assertThat(currencyFormatter.format(Double.POSITIVE_INFINITY)).isEqualTo("$Infinity");
        assertThat(currencyFormatter.format(Double.NEGATIVE_INFINITY)).isEqualTo("$-Infinity");
        
        assertThat(percentageFormatter.format(Double.NaN)).isEqualTo("NaN%");
        assertThat(percentageFormatter.format(Double.POSITIVE_INFINITY)).isEqualTo("Infinity%");
    }

    @Test
    void testFormatters_WithVeryLargeNumbers() {
        AttributeFormatter<Number> currencyFormatter = AttributeFormatter.currency("$");
        
        assertThat(currencyFormatter.format(1e12)).isEqualTo("$1,000,000,000,000.00");
        assertThat(currencyFormatter.format(-1e12)).isEqualTo("$-1,000,000,000,000.00");
    }

    @Test
    void testFormatters_WithVerySmallNumbers() {
        AttributeFormatter<Number> currencyFormatter = AttributeFormatter.currency("$");
        AttributeFormatter<Number> percentageFormatter = AttributeFormatter.percentage();
        
        assertThat(currencyFormatter.format(0.001)).isEqualTo("$0.00");
        assertThat(percentageFormatter.format(0.001)).isEqualTo("0.0%");
    }

    // ========== CHAINING FORMATTERS ==========

    @Test
    void testChainedFormatters_CustomLogic() {
        // Create a formatter that first applies currency formatting, then truncates
        AttributeFormatter<Number> currencyFormatter = AttributeFormatter.currency("$");
        AttributeFormatter<Number> chainedFormatter = value -> {
            String currencyString = currencyFormatter.format(value);
            return currencyString == null ? null : 
                (currencyString.length() > 10 ? currencyString.substring(0, 10) + "..." : currencyString);
        };
        
        assertThat(chainedFormatter.format(123.45)).isEqualTo("$123.45");
        assertThat(chainedFormatter.format(1234567.89)).isEqualTo("$1,234,567...");
        assertThat(chainedFormatter.format(null)).isNull();
    }

    // ========== PERFORMANCE TESTS (BASIC) ==========

    @Test
    void testFormatter_PerformanceWithManyValues() {
        AttributeFormatter<Number> formatter = AttributeFormatter.currency("$");
        
        // Basic performance test - should handle many values without issues
        for (int i = 0; i < 1000; i++) {
            String result = formatter.format(i * 0.01);
            assertThat(result).isNotNull().startsWith("$");
        }
    }
}