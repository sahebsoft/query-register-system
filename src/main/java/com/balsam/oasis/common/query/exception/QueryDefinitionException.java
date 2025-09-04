package com.balsam.oasis.common.query.exception;

/**
 * Exception thrown when query definition is invalid
 */
public class QueryDefinitionException extends QueryException {

    public QueryDefinitionException(String message) {
        super(message);
    }

    public QueryDefinitionException(String queryName, String message) {
        super(queryName, "DEFINITION_ERROR", message);
    }

    public QueryDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}