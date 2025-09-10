package com.balsam.oasis.common.registry.domain.exception;

/**
 * Exception thrown when query execution times out
 */
public class QueryTimeoutException extends QueryException {

    private final long timeoutMs;

    public QueryTimeoutException(String queryName, long timeoutMs) {
        super(queryName, "TIMEOUT_ERROR",
                String.format("Query execution exceeded timeout of %d ms", timeoutMs));
        this.timeoutMs = timeoutMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}