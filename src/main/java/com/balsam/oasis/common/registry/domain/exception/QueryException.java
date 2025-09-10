package com.balsam.oasis.common.registry.domain.exception;

/**
 * Base exception for all query-related errors
 */
public class QueryException extends RuntimeException {

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