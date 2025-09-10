package com.balsam.oasis.common.registry.domain.common;

import com.google.common.base.CaseFormat;

/**
 * Naming strategy for dynamic attributes coming from Oracle SQL columns (always
 * UPPER_UNDERSCORE).
 * Uses Guava CaseFormat for reliable name conversion.
 */
public enum NamingStrategy {
    AS_IS, // Keep original column name (USER_NAME -> USER_NAME)
    CAMEL, // Convert to lowerCamelCase (USER_NAME -> userName)
    PASCAL, // Convert to UpperCamelCase (USER_NAME -> UserName)
    LOWER, // Convert to lower_snake_case (USER_NAME -> user_name)
    UPPER; // Convert to UPPER_SNAKE_CASE (USER_NAME -> USER_NAME)

    /**
     * Convert a column name according to this naming strategy.
     *
     * @param columnName the original Oracle column name (always UPPER_SNAKE)
     * @return the converted name
     */
    public String convert(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }

        return switch (this) {
            case AS_IS -> columnName;
            case CAMEL -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnName);
            case PASCAL -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName);
            case LOWER -> columnName.toLowerCase();
            case UPPER -> columnName.toUpperCase();
        };
    }
}
