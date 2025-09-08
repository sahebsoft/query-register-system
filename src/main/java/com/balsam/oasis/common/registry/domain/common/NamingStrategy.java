package com.balsam.oasis.common.registry.domain.common;

/**
 * Naming strategy for dynamic attributes that are not explicitly defined.
 * Used to convert database column names to attribute names in responses.
 */
public enum NamingStrategy {
    AS_IS,    // Keep original column name as-is
    CAMEL,    // Convert to camelCase (USER_NAME -> userName)
    PASCAL,   // Convert to PascalCase (USER_NAME -> UserName)
    SNAKE,    // Convert to snake_case (userName -> user_name)
    UPPER,    // Convert to UPPER_CASE (userName -> USER_NAME)
    LOWER;    // Convert to lowercase (USER_NAME -> user_name)

    /**
     * Convert a column name according to this naming strategy.
     * 
     * @param columnName the original column name from database
     * @return the converted name according to the strategy
     */
    public String convert(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }

        switch (this) {
            case CAMEL:
                return toCamelCase(columnName);
            case PASCAL:
                return toPascalCase(columnName);
            case SNAKE:
                return toSnakeCase(columnName);
            case UPPER:
                return columnName.toUpperCase();
            case LOWER:
                return columnName.toLowerCase();
            case AS_IS:
            default:
                return columnName;
        }
    }

    private String toCamelCase(String input) {
        if (!input.contains("_")) {
            // If no underscores, just lowercase the first letter
            return Character.toLowerCase(input.charAt(0)) + 
                   (input.length() > 1 ? input.substring(1).toLowerCase() : "");
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            
            if (ch == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(ch));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(ch));
                }
            }
        }
        
        return result.toString();
    }

    private String toPascalCase(String input) {
        if (!input.contains("_")) {
            // If no underscores, just uppercase the first letter
            return Character.toUpperCase(input.charAt(0)) + 
                   (input.length() > 1 ? input.substring(1).toLowerCase() : "");
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = true;
        
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            
            if (ch == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(ch));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(ch));
                }
            }
        }
        
        return result.toString();
    }

    private String toSnakeCase(String input) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else if (ch == ' ' || ch == '-') {
                result.append('_');
            } else {
                result.append(ch);
            }
        }
        
        return result.toString();
    }
}