package com.balasam.oasis.common.query.processor;

import com.balasam.oasis.common.query.core.execution.QueryContext;

/**
 * Generator for dynamic SQL criteria
 */
@FunctionalInterface
public interface CriteriaGenerator {
    /**
     * Generate SQL criteria dynamically based on context
     * @param context the query context
     * @return the generated SQL fragment
     */
    String generate(QueryContext context);
}