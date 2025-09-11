package com.balsam.oasis.common.registry.domain.definition;

import java.util.function.Predicate;

import com.balsam.oasis.common.registry.domain.execution.QueryContext;

/**
 * Immutable criteria definition for dynamic SQL conditions.
 */
public record CriteriaDef(
        String name,
        String sql,
        Predicate<QueryContext> condition) {
    public static Builder name(String name) {
        return new Builder().name(name);
    }

    public boolean hasCondition() {
        return condition != null;
    }

    /**
     * Manual builder implementation for record
     */
    public static class Builder {
        private String name;
        private String sql;
        private Predicate<QueryContext> condition;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder condition(Predicate<QueryContext> condition) {
            this.condition = condition;
            return this;
        }

        public CriteriaDef build() {
            return new CriteriaDef(name, sql, condition);
        }
    }
}
