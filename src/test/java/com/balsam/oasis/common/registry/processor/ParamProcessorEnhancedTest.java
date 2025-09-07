package com.balsam.oasis.common.registry.processor;

import com.balsam.oasis.common.registry.query.QueryContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test cases for the enhanced ParamProcessor that accepts Object input
 * and returns the target type.
 */
class ParamProcessorEnhancedTest {

    @Test
    void testFlexibleTypeConversion() {
        // String to Integer conversion
        ParamProcessor<Integer> intProcessor = ParamProcessor.convert(Integer.class);
        Integer result = intProcessor.process("123", null);
        assertThat(result).isEqualTo(123);
    }

    @Test
    void testStringToLocalDateConversion() {
        // String to LocalDate conversion
        ParamProcessor<LocalDate> dateProcessor = ParamProcessor.convert(LocalDate.class);
        LocalDate result = dateProcessor.process("2023-12-25", null);
        assertThat(result).isEqualTo(LocalDate.of(2023, 12, 25));
    }

    @Test
    void testStringToBigDecimalConversion() {
        // String to BigDecimal conversion
        ParamProcessor<BigDecimal> decimalProcessor = ParamProcessor.convert(BigDecimal.class);
        BigDecimal result = decimalProcessor.process("123.45", null);
        assertThat(result).isEqualTo(new BigDecimal("123.45"));
    }

    @Test
    void testObjectToSameTypeConversion() {
        // Object already of target type
        ParamProcessor<String> stringProcessor = ParamProcessor.convert(String.class);
        String result = stringProcessor.process("hello", null);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void testNullHandling() {
        // Null input
        ParamProcessor<Integer> intProcessor = ParamProcessor.convert(Integer.class);
        Integer result = intProcessor.process(null, null);
        assertThat(result).isNull();
    }

    @Test
    void testSimpleProcessorWithFlexibleInput() {
        // Custom processor that converts and transforms
        ParamProcessor<String> upperCaseProcessor = ParamProcessor.simple(value -> {
            if (value == null) return null;
            return value.toString().toUpperCase();
        });

        String result = upperCaseProcessor.process("hello", null);
        assertThat(result).isEqualTo("HELLO");

        // Test with different input types
        String numberResult = upperCaseProcessor.process(123, null);
        assertThat(numberResult).isEqualTo("123");
    }

    @Test
    void testRangeValidatorWithFlexibleInput() {
        // Range validator that accepts String and converts to Number
        ParamProcessor<Number> rangeProcessor = ParamProcessor.range(1, 100);
        
        // Test with String input
        Number result = rangeProcessor.process("50", null);
        assertThat(result.doubleValue()).isEqualTo(50.0);

        // Test with Number input
        Number numberResult = rangeProcessor.process(75, null);
        assertThat(numberResult.intValue()).isEqualTo(75);
    }

    @Test
    void testRangeWithTypeValidation() {
        // Range validator with specific type conversion
        ParamProcessor<Integer> intRangeProcessor = ParamProcessor.rangeWithType(Integer.class, 1, 100);
        
        // Test with String input
        Integer result = intRangeProcessor.process("50", null);
        assertThat(result).isEqualTo(50);

        // Test with Number input
        Integer numberResult = intRangeProcessor.process(75, null);
        assertThat(numberResult).isEqualTo(75);
    }

    @Test
    void testRangeValidatorFailure() {
        ParamProcessor<Number> rangeProcessor = ParamProcessor.range(1, 100);
        
        // Test out of range
        assertThatThrownBy(() -> rangeProcessor.process("150", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("outside range [1, 100]");
    }

    @Test
    void testLengthValidatorWithFlexibleInput() {
        // Length validator that accepts any type and converts to String
        ParamProcessor<String> lengthProcessor = ParamProcessor.lengthBetween(3, 10);
        
        // Test with String input
        String result = lengthProcessor.process("hello", null);
        assertThat(result).isEqualTo("hello");

        // Test with Number input (converted to string)
        String numberResult = lengthProcessor.process(12345, null);
        assertThat(numberResult).isEqualTo("12345");
    }

    @Test
    void testPatternValidatorWithFlexibleInput() {
        // Pattern validator for email-like format
        ParamProcessor<String> emailProcessor = ParamProcessor.pattern(".*@.*\\..*");
        
        String result = emailProcessor.process("user@domain.com", null);
        assertThat(result).isEqualTo("user@domain.com");

        // Test with invalid pattern
        assertThatThrownBy(() -> emailProcessor.process("invalid-email", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not match pattern");
    }

    @Test
    void testConvertAndValidate() {
        // Combined conversion and validation
        ParamProcessor<Integer> processor = ParamProcessor.convertAndValidate(
            Integer.class,
            value -> value > 0,
            "Value must be positive"
        );

        Integer result = processor.process("42", null);
        assertThat(result).isEqualTo(42);

        // Test validation failure
        assertThatThrownBy(() -> processor.process("-5", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Value must be positive");
    }

    @Test
    void testComplexScenario() {
        // Create a processor that accepts String date, converts to LocalDate,
        // then formats back to a specific string format
        ParamProcessor<String> dateFormatProcessor = ParamProcessor.simple(value -> {
            if (value == null) return null;
            
            // First convert to LocalDate if it's a string
            LocalDate date;
            if (value instanceof String) {
                date = LocalDate.parse((String) value);
            } else if (value instanceof LocalDate) {
                date = (LocalDate) value;
            } else {
                throw new IllegalArgumentException("Unsupported date format");
            }
            
            // Format as MM/dd/yyyy
            return String.format("%02d/%02d/%d", 
                date.getMonthValue(), 
                date.getDayOfMonth(), 
                date.getYear());
        });

        String result = dateFormatProcessor.process("2023-12-25", null);
        assertThat(result).isEqualTo("12/25/2023");
    }
}