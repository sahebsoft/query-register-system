package com.balsam.oasis.common.query.exception;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Exception thrown when query validation fails
 */
public class QueryValidationException extends QueryException {

    private final List<String> violations;

    public QueryValidationException(String message) {
        super(message);
        this.violations = ImmutableList.of();
    }

    public QueryValidationException(String queryName, List<String> violations) {
        super(queryName, "VALIDATION_ERROR",
                String.format("Validation failed: %s", String.join(", ", violations)));
        this.violations = ImmutableList.copyOf(violations);
    }

    public QueryValidationException(String queryName, String message) {
        super(queryName, "VALIDATION_ERROR", message);
        this.violations = ImmutableList.of(message);
    }

    public List<String> getViolations() {
        return violations;
    }
}