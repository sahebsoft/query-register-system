package com.balsam.oasis.common.registry.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.balsam.oasis.common.registry.domain.definition.PlsqlParamDef;
import com.balsam.oasis.common.registry.domain.processor.PlsqlPostProcessor;
import com.balsam.oasis.common.registry.domain.processor.PlsqlPreProcessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlsqlDefinitionBuilder {
    private final String name;
    private final String plsql;
    private final Map<String, PlsqlParamDef<?>> parameters;
    private final List<PlsqlPreProcessor> preProcessors;
    private final List<PlsqlPostProcessor> postProcessors;

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private String plsql;
        private final Map<String, PlsqlParamDef<?>> parameters = new LinkedHashMap<>();
        private final List<PlsqlPreProcessor> preProcessors = new ArrayList<>();
        private final List<PlsqlPostProcessor> postProcessors = new ArrayList<>();

        public Builder(String name) {
            Preconditions.checkNotNull(name, "PL/SQL name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "PL/SQL name cannot be empty");
            this.name = name;
        }

        public Builder plsql(String plsql) {
            Preconditions.checkNotNull(plsql, "PL/SQL cannot be null");
            this.plsql = plsql;
            return this;
        }

        public Builder parameter(PlsqlParamDef<?> param) {
            Preconditions.checkNotNull(param, "Parameter cannot be null");
            if (this.parameters.containsKey(param.name())) {
                throw new IllegalStateException(String.format(
                        "Duplicate parameter definition: Parameter '%s' is already defined in this PL/SQL block",
                        param.name()));
            }
            this.parameters.put(param.name(), param);
            return this;
        }

        public Builder param(PlsqlParamDef<?> param) {
            return parameter(param);
        }

        public Builder preProcessor(PlsqlPreProcessor processor) {
            Preconditions.checkNotNull(processor, "PlsqlPreProcessor cannot be null");
            this.preProcessors.add(processor);
            return this;
        }

        public Builder postProcessor(PlsqlPostProcessor processor) {
            Preconditions.checkNotNull(processor, "PlsqlPostProcessor cannot be null");
            this.postProcessors.add(processor);
            return this;
        }


        public PlsqlDefinitionBuilder build() {
            Preconditions.checkNotNull(plsql, "PL/SQL is required");
            Preconditions.checkArgument(!plsql.trim().isEmpty(), "PL/SQL cannot be empty");

            return new PlsqlDefinitionBuilder(
                    name,
                    plsql,
                    ImmutableMap.copyOf(parameters),
                    ImmutableList.copyOf(preProcessors),
                    ImmutableList.copyOf(postProcessors)
            );
        }
    }

    // Helper methods
    public boolean hasParameters() {
        return parameters != null && !parameters.isEmpty();
    }

    public boolean hasPreProcessors() {
        return preProcessors != null && !preProcessors.isEmpty();
    }

    public boolean hasPostProcessors() {
        return postProcessors != null && !postProcessors.isEmpty();
    }

    @SuppressWarnings("rawtypes")
    public PlsqlParamDef getParameter(String name) {
        return parameters.get(name);
    }
}