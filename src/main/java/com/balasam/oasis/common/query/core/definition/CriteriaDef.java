package com.balasam.oasis.common.query.core.definition;

import com.balasam.oasis.common.query.core.execution.QueryContext;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.Value;

/**
 * Immutable criteria definition for dynamic SQL conditions
 * Uses builder pattern for construction
 */
@Value
public class CriteriaDef {
    String name;
    String sql; // SQL fragment with named parameters
    Predicate<QueryContext> condition; // When to apply this criteria
    Set<String> bindParams; // Required bind parameters
    Function<Object, Object> processor; // Process context before applying
    boolean dynamic; // Generated at runtime
    Function<Object, String> generator; // Generate SQL dynamically
    boolean securityRelated; // Is this a security filter
    String description;
    String appliedReason; // Why this criteria was applied
    int priority; // Order of application (lower = earlier)
    boolean isFindByKey; // Is this a findByKey criteria (returns single object)
    
    // Private constructor - only accessible through builder
    private CriteriaDef(BuilderStage builder) {
        this.name = builder.name;
        this.sql = builder.sql;
        this.condition = builder.condition;
        this.bindParams = ImmutableSet.copyOf(builder.bindParams);
        this.processor = builder.processor;
        this.dynamic = builder.dynamic;
        this.generator = builder.generator;
        this.securityRelated = builder.securityRelated;
        this.description = builder.description;
        this.appliedReason = builder.appliedReason;
        this.priority = builder.priority;
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
        private Predicate<QueryContext> condition;
        private final Set<String> bindParams = new HashSet<>();
        private Function<Object, Object> processor;
        private boolean dynamic = false;
        private Function<Object, String> generator;
        private boolean securityRelated = false;
        private String description;
        private String appliedReason;
        private int priority = 0;
        private boolean isFindByKey = false;
        
        BuilderStage(String name) {
            Preconditions.checkNotNull(name, "Criteria name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "Criteria name cannot be empty");
            this.name = name;
        }
        
        public BuilderStage sql(String sql) {
            Preconditions.checkNotNull(sql, "SQL cannot be null");
            this.sql = sql;
            // Extract bind parameters from SQL
            extractBindParams(sql);
            return this;
        }
        
        public BuilderStage condition(Predicate<QueryContext> condition) {
            this.condition = condition;
            return this;
        }
        
        public BuilderStage bindParams(String... params) {
            for (String param : params) {
                this.bindParams.add(param);
            }
            return this;
        }
        
        public BuilderStage processor(Function<Object, Object> processor) {
            this.processor = processor;
            return this;
        }
        
        public BuilderStage dynamic(boolean dynamic) {
            this.dynamic = dynamic;
            return this;
        }
        
        public BuilderStage generator(Function<Object, String> generator) {
            this.generator = generator;
            this.dynamic = true; // Auto-set dynamic when generator is provided
            return this;
        }
        
        public BuilderStage securityRelated(boolean securityRelated) {
            this.securityRelated = securityRelated;
            return this;
        }
        
        public BuilderStage description(String description) {
            this.description = description;
            return this;
        }
        
        public BuilderStage appliedReason(String appliedReason) {
            this.appliedReason = appliedReason;
            return this;
        }
        
        public BuilderStage priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public BuilderStage isFindByKey(boolean isFindByKey) {
            this.isFindByKey = isFindByKey;
            return this;
        }
        
        private void extractBindParams(String sql) {
            // Simple regex to extract :paramName from SQL
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":(\\w+)");
            java.util.regex.Matcher matcher = pattern.matcher(sql);
            while (matcher.find()) {
                bindParams.add(matcher.group(1));
            }
        }
        
        public CriteriaDef build() {
            // Validate
            if (!dynamic) {
                Preconditions.checkNotNull(sql, "SQL is required for non-dynamic criteria");
            }
            if (dynamic && generator == null) {
                Preconditions.checkNotNull(sql, "Either SQL or generator is required for dynamic criteria");
            }
            
            return new CriteriaDef(this);
        }
    }
    
    // Helper methods
    public boolean hasProcessor() {
        return processor != null;
    }
    
    public boolean hasGenerator() {
        return generator != null;
    }
    
    public boolean hasCondition() {
        return condition != null;
    }
    
    public boolean requiresParams() {
        return bindParams != null && !bindParams.isEmpty();
    }
}