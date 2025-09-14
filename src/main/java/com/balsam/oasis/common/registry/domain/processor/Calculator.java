package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QueryRow;

@FunctionalInterface
public interface Calculator<T> extends QueryProcessor {
    T calculate(QueryRow row, QueryContext context);

    /**
     * Enhanced calculation method that provides access to all query rows for aggregate calculations.
     * Default implementation delegates to the single-row calculate method for backward compatibility.
     */
    default T calculateWithAllRows(QueryRow currentRow, java.util.List<QueryRow> allRows, QueryContext context) {
        return calculate(currentRow, context);
    }

    @Override
    default Object process(Object input, QueryContext context) {
        if (!(input instanceof QueryRow)) {
            throw new IllegalArgumentException("Calculator processor requires QueryRow input");
        }
        return calculate((QueryRow) input, context);
    }

    /**
     * Creates a Calculator that has access to all rows for aggregate calculations
     */
    static <T> Calculator<T> withFullContext(AggregateCalculator<T> aggregateCalc) {
        return new Calculator<T>() {
            @Override
            public T calculate(QueryRow row, QueryContext context) {
                // This will be called during initial mapping - use simple calculation
                return aggregateCalc.calculate(row, java.util.List.of(row), context);
            }

            @Override
            public T calculateWithAllRows(QueryRow currentRow, java.util.List<QueryRow> allRows, QueryContext context) {
                return aggregateCalc.calculate(currentRow, allRows, context);
            }
        };
    }

    @FunctionalInterface
    interface AggregateCalculator<T> {
        T calculate(QueryRow currentRow, java.util.List<QueryRow> allRows, QueryContext context);
    }
}