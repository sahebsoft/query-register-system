package com.balsam.oasis.common.query.processor;

import com.balsam.oasis.common.query.core.execution.QueryContext;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic processor for parameter validation and transformation
 * Applied before query execution when no Row is available
 * 
 * @param <T> the type of parameter value to process
 */
@FunctionalInterface
public interface ParamProcessor<T> {

    /**
     * Process a parameter value with query context
     *
     * @param value   the parameter value
     * @param context the query context
     * @return the processed/validated value
     * @throws IllegalArgumentException if validation fails
     */
    T process(T value, QueryContext context);

    /**
     * Convenience static method for simple value-only processing
     */
    static <T> ParamProcessor<T> simple(Function<T, T> func) {
        return (value, context) -> func.apply(value);
    }

    /**
     * Convenience static method for validation
     */
    static <T> ParamProcessor<T> validator(Predicate<T> validator, String errorMessage) {
        return (value, context) -> {
            if (!validator.test(value)) {
                throw new IllegalArgumentException(errorMessage);
            }
            return value;
        };
    }

    /**
     * Convenience static method for range validation (numbers)
     */
    static ParamProcessor<Number> range(int min, int max) {
        return (value, context) -> {
            if (value == null)
                return value;
            double d = value.doubleValue();
            if (d < min || d > max) {
                throw new IllegalArgumentException(
                        String.format("Value %s is outside range [%d, %d]", value, min, max));
            }
            return value;
        };
    }

    /**
     * Convenience static method for string length validation
     */
    static ParamProcessor<String> lengthBetween(int min, int max) {
        return (value, context) -> {
            if (value == null)
                return value;
            int length = value.length();
            if (length < min || length > max) {
                throw new IllegalArgumentException(
                        String.format("String length %d is outside range [%d, %d]", length, min, max));
            }
            return value;
        };
    }

    /**
     * Convenience static method for pattern validation
     */
    static ParamProcessor<String> pattern(String pattern) {
        return (value, context) -> {
            if (value == null)
                return value;
            if (!value.matches(pattern)) {
                throw new IllegalArgumentException(
                        String.format("Value '%s' does not match pattern '%s'", value, pattern));
            }
            return value;
        };
    }

}
