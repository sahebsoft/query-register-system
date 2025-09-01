package com.balasam.oasis.common.query.processor.impl;

import com.balasam.oasis.common.query.processor.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Common processor implementations
 */
public class CommonProcessors {

    // Validators
    public static final Validator EMAIL_VALIDATOR = value -> {
        if (value == null)
            return false;
        String email = value.toString();
        return Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$").matcher(email).matches();
    };

    public static final Validator PHONE_VALIDATOR = value -> {
        if (value == null)
            return false;
        String phone = value.toString().replaceAll("[^0-9]", "");
        return phone.length() >= 10 && phone.length() <= 15;
    };

    public static final Validator NOT_NULL_VALIDATOR = value -> value != null;

    public static final Validator NOT_EMPTY_VALIDATOR = value -> value != null && !value.toString().trim().isEmpty();

    // Formatters
    public static final AttributeProcessor<BigDecimal> CURRENCY_FORMATTER = (value, row, context) -> {
        if (value == null)
            return null;
        BigDecimal amount = value instanceof BigDecimal ? (BigDecimal) value
                : new BigDecimal(value.toString());
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format(amount);
    };

    public static final AttributeProcessor<Number> PERCENTAGE_FORMATTER = (value, row, context) -> {
        if (value == null)
            return null;
        double percentage = value instanceof Number ? ((Number) value).doubleValue()
                : Double.parseDouble(value.toString());
        return String.format("%.2f%%", percentage);
    };

    public static final AttributeProcessor<LocalDate> DATE_FORMATTER = (value, row, context) -> {
        if (value == null)
            return null;
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return value.toString();
    };

    public static final AttributeProcessor<LocalDateTime> DATETIME_FORMATTER = (value, row, context) -> {
        if (value == null)
            return null;
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return value.toString();
    };

    // Processors
    public static final AttributeProcessor<String> UPPERCASE_PROCESSOR = (value, row,
            context) -> value != null ? value.toString().toUpperCase() : null;

    public static final AttributeProcessor<String> LOWERCASE_PROCESSOR = (value, row,
            context) -> value != null ? value.toString().toLowerCase() : null;

    public static final AttributeProcessor<String> TRIM_PROCESSOR = (value, row, context) -> value != null
            ? value.toString().trim()
            : null;

    public static final AttributeProcessor<String> MASK_SSN_PROCESSOR = (value, row, context) -> {
        if (value == null)
            return null;
        String ssn = value.toString().replaceAll("[^0-9]", "");
        if (ssn.length() == 9) {
            return "***-**-" + ssn.substring(5);
        }
        return "***MASKED***";
    };

    public static final AttributeProcessor<String> MASK_EMAIL_PROCESSOR = (value, row, context) -> {
        if (value == null)
            return null;
        String email = value.toString();
        int atIndex = email.indexOf('@');
        if (atIndex > 1) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return "***@***";
    };

}