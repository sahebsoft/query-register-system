package com.balsam.oasis.common.query.exception;

/**
 * Exception thrown when a query is not found
 */
public class QueryNotFoundException extends QueryException {

    public QueryNotFoundException(String queryName) {
        super(queryName, "NOT_FOUND", String.format("Query '%s' not found", queryName));
    }
}