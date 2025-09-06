package com.balsam.oasis.common.registry.exception;

/**
 * Exception thrown when security constraints are violated
 */
public class QuerySecurityException extends QueryException {

    public QuerySecurityException(String message) {
        super(message);
    }

    public QuerySecurityException(String queryName, String message) {
        super(queryName, "SECURITY_ERROR", message);
    }

    public QuerySecurityException(String queryName, String attribute, String reason) {
        super(queryName, "SECURITY_ERROR",
                String.format("Access denied to attribute '%s': %s", attribute, reason));
    }
}