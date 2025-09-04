package com.balsam.oasis.common.query.exception;

/**
 * Exception thrown during query execution
 */
public class QueryExecutionException extends QueryException {

    public QueryExecutionException(String message) {
        super(message);
    }

    public QueryExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryExecutionException(String queryName, String message) {
        super(queryName, "EXECUTION_ERROR", message);
    }

    public QueryExecutionException(String queryName, String message, Throwable cause) {
        super(queryName, "EXECUTION_ERROR", message, cause);
    }
}