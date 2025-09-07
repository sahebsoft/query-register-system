package com.balsam.oasis.common.registry.processor;

import java.util.function.Function;
import java.util.function.Predicate;

import com.balsam.oasis.common.registry.query.QueryContext;

/**
 * Generic processor for parameter validation and transformation.
 * Applied before query execution when no Row is available.
 * Can accept any input type and return the target type for flexible processing.
 * 
 * @param <T> the target type of parameter value to return
 */
@FunctionalInterface
public interface ParamProcessor<T> {

    /**
     * Process a parameter value with query context.
     * Accepts any input type and returns the target type.
     *
     * @param value   the parameter value (can be any type)
     * @param context the query context
     * @return the processed/validated value of target type T
     * @throws IllegalArgumentException if validation fails
     */
    T process(Object value, QueryContext context);

    /**
     * Convenience static method for simple value-only processing with type conversion
     */
    static <T> ParamProcessor<T> simple(Function<Object, T> func) {
        return (value, context) -> func.apply(value);
    }

    /**
     * Convenience static method for simple same-type processing (legacy compatibility)
     */
    @SuppressWarnings("unchecked")
    static <T> ParamProcessor<T> sameType(Function<T, T> func) {
        return (value, context) -> {
            try {
                return func.apply((T) value);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(
                    String.format("Expected type %s but got %s", 
                        func.getClass().getGenericInterfaces()[0], 
                        value != null ? value.getClass().getSimpleName() : "null"), e);
            }
        };
    }

    /**
     * Convenience static method for validation with type conversion
     */
    @SuppressWarnings("unchecked")
    static <T> ParamProcessor<T> validator(Predicate<Object> validator, String errorMessage) {
        return (value, context) -> {
            if (!validator.test(value)) {
                throw new IllegalArgumentException(errorMessage);
            }
            return (T) value;
        };
    }

    /**
     * Convenience static method for validation with specific type (legacy compatibility)
     */
    @SuppressWarnings("unchecked")
    static <T> ParamProcessor<T> typedValidator(Predicate<T> validator, String errorMessage) {
        return (value, context) -> {
            try {
                T typedValue = (T) value;
                if (!validator.test(typedValue)) {
                    throw new IllegalArgumentException(errorMessage);
                }
                return typedValue;
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(
                    String.format("Type validation failed: expected specific type but got %s", 
                        value != null ? value.getClass().getSimpleName() : "null"), e);
            }
        };
    }

    /**
     * Convenience static method for range validation (numbers)
     * Accepts any input type and validates range, returns Number
     */
    static ParamProcessor<Number> range(int min, int max) {
        return (value, context) -> {
            if (value == null)
                return null;
            
            Number numValue;
            if (value instanceof Number) {
                numValue = (Number) value;
            } else if (value instanceof String) {
                try {
                    numValue = Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        String.format("Cannot convert '%s' to number", value), e);
                }
            } else {
                throw new IllegalArgumentException(
                    String.format("Cannot convert %s to Number", 
                        value.getClass().getSimpleName()));
            }
            
            double d = numValue.doubleValue();
            if (d < min || d > max) {
                throw new IllegalArgumentException(
                        String.format("Value %s is outside range [%d, %d]", numValue, min, max));
            }
            return numValue;
        };
    }

    /**
     * Convenience static method for range validation with specific type
     */
    static <T extends Number> ParamProcessor<T> rangeWithType(Class<T> targetType, int min, int max) {
        return (value, context) -> {
            if (value == null)
                return null;
            
            // First validate range
            Number numValue;
            if (value instanceof Number) {
                numValue = (Number) value;
            } else if (value instanceof String) {
                try {
                    numValue = Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        String.format("Cannot convert '%s' to number", value), e);
                }
            } else {
                throw new IllegalArgumentException(
                    String.format("Cannot convert %s to Number", 
                        value.getClass().getSimpleName()));
            }
            
            double d = numValue.doubleValue();
            if (d < min || d > max) {
                throw new IllegalArgumentException(
                        String.format("Value %s is outside range [%d, %d]", numValue, min, max));
            }
            
            // Then convert to target type
            try {
                return com.balsam.oasis.common.registry.util.TypeConverter.convert(numValue, targetType);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format("Failed to convert %s to %s: %s", 
                        numValue.getClass().getSimpleName(), 
                        targetType.getSimpleName(), 
                        e.getMessage()), e);
            }
        };
    }

    /**
     * Convenience static method for string length validation
     * Accepts any input type and converts to String
     */
    static ParamProcessor<String> lengthBetween(int min, int max) {
        return (value, context) -> {
            if (value == null)
                return null;
            
            String strValue = value.toString();
            int length = strValue.length();
            if (length < min || length > max) {
                throw new IllegalArgumentException(
                        String.format("String length %d is outside range [%d, %d]", length, min, max));
            }
            return strValue;
        };
    }

    /**
     * Convenience static method for pattern validation
     * Accepts any input type and converts to String
     */
    static ParamProcessor<String> pattern(String pattern) {
        return (value, context) -> {
            if (value == null)
                return null;
            
            String strValue = value.toString();
            if (!strValue.matches(pattern)) {
                throw new IllegalArgumentException(
                        String.format("Value '%s' does not match pattern '%s'", strValue, pattern));
            }
            return strValue;
        };
    }

    /**
     * Convenience static method for type conversion using TypeConverter
     */
    static <T> ParamProcessor<T> convert(Class<T> targetType) {
        return (value, context) -> {
            if (value == null)
                return null;
            
            try {
                return com.balsam.oasis.common.registry.util.TypeConverter.convert(value, targetType);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format("Failed to convert %s to %s: %s", 
                        value.getClass().getSimpleName(), 
                        targetType.getSimpleName(), 
                        e.getMessage()), e);
            }
        };
    }

    /**
     * Convenience static method for type conversion with validation
     */
    static <T> ParamProcessor<T> convertAndValidate(Class<T> targetType, Predicate<T> validator, String errorMessage) {
        return (value, context) -> {
            if (value == null)
                return null;
            
            // First convert
            T convertedValue;
            try {
                convertedValue = com.balsam.oasis.common.registry.util.TypeConverter.convert(value, targetType);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format("Failed to convert %s to %s: %s", 
                        value.getClass().getSimpleName(), 
                        targetType.getSimpleName(), 
                        e.getMessage()), e);
            }
            
            // Then validate
            if (!validator.test(convertedValue)) {
                throw new IllegalArgumentException(errorMessage);
            }
            
            return convertedValue;
        };
    }

}
