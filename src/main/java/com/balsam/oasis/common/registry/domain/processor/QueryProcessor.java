package com.balsam.oasis.common.registry.domain.processor;

import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.engine.query.QueryRow;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface QueryProcessor {
    Object process(Object input, QueryContext context);

    // Static factory methods for different processor types

    static QueryProcessor attributeFormatter(Function<Object, String> formatter) {
        return (input, context) -> {
            if (input == null) return null;
            return formatter.apply(input);
        };
    }

    static QueryProcessor calculator(BiFunction<QueryRow, QueryContext, Object> calc) {
        return (input, context) -> {
            if (!(input instanceof QueryRow)) {
                throw new IllegalArgumentException("Calculator processor requires QueryRow input");
            }
            return calc.apply((QueryRow) input, context);
        };
    }

    static QueryProcessor paramProcessor(BiFunction<Object, QueryContext, Object> processor) {
        return (input, context) -> processor.apply(input, context);
    }

    static QueryProcessor preProcessor(PreProcessorAction action) {
        return (input, context) -> {
            action.process(context);
            return null; // Pre-processors don't return values
        };
    }

    static QueryProcessor rowProcessor(RowProcessorAction action) {
        return (input, context) -> {
            if (!(input instanceof QueryRow)) {
                throw new IllegalArgumentException("Row processor requires QueryRow input");
            }
            return action.process((QueryRow) input, context);
        };
    }

    static QueryProcessor postProcessor(PostProcessorAction action) {
        return (input, context) -> {
            if (!(input instanceof QueryData)) {
                throw new IllegalArgumentException("Post processor requires QueryData input");
            }
            return action.process((QueryData) input, context);
        };
    }

    // Helper functional interfaces for cleaner syntax
    @FunctionalInterface
    interface PreProcessorAction {
        void process(QueryContext context);
    }

    @FunctionalInterface
    interface RowProcessorAction {
        QueryRow process(QueryRow row, QueryContext context);
    }

    @FunctionalInterface
    interface PostProcessorAction {
        QueryData process(QueryData result, QueryContext context);
    }
}