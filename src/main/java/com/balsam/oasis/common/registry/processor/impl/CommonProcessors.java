package com.balsam.oasis.common.registry.processor.impl;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

import com.balsam.oasis.common.registry.processor.AttributeFormatter;
import com.balsam.oasis.common.registry.processor.Validator;

/**
 * Common processor implementations
 */
public class CommonProcessors {

    // Validators
    public static final Validator EMAIL_VALIDATOR = value -> {
        if (value == null)
            return false;
        String email = value.toString().trim();
        if (email.isEmpty() || email.contains(" ") || email.contains(".."))
            return false;
        return Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,}|[A-Za-z0-9]+)$").matcher(email).matches();
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
    public static final AttributeFormatter<BigDecimal> CURRENCY_FORMATTER = (value) -> {
        if (value == null)
            return null;
        BigDecimal amount = value instanceof BigDecimal ? (BigDecimal) value
                : new BigDecimal(value.toString());
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format(amount);
    };

    public static final AttributeFormatter<Number> PERCENTAGE_FORMATTER = (value) -> {
        if (value == null)
            return null;
        double percentage = value instanceof Number ? ((Number) value).doubleValue()
                : Double.parseDouble(value.toString());
        return String.format("%.2f%%", percentage);
    };

    public static final AttributeFormatter<LocalDate> DATE_FORMATTER = (value) -> {
        if (value == null)
            return null;
        if (value instanceof LocalDate localDate) {
            return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return value.toString();
    };

    public static final AttributeFormatter<LocalDateTime> DATETIME_FORMATTER = (value) -> {
        if (value == null)
            return null;
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return value.toString();
    };

    // Processors
    public static final AttributeFormatter<String> UPPERCASE_PROCESSOR = (value) -> value != null ? value.toUpperCase()
            : null;

    public static final AttributeFormatter<String> LOWERCASE_PROCESSOR = (value) -> value != null ? value.toLowerCase()
            : null;

    public static final AttributeFormatter<String> TRIM_PROCESSOR = (value) -> value != null
            ? value.trim()
            : null;

    public static final AttributeFormatter<String> MASK_SSN_PROCESSOR = (value) -> {
        if (value == null)
            return null;
        String ssn = value.replaceAll("[^0-9]", "");
        if (ssn.length() == 9) {
            return "***-**-" + ssn.substring(5);
        }
        return "***MASKED***";
    };

    public static final AttributeFormatter<String> MASK_EMAIL_PROCESSOR = (value) -> {
        if (value == null)
            return null;
        String email = value;
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {  // Changed from > 1 to > 0
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return "***@***";
    };

}