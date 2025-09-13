package com.balsam.oasis.common.registry.domain.exception;

/**
 * Base exception for all query-related errors
 */
public class QueryException extends RuntimeException {

    /**
     * Error codes for different types of query exceptions
     */
    public enum ErrorCode {
        QUERY_NOT_FOUND("QRY001", "Query not found"),
        EXECUTION_ERROR("QRY002", "Query execution error"),
        TIMEOUT("QRY003", "Query timeout"),
        SECURITY_VIOLATION("QRY004", "Security violation"),
        DEFINITION_ERROR("QRY005", "Query definition error"),
        VALIDATION_ERROR("QRY006", "Validation error"),
        SQL_ERROR("QRY007", "SQL error"),
        PARAMETER_ERROR("QRY008", "Parameter error");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    private final String queryName;
    private final String errorCode;

    public QueryException(String message) {
        super(message);
        this.queryName = null;
        this.errorCode = null;
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
        this.queryName = null;
        this.errorCode = null;
    }

    public QueryException(String queryName, String message) {
        super(formatMessage(queryName, message));
        this.queryName = queryName;
        this.errorCode = null;
    }

    public QueryException(String queryName, String message, Throwable cause) {
        super(formatMessage(queryName, message), cause);
        this.queryName = queryName;
        this.errorCode = null;
    }

    public QueryException(String queryName, String errorCode, String message) {
        super(formatMessage(queryName, message));
        this.queryName = queryName;
        this.errorCode = errorCode;
    }

    public QueryException(String queryName, String errorCode, String message, Throwable cause) {
        super(formatMessage(queryName, message), cause);
        this.queryName = queryName;
        this.errorCode = errorCode;
    }

    // New constructors with ErrorCode enum
    public QueryException(ErrorCode errorCode, String message) {
        super(message);
        this.queryName = null;
        this.errorCode = errorCode.getCode();
    }

    public QueryException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.queryName = null;
        this.errorCode = errorCode.getCode();
    }

    public QueryException(String queryName, ErrorCode errorCode, String message) {
        super(formatMessage(queryName, message));
        this.queryName = queryName;
        this.errorCode = errorCode.getCode();
    }

    public QueryException(String queryName, ErrorCode errorCode, String message, Throwable cause) {
        super(formatMessage(queryName, message), cause);
        this.queryName = queryName;
        this.errorCode = errorCode.getCode();
    }

    private static String formatMessage(String queryName, String message) {
        if (queryName != null) {
            return String.format("Query '%s': %s", queryName, message);
        }
        return message;
    }

    public String getQueryName() {
        return queryName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}