package com.balsam.oasis.common.registry.domain.definition;

import java.util.function.Predicate;

import com.balsam.oasis.common.registry.base.BaseContext;
import com.google.common.base.Preconditions;

import lombok.Value;

/**
 * Immutable criteria definition for dynamic SQL conditions
 * Uses builder pattern for construction
 */
@Value
public class CriteriaDef {
    String name;
    String sql; // SQL fragment with named parameters
    Predicate<BaseContext<?>> condition; // When to apply this criteria
    String description;
    boolean isFindByKey; // Is this a findByKey criteria (returns single object)

    // Private constructor - only accessible through builder
    private CriteriaDef(BuilderStage builder) {
        this.name = builder.name;
        this.sql = builder.sql;
        this.condition = builder.condition;
        this.description = builder.description;
        this.isFindByKey = builder.isFindByKey;
    }

    /**
     * Static factory method to start building a criteria
     * Must call .name() first to specify the criteria name
     */
    public static NameStage criteria() {
        return new NameStage();
    }

    /**
     * First stage - requires name specification
     */
    public static class NameStage {
        public BuilderStage name(String name) {
            return new BuilderStage(name);
        }
    }

    /**
     * Builder stage for criteria
     */
    public static class BuilderStage {
        private final String name;
        private String sql;
        private Predicate<BaseContext<?>> condition;
        private String description;
        private boolean isFindByKey = false;

        BuilderStage(String name) {
            Preconditions.checkNotNull(name, "Criteria name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "Criteria name cannot be empty");
            this.name = name;
        }

        public BuilderStage sql(String sql) {
            Preconditions.checkNotNull(sql, "SQL cannot be null");
            this.sql = sql;
            return this;
        }

        public BuilderStage condition(Predicate<BaseContext<?>> condition) {
            this.condition = condition;
            return this;
        }

        public BuilderStage description(String description) {
            this.description = description;
            return this;
        }

        public BuilderStage isFindByKey(boolean isFindByKey) {
            this.isFindByKey = isFindByKey;
            return this;
        }

        public CriteriaDef build() {
            Preconditions.checkNotNull(sql, "SQL is required for non-dynamic criteria");
            return new CriteriaDef(this);
        }
    }

    public boolean hasCondition() {
        return condition != null;
    }

}