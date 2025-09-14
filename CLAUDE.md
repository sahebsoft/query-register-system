# RULES
- THIS FILE IS THE SOURCE OF TRUTH - IT WILL BE AUTO UPDATED USING HOOKS
- YOU DO NOT NEED TO READ JAVA FILES IN THIS LIB - ALL FILES ARE HERE AND AUTO UPDATED
- NO NEED TO MAINTAIN BACKWARD COMPATIBILITY - IT'S A NEW LIB, NO ONE USES IT YET
- FOLLOW SOLID PRINCIPLE , MY GOAL IS TO MAKE THIS LIB SMALLER WITHOUT BREAK ANY FEATURE
- After each feature you code , run spring boot app using mvnw , and test rest api manually 

# Java Source Code Files

This document contains all Java source files from the project.

## Table of Contents

- [src/main/java/com/balsam/oasis/common/registry/QueryRegistrationApplication.java](#src-main-java-com-balsam-oasis-common-registry-queryregistrationapplication-java)
- [src/main/java/com/balsam/oasis/common/registry/builder/QueryDefinitionBuilder.java](#src-main-java-com-balsam-oasis-common-registry-builder-querydefinitionbuilder-java)
- [src/main/java/com/balsam/oasis/common/registry/config/QueryConfiguration.java](#src-main-java-com-balsam-oasis-common-registry-config-queryconfiguration-java)
- [src/main/java/com/balsam/oasis/common/registry/config/QueryProperties.java](#src-main-java-com-balsam-oasis-common-registry-config-queryproperties-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/common/Pagination.java](#src-main-java-com-balsam-oasis-common-registry-domain-common-pagination-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/common/QueryData.java](#src-main-java-com-balsam-oasis-common-registry-domain-common-querydata-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/common/SqlResult.java](#src-main-java-com-balsam-oasis-common-registry-domain-common-sqlresult-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/definition/AttributeDef.java](#src-main-java-com-balsam-oasis-common-registry-domain-definition-attributedef-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/definition/CacheConfig.java](#src-main-java-com-balsam-oasis-common-registry-domain-definition-cacheconfig-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/definition/CriteriaDef.java](#src-main-java-com-balsam-oasis-common-registry-domain-definition-criteriadef-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/definition/FilterOp.java](#src-main-java-com-balsam-oasis-common-registry-domain-definition-filterop-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/definition/ParamDef.java](#src-main-java-com-balsam-oasis-common-registry-domain-definition-paramdef-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/definition/SortDir.java](#src-main-java-com-balsam-oasis-common-registry-domain-definition-sortdir-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/exception/QueryException.java](#src-main-java-com-balsam-oasis-common-registry-domain-exception-queryexception-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/execution/QueryContext.java](#src-main-java-com-balsam-oasis-common-registry-domain-execution-querycontext-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/execution/QueryExecution.java](#src-main-java-com-balsam-oasis-common-registry-domain-execution-queryexecution-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/metadata/QueryMetadata.java](#src-main-java-com-balsam-oasis-common-registry-domain-metadata-querymetadata-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/processor/AttributeFormatter.java](#src-main-java-com-balsam-oasis-common-registry-domain-processor-attributeformatter-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/processor/Calculator.java](#src-main-java-com-balsam-oasis-common-registry-domain-processor-calculator-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/processor/ParamProcessor.java](#src-main-java-com-balsam-oasis-common-registry-domain-processor-paramprocessor-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/processor/PostProcessor.java](#src-main-java-com-balsam-oasis-common-registry-domain-processor-postprocessor-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/processor/PreProcessor.java](#src-main-java-com-balsam-oasis-common-registry-domain-processor-preprocessor-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/processor/QueryProcessor.java](#src-main-java-com-balsam-oasis-common-registry-domain-processor-queryprocessor-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/processor/RowProcessor.java](#src-main-java-com-balsam-oasis-common-registry-domain-processor-rowprocessor-java)
- [src/main/java/com/balsam/oasis/common/registry/domain/select/SelectItem.java](#src-main-java-com-balsam-oasis-common-registry-domain-select-selectitem-java)
- [src/main/java/com/balsam/oasis/common/registry/engine/query/QueryExecutorImpl.java](#src-main-java-com-balsam-oasis-common-registry-engine-query-queryexecutorimpl-java)
- [src/main/java/com/balsam/oasis/common/registry/engine/query/QueryRegistryImpl.java](#src-main-java-com-balsam-oasis-common-registry-engine-query-queryregistryimpl-java)
- [src/main/java/com/balsam/oasis/common/registry/engine/query/QueryRow.java](#src-main-java-com-balsam-oasis-common-registry-engine-query-queryrow-java)
- [src/main/java/com/balsam/oasis/common/registry/engine/query/QueryRowMapperImpl.java](#src-main-java-com-balsam-oasis-common-registry-engine-query-queryrowmapperimpl-java)
- [src/main/java/com/balsam/oasis/common/registry/engine/query/QuerySqlBuilder.java](#src-main-java-com-balsam-oasis-common-registry-engine-query-querysqlbuilder-java)
- [src/main/java/com/balsam/oasis/common/registry/example/OracleHRQueryConfig.java](#src-main-java-com-balsam-oasis-common-registry-example-oraclehrqueryconfig-java)
- [src/main/java/com/balsam/oasis/common/registry/service/QueryService.java](#src-main-java-com-balsam-oasis-common-registry-service-queryservice-java)
- [src/main/java/com/balsam/oasis/common/registry/util/QueryUtils.java](#src-main-java-com-balsam-oasis-common-registry-util-queryutils-java)
- [src/main/java/com/balsam/oasis/common/registry/web/builder/QueryResponseBuilder.java](#src-main-java-com-balsam-oasis-common-registry-web-builder-queryresponsebuilder-java)
- [src/main/java/com/balsam/oasis/common/registry/web/controller/QueryController.java](#src-main-java-com-balsam-oasis-common-registry-web-controller-querycontroller-java)
- [src/main/java/com/balsam/oasis/common/registry/web/controller/SelectController.java](#src-main-java-com-balsam-oasis-common-registry-web-controller-selectcontroller-java)
- [src/main/java/com/balsam/oasis/common/registry/web/dto/request/QueryRequest.java](#src-main-java-com-balsam-oasis-common-registry-web-dto-request-queryrequest-java)
- [src/main/java/com/balsam/oasis/common/registry/web/dto/request/QueryRequestBody.java](#src-main-java-com-balsam-oasis-common-registry-web-dto-request-queryrequestbody-java)
- [src/main/java/com/balsam/oasis/common/registry/web/dto/response/QueryResponse.java](#src-main-java-com-balsam-oasis-common-registry-web-dto-response-queryresponse-java)
- [src/main/java/com/balsam/oasis/common/registry/web/formatter/ResponseFormatter.java](#src-main-java-com-balsam-oasis-common-registry-web-formatter-responseformatter-java)
- [src/main/java/com/balsam/oasis/common/registry/web/parser/QueryRequestParser.java](#src-main-java-com-balsam-oasis-common-registry-web-parser-queryrequestparser-java)

---

## src/main/java/com/balsam/oasis/common/registry/QueryRegistrationApplication.java

```java
@SpringBootApplication
public class QueryRegistrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryRegistrationApplication.class, args);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/builder/QueryDefinitionBuilder.java

```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryDefinitionBuilder {
    private final String name;
    private final String description;
    private final String sql;
    private final Map<String, ParamDef<?>> parameters;
    private final Map<String, CriteriaDef> criteria;
    private final List<PreProcessor> preProcessors;
    private final List<RowProcessor> rowProcessors;
    private final List<PostProcessor> postProcessors;
    private final CacheConfig cacheConfig;
    private final Integer defaultPageSize;
    private final Integer maxPageSize;
    private final boolean auditEnabled;
    private final boolean metricsEnabled;
    private final Integer queryTimeout;
    private final Map<String, AttributeDef<?>> attributes;
    private final boolean paginationEnabled;
    private final Integer fetchSize;
    private final String valueAttribute;
    private final String labelAttribute;
    private final boolean selectMode;
    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }
    public boolean isSelectMode() {
        return selectMode;
    }
    public boolean hasLabelAttribute() {
        return labelAttribute != null;
    }
    public boolean hasValueAttribute() {
        return valueAttribute != null;
    }
    public AttributeDef<?> getAttribute(String name) {
        return attributes.get(name);
    }
    public ParamDef<?> getParam(String name) {
        return parameters.get(name);
    }
    public boolean hasParams() {
        return parameters != null && !parameters.isEmpty();
    }
    public boolean hasCriteria() {
        return criteria != null && !criteria.isEmpty();
    }
    public boolean hasCacheConfig() {
        return cacheConfig != null && cacheConfig.isEnabled();
    }
    public boolean hasPreProcessors() {
        return preProcessors != null && !preProcessors.isEmpty();
    }
    public boolean hasRowProcessors() {
        return rowProcessors != null && !rowProcessors.isEmpty();
    }
    public boolean hasPostProcessors() {
        return postProcessors != null && !postProcessors.isEmpty();
    }
    public static Builder builder(String name) {
        return new Builder(name);
    }
    public static class Builder {
        protected final String name;
        protected String sql;
        protected String description;
        protected final Map<String, AttributeDef<?>> attributes = new LinkedHashMap<>();
        protected final Map<String, ParamDef<?>> parameters = new LinkedHashMap<>();
        protected final Map<String, CriteriaDef> criteria = new LinkedHashMap<>();
        protected final List<PreProcessor> preProcessors = new ArrayList<>();
        protected final List<RowProcessor> rowProcessors = new ArrayList<>();
        protected final List<PostProcessor> postProcessors = new ArrayList<>();
        protected Boolean cacheEnabled = false;
        protected Duration cacheTTL;
        protected Function<Object, String> cacheKeyGenerator;
        protected Integer defaultPageSize = 50;
        protected Integer maxPageSize = 1000;
        protected Boolean paginationEnabled = true;
        protected Integer fetchSize = null; 
        protected Boolean auditEnabled = true;
        protected Boolean metricsEnabled = true;
        protected Integer queryTimeout;
        protected String valueAttribute;
        protected String labelAttribute;
        protected Boolean selectMode = false;
        protected Builder(String name) {
            Preconditions.checkNotNull(name, "Query name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "Query name cannot be empty");
            this.name = name;
        }
        public Builder sql(String sql) {
            Preconditions.checkNotNull(sql, "SQL cannot be null");
            this.sql = sql;
            return this;
        }
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        public Builder attribute(AttributeDef<?> attribute) {
            Preconditions.checkNotNull(attribute, "Attribute cannot be null");
            if (this.attributes.containsKey(attribute.name())) {
                throw new IllegalStateException(String.format(
                        "Duplicate attribute definition: Attribute '%s' is already defined in this query",
                        attribute.name()));
            }
            this.attributes.put(attribute.name(), attribute);
            return this;
        }
        public Builder parameter(ParamDef<?> param) {
            Preconditions.checkNotNull(param, "Parameter cannot be null");
            if (this.parameters.containsKey(param.name())) {
                throw new IllegalStateException(String.format(
                        "Duplicate parameter definition: Parameter '%s' is already defined in this query",
                        param.name()));
            }
            this.parameters.put(param.name(), param);
            return this;
        }
        public Builder criteria(CriteriaDef criteria) {
            Preconditions.checkNotNull(criteria, "Criteria cannot be null");
            if (this.criteria.containsKey(criteria.name())) {
                throw new IllegalStateException(String.format(
                        "Duplicate criteria definition: Criteria '%s' is already defined in this query",
                        criteria.name()));
            }
            this.criteria.put(criteria.name(), criteria);
            return this;
        }
        public Builder preProcessor(PreProcessor processor) {
            Preconditions.checkNotNull(processor, "PreProcessor cannot be null");
            this.preProcessors.add(context -> {
                processor.process((com.balsam.oasis.common.registry.domain.execution.QueryContext) context);
            });
            return this;
        }
        public Builder rowProcessor(RowProcessor processor) {
            Preconditions.checkNotNull(processor, "RowProcessor cannot be null");
            this.rowProcessors.add(processor);
            return this;
        }
        public Builder postProcessor(PostProcessor processor) {
            Preconditions.checkNotNull(processor, "PostProcessor cannot be null");
            this.postProcessors.add(processor);
            return this;
        }
        public Builder cache(Boolean enabled) {
            this.cacheEnabled = enabled;
            return this;
        }
        public Builder cacheTTL(Duration ttl) {
            this.cacheTTL = ttl;
            this.cacheEnabled = true;
            return this;
        }
        public Builder cacheKey(Function<Object, String> keyGenerator) {
            this.cacheKeyGenerator = keyGenerator;
            return this;
        }
        public Builder defaultPageSize(Integer size) {
            Preconditions.checkArgument(size > 0, "Page size must be positive");
            this.defaultPageSize = size;
            return this;
        }
        public Builder maxPageSize(Integer size) {
            Preconditions.checkArgument(size > 0, "Max page size must be positive");
            this.maxPageSize = size;
            return this;
        }
        public Builder paginationEnabled(Boolean enabled) {
            this.paginationEnabled = enabled;
            return this;
        }
        public Builder fetchSize(Integer size) {
            if (size != null && size < 0 && size != -1) {
                throw new IllegalArgumentException("Fetch size must be -1, 0, or positive");
            }
            this.fetchSize = size;
            return this;
        }
        public Builder auditEnabled(Boolean enabled) {
            this.auditEnabled = enabled;
            return this;
        }
        public Builder metricsEnabled(Boolean enabled) {
            this.metricsEnabled = enabled;
            return this;
        }
        public Builder queryTimeout(Integer seconds) {
            this.queryTimeout = seconds;
            return this;
        }
        public Builder asSelect(String valueAttribute, String labelAttribute) {
            Preconditions.checkNotNull(valueAttribute, "Value attribute cannot be null");
            Preconditions.checkNotNull(labelAttribute, "Label attribute cannot be null");
            this.selectMode = true;
            this.valueAttribute = valueAttribute;
            this.labelAttribute = labelAttribute;
            return this;
        }
        public Builder valueAttribute(String attribute) {
            this.valueAttribute = attribute;
            return this;
        }
        public Builder labelAttribute(String attribute) {
            this.labelAttribute = attribute;
            return this;
        }
        public QueryDefinitionBuilder build() {
            validate();
            CacheConfig cacheConfig = null;
            if (cacheEnabled) {
                cacheConfig = CacheConfig.builder()
                        .enabled(true)
                        .ttl(cacheTTL != null ? cacheTTL : Duration.ofMinutes(5))
                        .keyGenerator(cacheKeyGenerator)
                        .build();
            }
            QueryDefinitionBuilder queryDef = new QueryDefinitionBuilder(
                    name,
                    description,
                    sql,
                    ImmutableMap.copyOf(parameters),
                    ImmutableMap.copyOf(criteria),
                    ImmutableList.copyOf(preProcessors),
                    ImmutableList.copyOf(rowProcessors),
                    ImmutableList.copyOf(postProcessors),
                    cacheConfig,
                    defaultPageSize,
                    maxPageSize,
                    auditEnabled,
                    metricsEnabled,
                    queryTimeout,
                    ImmutableMap.copyOf(attributes),
                    paginationEnabled,
                    fetchSize,
                    valueAttribute,
                    labelAttribute,
                    selectMode);
            QueryUtils.validateQuery(queryDef);
            return queryDef;
        }
        private void validate() {
            Preconditions.checkNotNull(sql, "SQL is required");
            Preconditions.checkArgument(!sql.trim().isEmpty(), "SQL cannot be empty");
            validateCriteriaPlaceholders();
            validateParamReferences();
        }
        private void validateCriteriaPlaceholders() {
            for (CriteriaDef criteriaDef : criteria.values()) {
                String placeholder = "--" + criteriaDef.name();
                if (!sql.contains(placeholder)) {
                    throw new IllegalArgumentException(
                            "SQL does not contain placeholder for criteria: " + placeholder);
                }
            }
        }
        private void validateParamReferences() {
            for (ParamDef<?> paramDef : parameters.values()) {
                if (paramDef.required()) {
                    Boolean referenced = sql.contains(":" + paramDef.name());
                    if (!referenced) {
                        throw new IllegalArgumentException(
                                "Required parameter not referenced in SQL: " + paramDef.name());
                    }
                }
            }
        }
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/config/QueryConfiguration.java

```java
@Configuration
@EnableConfigurationProperties(QueryProperties.class)
public class QueryConfiguration {
    @Bean
    QuerySqlBuilder sqlBuilder() {
        return new QuerySqlBuilder();
    }
    @Bean
    QueryRegistryImpl queryRegistry() {
        return new QueryRegistryImpl();
    }
    @Bean
    QueryExecutorImpl queryExecutor(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder,
            QueryRegistryImpl queryRegistry,
            QueryProperties properties) {
        return new QueryExecutorImpl(jdbcTemplate, sqlBuilder, queryRegistry);
    }
    @Bean
    QueryRequestParser queryRequestParser() {
        return new QueryRequestParser();
    }
    @Bean
    QueryResponseBuilder queryResponseBuilder() {
        return new QueryResponseBuilder();
    }
    @Bean
    QueryService queryService(QueryExecutorImpl queryExecutor, QueryRegistryImpl queryRegistry) {
        return new QueryService(queryExecutor, queryRegistry);
    }
    @Bean
    QueryController queryController(
            QueryService queryService,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        return new QueryController(queryService, requestParser, responseBuilder);
    }
    @Bean
    SelectController selectController(
            QueryService queryService,
            QueryResponseBuilder responseBuilder,
            QueryRequestParser requestParser) {
        return new SelectController(queryService, responseBuilder, requestParser);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/config/QueryProperties.java

```java
@Data
@ConfigurationProperties(prefix = "query.registration")
public class QueryProperties {
    private RestProperties rest = new RestProperties();
    private JdbcProperties jdbc = new JdbcProperties();
    @Data
    public static class RestProperties {
        private String prefix = "/api/query";
        private int defaultPageSize = 50;
        private int maxPageSize = 1000;
    }
    @Data
    public static class JdbcProperties {
        private int fetchSize = 100;
        private Duration queryTimeout = Duration.ofSeconds(30);
        private boolean enableSqlLogging = true;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/common/Pagination.java

```java
@Data
@Builder
public class Pagination {
    private int start;
    private int end;
    private int total;
    private boolean hasNext;
    private boolean hasPrevious;
    public int getPageSize() {
        return end - start;
    }
    public int getOffset() {
        return start;
    }
    public int getLimit() {
        return getPageSize();
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/common/QueryData.java

```java
@Value
@Builder(toBuilder = true)
public class QueryData {
    @Builder.Default
    List<QueryRow> rows = ImmutableList.of();
    QueryMetadata metadata;
    QueryContext context;
    Long executionTimeMs;
    String sql;
    Map<String, Object> params;
    Map<String, Object> rowData;
    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }
    public int size() {
        return rows != null ? rows.size() : 0;
    }
    public int getCount() {
        if (metadata != null && metadata.getPagination() != null) {
            return metadata.getPagination().getTotal();
        }
        return size();
    }
    public boolean hasMetadata() {
        return metadata != null;
    }
    public QueryRow getFirstRow() {
        if (!isEmpty()) {
            return rows.get(0);
        }
        return null;
    }
    public List<Map<String, Object>> getData() {
        if (rows != null && !rows.isEmpty()) {
            return rows.stream()
                    .map(QueryRow::toMap)
                    .collect(ImmutableList.toImmutableList());
        }
        return ImmutableList.of();
    }
    public boolean isSuccess() {
        return true;
    }
    public String getSql() {
        return sql;
    }
    public Map<String, Object> getParams() {
        return params;
    }
    public Object get(String key) {
        if (rowData == null) return null;
        Object value = rowData.get(key);
        if (value != null) {
            return value;
        }
        return rowData.get(key.toUpperCase());
    }
    public Object getRaw(String columnName) {
        if (rowData == null) return null;
        return rowData.get(columnName.toUpperCase());
    }
    public void set(String key, Object value) {
        if (rowData == null) {
            throw new IllegalStateException("Cannot set values on QueryData without row data");
        }
        ((Map<String, Object>) rowData).put(key, value);
    }
    public Map<String, Object> toMap() {
        return rowData != null ? new HashMap<>(rowData) : new HashMap<>();
    }
    public boolean has(String key) {
        return rowData != null && (rowData.containsKey(key) || rowData.containsKey(key.toUpperCase()));
    }
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }
    public Integer getInteger(String key) {
        return get(key, Integer.class);
    }
    public Long getLong(String key) {
        return get(key, Long.class);
    }
    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }
    public static QueryData asResult(List<QueryRow> rows, QueryContext context, QueryMetadata metadata, Long executionTime) {
        return QueryData.builder()
                .rows(rows)
                .context(context)
                .metadata(metadata)
                .executionTimeMs(executionTime)
                .build();
    }
    public static QueryData asSql(String sql, Map<String, Object> params) {
        return QueryData.builder()
                .sql(sql)
                .params(params)
                .build();
    }
    public static QueryData asRow(Map<String, Object> data, QueryContext context) {
        return QueryData.builder()
                .rowData(new HashMap<>(data))
                .context(context)
                .build();
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/common/SqlResult.java

```java
public class SqlResult {
    private final String sql;
    private final Map<String, Object> params;
    public SqlResult(String sql, Map<String, Object> params) {
        this.sql = sql;
        this.params = params;
    }
    public String getSql() {
        return sql;
    }
    public Map<String, Object> getParams() {
        return params;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/definition/AttributeDef.java

```java
@Builder(toBuilder = true)
public record AttributeDef<T>(
        String name,
        Class<T> type,
        String aliasName,
        boolean primaryKey,
        boolean virtual,
        boolean selected,
        AttributeFormatter<T> formatter,
        Calculator<T> calculator,
        Function<Object, Boolean> securityRule,
        String description,
        String label,
        String labelKey,
        String width,
        String flex,
        String alignment,
        String headerStyle,
        boolean visible) {
    @SuppressWarnings("unchecked")
    public AttributeDef {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Attribute name is required");
        }
        type = type != null ? type : (Class<T>) Object.class;
        alignment = alignment != null ? alignment : "left";
        aliasName = (!virtual && aliasName == null) ? name : aliasName;
        if (virtual) {
            if (calculator == null) {
                throw new IllegalStateException("Virtual attribute '" + name + "' requires calculator");
            }
            if (primaryKey) {
                throw new IllegalStateException("Virtual attribute '" + name + "' cannot be primary key");
            }
            aliasName = null;
        } else if (calculator != null) {
            throw new IllegalStateException("Regular attribute '" + name + "' should not have calculator");
        }
    }
    @SuppressWarnings("unchecked")
    public static <T> AttributeDefBuilder<T> of(String name) {
        return (AttributeDefBuilder<T>) builder()
                .name(name)
                .type(Object.class)
                .aliasName(name)
                .selected(true)
                .visible(true)
                .alignment("left");
    }
    public static <T> AttributeDefBuilder<T> of(String name, Class<T> type) {
        return AttributeDef.<T>builder()
                .name(name).type(type).aliasName(name)
                .selected(true).visible(true);
    }
    public static <T> AttributeDefBuilder<T> name(String name) {
        return of(name);
    }
    public static <T> AttributeDefBuilder<T> name(String name, Class<T> type) {
        return of(name, type);
    }
    public boolean isSecured() {
        return securityRule != null;
    }
    public boolean hasFormatter() {
        return formatter != null;
    }
    public boolean hasCalculator() {
        return calculator != null;
    }
    public boolean isVirtual() {
        return virtual;
    }
    public boolean filterable() {
        return !virtual;
    }
    public boolean sortable() {
        return !virtual;
    }
    public static class AttributeDefBuilder<T> {
        public AttributeDefBuilder<T> calculated(Calculator<T> calc) {
            return calculator(calc).virtual(true).aliasName(null);
        }
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/definition/CacheConfig.java

```java
@Value
@Builder(toBuilder = true)
public class CacheConfig {
    @Builder.Default
    boolean enabled = false;
    @Builder.Default
    Duration ttl = Duration.ofMinutes(5);
    Function<Object, String> keyGenerator;
    public boolean hasKeyGenerator() {
        return keyGenerator != null;
    }
    public boolean isEnabled() {
        return enabled;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/definition/CriteriaDef.java

```java
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
```

---

## src/main/java/com/balsam/oasis/common/registry/domain/definition/FilterOp.java

```java
public enum FilterOp {
    EQUALS("=", "eq"),
    NOT_EQUALS("!=", "ne"),
    GREATER_THAN(">", "gt"),
    GREATER_THAN_OR_EQUAL(">=", "gte"),
    LESS_THAN("<", "lt"),
    LESS_THAN_OR_EQUAL("<=", "lte"),
    LIKE("LIKE", "like"),
    NOT_LIKE("NOT LIKE", "notlike"),
    IN("IN", "in"),
    NOT_IN("NOT IN", "notin"),
    BETWEEN("BETWEEN", "between"),
    IS_NULL("IS NULL", "null"),
    IS_NOT_NULL("IS NOT NULL", "notnull"),
    CONTAINS("CONTAINS", "contains"),
    STARTS_WITH("STARTS WITH", "startswith"),
    ENDS_WITH("ENDS WITH", "endswith");
    private final String sqlOperator;
    private final String urlShortcut;
    FilterOp(String sqlOperator, String urlShortcut) {
        this.sqlOperator = sqlOperator;
        this.urlShortcut = urlShortcut;
    }
    public String getSqlOperator() {
        return sqlOperator;
    }
    public String getUrlShortcut() {
        return urlShortcut;
    }
    public static FilterOp fromUrlShortcut(String shortcut) {
        for (FilterOp op : values()) {
            if (op.urlShortcut.equalsIgnoreCase(shortcut)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown filter operator shortcut: " + shortcut);
    }
    public boolean requiresValue() {
        return this != IS_NULL && this != IS_NOT_NULL;
    }
    public boolean requiresTwoValues() {
        return this == BETWEEN;
    }
    public boolean supportsMultipleValues() {
        return this == IN || this == NOT_IN;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/definition/ParamDef.java

```java
@Builder
public record ParamDef<T>(
        String name,
        Class<T> type,
        T defaultValue,
        boolean required,
        ParamProcessor<T> processor) {
    public static <T> ParamDefBuilder<T> name(String name) {
        return ParamDef.<T>builder().name(name);
    }
    public static <T> ParamDefBuilder<T> name(String name, Class<T> type) {
        return ParamDef.<T>builder().name(name).type(type);
    }
    public boolean hasProcessor() {
        return processor != null;
    }
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }
    public boolean isValid(T value, QueryContext context) {
        if (value == null) {
            return !required;
        }
        if (hasProcessor()) {
            try {
                T processed = processor.process(value, context);
                return processed != null;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/definition/SortDir.java

```java
public enum SortDir {
    ASC("ASC", "asc"),
    DESC("DESC", "desc");
    private final String sql;
    private final String urlParam;
    SortDir(String sql, String urlParam) {
        this.sql = sql;
        this.urlParam = urlParam;
    }
    public String getSql() {
        return sql;
    }
    public String getUrlParam() {
        return urlParam;
    }
    public static SortDir fromUrlParam(String param) {
        for (SortDir dir : values()) {
            if (dir.urlParam.equalsIgnoreCase(param)) {
                return dir;
            }
        }
        return ASC; 
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/exception/QueryException.java

```java
public class QueryException extends RuntimeException {
    public enum ErrorCode {
        QUERY_NOT_FOUND("QRY001", "Query not found"),
        EXECUTION_ERROR("QRY002", "Query execution error"),
        TIMEOUT("QRY003", "Query timeout"),
        SECURITY_VIOLATION("QRY004", "Security violation"),
        DEFINITION_ERROR("QRY005", "Query definition error"),
        VALIDATION_ERROR("QRY006", "Validation error"),
        SQL_ERROR("QRY007", "SQL error"),
        PARAMETER_ERROR("QRY008", "Parameter error");
        private final String code;
        private final String description;
        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        public String getCode() {
            return code;
        }
        public String getDescription() {
            return description;
        }
    }
    private final String queryName;
    private final String errorCode;
    public QueryException(String message) {
        super(message);
        this.queryName = null;
        this.errorCode = null;
    }
    public QueryException(String message, Throwable cause) {
        super(message, cause);
        this.queryName = null;
        this.errorCode = null;
    }
    public QueryException(String queryName, String message) {
        super(formatMessage(queryName, message));
        this.queryName = queryName;
        this.errorCode = null;
    }
    public QueryException(String queryName, String message, Throwable cause) {
        super(formatMessage(queryName, message), cause);
        this.queryName = queryName;
        this.errorCode = null;
    }
    public QueryException(String queryName, String errorCode, String message) {
        super(formatMessage(queryName, message));
        this.queryName = queryName;
        this.errorCode = errorCode;
    }
    public QueryException(String queryName, String errorCode, String message, Throwable cause) {
        super(formatMessage(queryName, message), cause);
        this.queryName = queryName;
        this.errorCode = errorCode;
    }
    public QueryException(ErrorCode errorCode, String message) {
        super(message);
        this.queryName = null;
        this.errorCode = errorCode.getCode();
    }
    public QueryException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.queryName = null;
        this.errorCode = errorCode.getCode();
    }
    public QueryException(String queryName, ErrorCode errorCode, String message) {
        super(formatMessage(queryName, message));
        this.queryName = queryName;
        this.errorCode = errorCode.getCode();
    }
    public QueryException(String queryName, ErrorCode errorCode, String message, Throwable cause) {
        super(formatMessage(queryName, message), cause);
        this.queryName = queryName;
        this.errorCode = errorCode.getCode();
    }
    private static String formatMessage(String queryName, String message) {
        if (queryName != null) {
            return String.format("Query '%s': %s", queryName, message);
        }
        return message;
    }
    public String getQueryName() {
        return queryName;
    }
    public String getErrorCode() {
        return errorCode;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/execution/QueryContext.java

```java
@Data
@Builder
public class QueryContext {
    protected QueryDefinitionBuilder definition;
    @Builder.Default
    protected Map<String, Object> params = new HashMap<>();
    protected Pagination pagination;
    protected Long startTime;
    protected Long endTime;
    @Builder.Default
    protected boolean includeMetadata = true;
    protected Integer totalCount;
    @Builder.Default
    private Map<String, Filter> filters = new LinkedHashMap<>();
    @Builder.Default
    private List<SortSpec> sorts = new ArrayList<>();
    private Object securityContext;
    @Data
    @Builder
    public static class Filter {
        private String attribute;
        private FilterOp operator;
        private Object value;
        private Object value2; 
        private List<Object> values; 
        public boolean requiresTwoValues() {
            return operator == FilterOp.BETWEEN;
        }
        public boolean hasMultipleValues() {
            return operator == FilterOp.IN || operator == FilterOp.NOT_IN;
        }
    }
    @Data
    @Builder
    public static class SortSpec {
        private String attribute;
        private SortDir direction;
    }
    public void addParam(String name, Object value) {
        params.put(name, value);
    }
    public void addFilter(String attribute, FilterOp operator, Object value) {
        filters.put(attribute, Filter.builder()
                .attribute(attribute)
                .operator(operator)
                .value(value)
                .build());
    }
    public void addFilter(String attribute, FilterOp operator, Object value1, Object value2) {
        filters.put(attribute, Filter.builder()
                .attribute(attribute)
                .operator(operator)
                .value(value1)
                .value2(value2)
                .build());
    }
    public void addFilter(String attribute, FilterOp operator, List<Object> values) {
        filters.put(attribute, Filter.builder()
                .attribute(attribute)
                .operator(operator)
                .values(values)
                .build());
    }
    public void addSort(String attribute, SortDir direction) {
        sorts.add(SortSpec.builder()
                .attribute(attribute)
                .direction(direction)
                .build());
    }
    public Object getParam(String name) {
        return params.get(name);
    }
    public boolean hasParam(String name) {
        return params.containsKey(name) && params.get(name) != null;
    }
    public boolean hasPagination() {
        return pagination != null;
    }
    public boolean hasSorts() {
        return sorts != null && !sorts.isEmpty();
    }
    public void startExecution() {
        this.startTime = System.currentTimeMillis();
    }
    public void endExecution() {
        this.endTime = System.currentTimeMillis();
    }
    public long getExecutionTime() {
        return (startTime != null && endTime != null) ? endTime - startTime : 0;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/execution/QueryExecution.java

```java
public class QueryExecution {
    private final QueryDefinitionBuilder definition;
    private final QueryContext context;
    private final QueryExecutorImpl executor;
    public QueryExecution(QueryDefinitionBuilder definition, QueryExecutorImpl executor) {
        this.definition = definition;
        this.executor = executor;
        this.context = QueryContext.builder()
                .definition(definition)
                .build();
    }
    public QueryExecution withParam(String name, Object value) {
        context.addParam(name, value);
        return this;
    }
    public QueryExecution withParams(Map<String, Object> params) {
        params.forEach(context::addParam);
        return this;
    }
    public QueryExecution withFilter(String attributeName, FilterOp op, Object value) {
        context.addFilter(attributeName, op, value);
        return this;
    }
    public QueryExecution withFilter(String attributeName, FilterOp op, Object value1, Object value2) {
        context.addFilter(attributeName, op, value1, value2);
        return this;
    }
    public QueryExecution withFilter(String attributeName, FilterOp op, List<Object> values) {
        context.addFilter(attributeName, op, values);
        return this;
    }
    public QueryExecution withFilters(Map<String, QueryContext.Filter> filters) {
        filters.forEach((key, filter) -> {
            if (filter.hasMultipleValues()) {
                context.addFilter(filter.getAttribute(), filter.getOperator(), filter.getValues());
            } else if (filter.requiresTwoValues()) {
                context.addFilter(filter.getAttribute(), filter.getOperator(),
                        filter.getValue(), filter.getValue2());
            } else {
                context.addFilter(filter.getAttribute(), filter.getOperator(), filter.getValue());
            }
        });
        return this;
    }
    public QueryExecution withSort(String attributeName, SortDir direction) {
        context.addSort(attributeName, direction);
        return this;
    }
    public QueryExecution withSort(List<QueryContext.SortSpec> sorts) {
        sorts.forEach(sort -> context.addSort(sort.getAttribute(), sort.getDirection()));
        return this;
    }
    public QueryExecution withPagination(int start, int end) {
        if (start < 0) {
            start = 0; 
        }
        if (end < start) {
            end = start; 
        }
        int pageSize = end - start;
        if (definition.getMaxPageSize() != null
                && definition.getMaxPageSize() > 0
                && pageSize > definition.getMaxPageSize()) {
            end = start + definition.getMaxPageSize();
        }
        context.setPagination(Pagination.builder()
                .start(start)
                .end(end)
                .build());
        return this;
    }
    public QueryExecution withOffsetLimit(int offset, int limit) {
        if (offset < 0 || limit < 0) {
            offset = 0;
            limit = 0;
        }
        return withPagination(offset, offset + limit);
    }
    public QueryExecution withSecurityContext(Object securityContext) {
        context.setSecurityContext(securityContext);
        return this;
    }
    public QueryExecution includeMetadata(boolean include) {
        context.setIncludeMetadata(include);
        return this;
    }
    public QueryExecution validate() {
        List<String> violations = new ArrayList<>();
        definition.getParameters().forEach((name, paramDef) -> {
            if (!context.hasParam(name)) {
                if (paramDef.hasDefaultValue()) {
                    context.addParam(name, paramDef.defaultValue());
                } else if (!paramDef.required()) {
                    context.addParam(name, null);
                }
            }
            if (paramDef.required() && !context.hasParam(name)) {
                violations.add("Required parameter missing: " + name);
            }
            if (context.hasParam(name)) {
                Object value = context.getParam(name);
                if (paramDef.hasProcessor()) {
                    try {
                        ParamProcessor<?> processor = paramDef.processor();
                        Object processedValue = processor.process(value, context);
                        if (processedValue == null && paramDef.hasDefaultValue()) {
                            context.addParam(name, paramDef.defaultValue());
                        } else {
                            context.addParam(name, processedValue);
                        }
                    } catch (Exception e) {
                        violations.add("Parameter validation/processing failed for " + name + ": " + e.getMessage());
                    }
                } else {
                    if (paramDef.required() && value == null) {
                        violations.add("Parameter validation failed: " + name);
                    }
                }
            }
        });
        context.getFilters().forEach((attribute, filter) -> {
            var attrDef = definition.getAttribute(attribute);
            if (attrDef == null) {
                violations.add("Unknown attribute for filter: " + attribute);
            } else if (!attrDef.filterable()) {
                violations.add("Attribute not filterable: " + attribute);
            }
        });
        context.getSorts().forEach(sort -> {
            var attrDef = definition.getAttribute(sort.getAttribute());
            if (attrDef == null) {
                violations.add("Unknown attribute for sort: " + sort.getAttribute());
            } else if (!attrDef.sortable()) {
                violations.add("Attribute not sortable: " + sort.getAttribute());
            }
        });
        if (context.hasPagination()) {
            var pagination = context.getPagination();
            if (pagination.getPageSize() > definition.getMaxPageSize()) {
                violations.add(String.format("Page size %d exceeds maximum %d",
                        pagination.getPageSize(), definition.getMaxPageSize()));
            }
        }
        if (!violations.isEmpty()) {
            throw new QueryException(definition.getName(),
                    QueryException.ErrorCode.VALIDATION_ERROR,
                    "Validation failed: " + String.join(", ", violations));
        }
        return this;
    }
    public QueryData execute() {
        initializeNonRequiredParams();
        validate();
        return executor.doExecute(context);
    }
    private void initializeNonRequiredParams() {
        definition.getParameters().forEach((name, paramDef) -> {
            if (!context.hasParam(name) && !paramDef.required()) {
                if (paramDef.hasDefaultValue()) {
                    context.addParam(name, paramDef.defaultValue());
                } else {
                    context.addParam(name, null);
                }
            }
        });
    }
    public QueryRow executeSingle() {
        initializeNonRequiredParams();
        validate();
        QueryData result = executor.doExecute(context);
        if (result.getRows().isEmpty()) {
            return null;
        }
        if (result.getRows().size() > 1) {
            throw new QueryException(definition.getName(),
                    QueryException.ErrorCode.VALIDATION_ERROR,
                    String.format("FindByKey query returned %d results, expected 1", result.getRows().size()));
        }
        return result.getRows().get(0);
    }
    public java.util.concurrent.CompletableFuture<QueryData> executeAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(this::execute);
    }
    public java.util.concurrent.CompletableFuture<Object> executeSingleAsync() {
        return java.util.concurrent.CompletableFuture.supplyAsync(this::executeSingle);
    }
}
```

---

## src/main/java/com/balsam/oasis/common/registry/domain/metadata/QueryMetadata.java

```java
@Value
@Builder
public class QueryMetadata {
    PaginationInfo pagination;
    List<AttributeInfo> attributes;
    Long executionTimeMs;
    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {
        int start;
        int end;
        int total;
        Boolean hasNext;
        Boolean hasPrevious;
        int pageSize;
        int pageCount;
        int currentPage;
    }
    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AttributeInfo {
        String name;
        String type;
        Boolean restricted;
        String label;
        String labelKey;
        String width;
        String flex;
        String headerText;
        String headerStyle;
        String alignment;
        Boolean visible;
    }
    public static class MetadataBuilder {
        private final QueryContext context;
        private final QueryData result;
        public MetadataBuilder(QueryContext context, QueryData result) {
            this.context = context;
            this.result = result;
        }
        public QueryMetadata build() {
            return QueryMetadata.builder()
                    .pagination(buildPaginationInfo())
                    .attributes(buildAttributesInfo())
                    .executionTimeMs(context.getExecutionTime())
                    .build();
        }
        private PaginationInfo buildPaginationInfo() {
            if (!context.hasPagination()) {
                return null;
            }
            Pagination pagination = context.getPagination();
            Integer totalCount = context.getTotalCount();
            int total = (totalCount != null) ? totalCount : result.getRows().size();
            int pageSize = pagination.getPageSize();
            int pageCount = (total + pageSize - 1) / pageSize;
            int currentPage = pagination.getStart() / pageSize + 1;
            boolean hasNext = pagination.getEnd() < total;
            boolean hasPrevious = pagination.getStart() > 0;
            return PaginationInfo.builder()
                    .start(pagination.getStart())
                    .end(pagination.getEnd())
                    .total(total)
                    .hasNext(hasNext)
                    .hasPrevious(hasPrevious)
                    .pageSize(pageSize)
                    .pageCount(pageCount)
                    .currentPage(currentPage)
                    .build();
        }
        private List<AttributeInfo> buildAttributesInfo() {
            List<AttributeInfo> metadata = new ArrayList<>();
            QueryDefinitionBuilder definition = context.getDefinition();
            for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
                String attrName = entry.getKey();
                AttributeDef<?> attr = entry.getValue();
                if (!attr.selected()) {
                    continue;
                }
                Boolean restricted = null;
                if (attr.isSecured() && context.getSecurityContext() != null) {
                    Boolean allowed = attr.securityRule().apply(context.getSecurityContext());
                    if (!Boolean.TRUE.equals(allowed)) {
                        restricted = true;
                    }
                }
                AttributeInfo attrInfo = AttributeInfo
                        .builder()
                        .name(attrName)
                        .type(attr.type() != null ? attr.type().getSimpleName() : "Object")
                        .restricted(restricted)
                        .label(attr.label())
                        .labelKey(attr.labelKey())
                        .width(attr.width())
                        .flex(attr.flex())
                        .headerText(attr.label())
                        .alignment(attr.alignment())
                        .headerStyle(attr.headerStyle())
                        .visible(attr.visible())
                        .build();
                metadata.add(attrInfo);
            }
            return metadata;
        }
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/processor/AttributeFormatter.java

```java
@FunctionalInterface
public interface AttributeFormatter<T> extends QueryProcessor {
    String format(T value);
    @Override
    @SuppressWarnings("unchecked")
    default Object process(Object input, QueryContext context) {
        if (input == null) return null;
        return format((T) input);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/processor/Calculator.java

```java
@FunctionalInterface
public interface Calculator<T> extends QueryProcessor {
    T calculate(QueryRow row, QueryContext context);
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
    static <T> Calculator<T> withFullContext(AggregateCalculator<T> aggregateCalc) {
        return new Calculator<T>() {
            @Override
            public T calculate(QueryRow row, QueryContext context) {
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
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/processor/ParamProcessor.java

```java
@FunctionalInterface
public interface ParamProcessor<T> extends QueryProcessor {
    T process(Object value, QueryContext context);
}
```

---

## src/main/java/com/balsam/oasis/common/registry/domain/processor/PostProcessor.java

```java
@FunctionalInterface
public interface PostProcessor extends QueryProcessor {
    QueryData process(QueryData result, QueryContext context);
    @Override
    default Object process(Object input, QueryContext context) {
        if (!(input instanceof QueryData)) {
            throw new IllegalArgumentException("Post processor requires QueryData input");
        }
        return process((QueryData) input, context);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/processor/PreProcessor.java

```java
@FunctionalInterface
public interface PreProcessor extends QueryProcessor {
    void process(QueryContext context);
    @Override
    default Object process(Object input, QueryContext context) {
        process(context);
        return null; 
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/processor/QueryProcessor.java

```java
@FunctionalInterface
public interface QueryProcessor {
    Object process(Object input, QueryContext context);
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
            return null; 
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
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/processor/RowProcessor.java

```java
@FunctionalInterface
public interface RowProcessor extends QueryProcessor {
    QueryRow process(QueryRow row, QueryContext context);
    @Override
    default Object process(Object input, QueryContext context) {
        if (!(input instanceof QueryRow)) {
            throw new IllegalArgumentException("Row processor requires QueryRow input");
        }
        return process((QueryRow) input, context);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/domain/select/SelectItem.java

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelectItem {
    private String value;
    private String label;
    public static SelectItem of(String value, String label) {
        return SelectItem.builder()
                .value(value)
                .label(label)
                .build();
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/engine/query/QueryExecutorImpl.java

```java
public class QueryExecutorImpl {
    private static final Logger log = LoggerFactory.getLogger(QueryExecutorImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final QueryRegistryImpl queryRegistry;
    private final QuerySqlBuilder sqlBuilder;
    private final QueryRowMapperImpl rowMapper;
    public QueryExecutorImpl(JdbcTemplate jdbcTemplate, QuerySqlBuilder sqlBuilder, QueryRegistryImpl queryRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.queryRegistry = queryRegistry;
        this.sqlBuilder = sqlBuilder;
        this.rowMapper = new QueryRowMapperImpl();
    }
    public QueryExecution execute(String queryName) {
        QueryDefinitionBuilder definition = queryRegistry.get(queryName);
        if (definition == null) {
            throw new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND, "Query not found: " + queryName);
        }
        return new QueryExecution(definition, this);
    }
    public QueryExecution execute(QueryDefinitionBuilder definition) {
        return new QueryExecution(definition, this);
    }
    public QueryExecution prepare(QueryDefinitionBuilder definition) {
        return new QueryExecution(definition, this);
    }
    @Transactional(readOnly = true)
    public QueryData doExecute(QueryContext context) {
        context.startExecution();
        try {
            runPreProcessors(context);
            SqlResult sqlResult = sqlBuilder.build(context);
            String finalSql = sqlResult.getSql();
            Map<String, Object> params = sqlResult.getParams();
            log.debug("Executing query '{}': {}", context.getDefinition().getName(), finalSql);
            log.debug("Parameters: {}", params);
            if (context.hasPagination() && context.getDefinition().isPaginationEnabled()) {
                int totalCount = executeTotalCountQuery(context, sqlResult);
                context.setTotalCount(totalCount);
            }
            List<QueryRow> rows = executeQuery(context, finalSql, params);
            rows = runRowProcessors(context, rows);
            QueryData result = QueryData.builder()
                    .rows(ImmutableList.copyOf(rows))
                    .context(context)
                    .build();
            runResultAwareParamProcessors(context);
            result = runPostProcessors(context, result);
            if (context.isIncludeMetadata()) {
                result = addMetadata(context, result);
            }
            context.endExecution();
            return result.toBuilder()
                    .executionTimeMs(context.getExecutionTime())
                    .build();
        } catch (Exception e) {
            context.endExecution();
            log.error("Query execution failed for '{}': {}",
                    context.getDefinition().getName(), e.getMessage(), e);
            if (e instanceof QueryException queryException) {
                throw queryException;
            } else {
                throw new QueryException(
                        context.getDefinition().getName(),
                        QueryException.ErrorCode.EXECUTION_ERROR,
                        "Query execution failed: " + e.getMessage(), e);
            }
        }
    }
    private void runPreProcessors(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (definition.hasPreProcessors()) {
            definition.getPreProcessors().forEach(processor -> processor.process(context));
        }
    }
    private void runResultAwareParamProcessors(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (!definition.hasParams()) {
            return;
        }
        definition.getParameters().forEach((paramName, paramDef) -> {
            if (paramDef.hasProcessor()) {
                try {
                    Object currentValue = context.getParam(paramName);
                    @SuppressWarnings("unchecked")
                    var processor = (com.balsam.oasis.common.registry.domain.processor.ParamProcessor<Object>) paramDef.processor();
                    Object processedValue = processor.process(currentValue, context);
                    context.addParam(paramName, processedValue);
                } catch (Exception e) {
                    log.warn("Failed to process result-aware parameter {}: {}", paramName, e.getMessage());
                }
            }
        });
    }
    private List<QueryRow> executeQuery(QueryContext context, String sql, Map<String, Object> params) {
        try {
            if (context.getDefinition().getQueryTimeout() != null) {
                jdbcTemplate.setQueryTimeout(context.getDefinition().getQueryTimeout());
            }
            final QueryContext finalContext = context;
            final QueryDefinitionBuilder finalDefinition = context.getDefinition();
            return namedJdbcTemplate.execute(sql, params, (ps) -> {
                if (finalDefinition.getFetchSize() != null) {
                    ps.setFetchSize(finalDefinition.getFetchSize());
                } else {
                    ps.setFetchSize(100);
                }
                ps.setFetchDirection(ResultSet.FETCH_FORWARD);
                try (ResultSet rs = ps.executeQuery()) {
                    List<QueryRow> results = new ArrayList<>();
                    int rowNum = 0;
                    while (rs.next()) {
                        results.add(rowMapper.mapRow(rs, rowNum++, finalContext));
                    }
                    return results;
                }
            });
        } catch (Exception e) {
            throw new QueryException(
                    context.getDefinition().getName(),
                    QueryException.ErrorCode.EXECUTION_ERROR,
                    "Failed to execute query: " + e.getMessage(), e);
        }
    }
    private int executeTotalCountQuery(QueryContext context, SqlResult sqlResult) {
        try {
            String countSql = sqlBuilder.buildCountQuery(context);
            Map<String, Object> params = sqlResult.getParams();
            log.debug("Executing count query: {}", countSql);
            Integer count = namedJdbcTemplate.queryForObject(countSql, params, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Failed to execute count query, using fallback: {}", e.getMessage());
            return 0;
        }
    }
    private List<QueryRow> runRowProcessors(QueryContext context, List<QueryRow> rows) {
        QueryDefinitionBuilder definition = context.getDefinition();
        boolean hasAggregateCalculators = definition.hasAttributes() &&
                definition.getAttributes().values().stream()
                    .anyMatch(attr -> attr.virtual() && attr.hasCalculator());
        boolean hasCustomRowProcessors = definition.hasRowProcessors();
        boolean hasFormatters = definition.hasAttributes() &&
                definition.getAttributes().values().stream()
                    .anyMatch(com.balsam.oasis.common.registry.domain.definition.AttributeDef::hasFormatter);
        if (!hasAggregateCalculators && !hasCustomRowProcessors && !hasFormatters) {
            return rows;
        }
        List<QueryRow> processedRows = new ArrayList<>(rows);
        if (processedRows.size() > 1000) {
            processBatchedRows(processedRows, hasAggregateCalculators, hasCustomRowProcessors, hasFormatters, context, definition);
        } else {
            for (int i = 0; i < processedRows.size(); i++) {
                QueryRow row = processedRows.get(i);
                row = processRowWithAllSteps(row, processedRows, hasAggregateCalculators, hasCustomRowProcessors, hasFormatters, context, definition);
                processedRows.set(i, row);
            }
        }
        return processedRows;
    }
    private QueryRow processRowWithAllSteps(QueryRow row, List<QueryRow> allRows,
                                          boolean hasAggregateCalculators, boolean hasCustomRowProcessors,
                                          boolean hasFormatters, QueryContext context, QueryDefinitionBuilder definition) {
        if (hasAggregateCalculators) {
            row = applyAggregateCalculations(row, allRows, context, definition);
        }
        if (hasCustomRowProcessors) {
            for (var processor : definition.getRowProcessors()) {
                row = processor.process(row, context);
            }
        }
        if (hasFormatters) {
            row = applyRowAttributeFormatters(row, definition);
        }
        return row;
    }
    private void processBatchedRows(List<QueryRow> processedRows,
                                   boolean hasAggregateCalculators, boolean hasCustomRowProcessors,
                                   boolean hasFormatters, QueryContext context, QueryDefinitionBuilder definition) {
        final int BATCH_SIZE = 100;
        for (int startIndex = 0; startIndex < processedRows.size(); startIndex += BATCH_SIZE) {
            int endIndex = Math.min(startIndex + BATCH_SIZE, processedRows.size());
            for (int i = startIndex; i < endIndex; i++) {
                QueryRow row = processedRows.get(i);
                row = processRowWithAllSteps(row, processedRows, hasAggregateCalculators, hasCustomRowProcessors, hasFormatters, context, definition);
                processedRows.set(i, row);
            }
            if (processedRows.size() > 5000) {
                System.gc(); 
            }
        }
    }
    private QueryRow applyAggregateCalculations(QueryRow row, List<QueryRow> allRows, QueryContext context, QueryDefinitionBuilder definition) {
        for (Map.Entry<String, com.balsam.oasis.common.registry.domain.definition.AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            var attr = entry.getValue();
            if (attr.virtual() && attr.hasCalculator()) {
                try {
                    @SuppressWarnings("unchecked")
                    var calculator = (com.balsam.oasis.common.registry.domain.processor.Calculator<Object>) attr.calculator();
                    Object enhancedValue = calculator.calculateWithAllRows(row, allRows, context);
                    row.set(attrName, enhancedValue);
                } catch (Exception e) {
                    log.warn("Failed to recalculate virtual attribute {}: {}", attrName, e.getMessage());
                }
            }
        }
        return row;
    }
    private QueryRow applyRowAttributeFormatters(QueryRow row, QueryDefinitionBuilder definition) {
        for (Map.Entry<String, com.balsam.oasis.common.registry.domain.definition.AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            var attr = entry.getValue();
            if (attr.hasFormatter()) {
                Object value = row.get(attrName);
                if (value != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        var formatter = (com.balsam.oasis.common.registry.domain.processor.AttributeFormatter<Object>) attr.formatter();
                        String formattedValue = formatter.format(value);
                        row.set(attrName, formattedValue);
                    } catch (Exception e) {
                        log.warn("Failed to format attribute {}: {}", attrName, e.getMessage());
                    }
                }
            }
        }
        return row;
    }
    private QueryData runPostProcessors(QueryContext context, QueryData result) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (!definition.hasPostProcessors()) {
            return result;
        }
        QueryData processedResult = result;
        for (var processor : definition.getPostProcessors()) {
            processedResult = processor.process(processedResult, context);
        }
        return processedResult;
    }
    private QueryData addMetadata(QueryContext context, QueryData result) {
        var metadataBuilder = new QueryMetadata.MetadataBuilder(context, result);
        var metadata = metadataBuilder.build();
        return result.toBuilder()
                .metadata(metadata)
                .build();
    }
}
```

---

## src/main/java/com/balsam/oasis/common/registry/engine/query/QueryRegistryImpl.java

```java
public class QueryRegistryImpl {
    private static final Logger log = LoggerFactory.getLogger(QueryRegistryImpl.class);
    private final ConcurrentMap<String, QueryDefinitionBuilder> registry = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    public void register(QueryDefinitionBuilder definition) {
        validateDefinition(definition);
        String name = definition.getName();
        lock.writeLock().lock();
        try {
            if (registry.putIfAbsent(name, definition) != null) {
                throw new IllegalStateException("Query already registered: " + name);
            }
            StringBuilder registrationLog = new StringBuilder();
            registrationLog.append("Registered query '" + name + "': ");
            registrationLog.append("attributes=").append(definition.getAttributes().size());
            if (definition.hasParams()) {
                registrationLog.append(", has_params=true");
                Set<String> unusedParams = QueryUtils.findUnusedParameters(definition);
                if (!unusedParams.isEmpty()) {
                    registrationLog.append(", unused_params=").append(unusedParams);
                }
            }
            log.info(registrationLog.toString());
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void clear() {
        lock.writeLock().lock();
        try {
            int count = registry.size();
            registry.clear();
            log.info("Cleared {} queries from registry", count);
        } finally {
            lock.writeLock().unlock();
        }
    }
    public QueryDefinitionBuilder get(String name) {
        if (name == null) {
            return null;
        }
        lock.readLock().lock();
        try {
            return registry.get(name);
        } finally {
            lock.readLock().unlock();
        }
    }
    public Set<String> getQueryNames() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(registry.keySet()));
        } finally {
            lock.readLock().unlock();
        }
    }
    public Collection<QueryDefinitionBuilder> getAllQueries() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(registry.values()));
        } finally {
            lock.readLock().unlock();
        }
    }
    public int size() {
        lock.readLock().lock();
        try {
            return registry.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return registry.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
    private void validateDefinition(QueryDefinitionBuilder definition) {
        if (definition == null) {
            throw new IllegalArgumentException("QueryDefinition cannot be null");
        }
        String name = definition.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("QueryDefinition name is required");
        }
        String sql = definition.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("QueryDefinition SQL is required for query: " + name);
        }
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/engine/query/QueryRow.java

```java
public class QueryRow {
    private final Map<String, Object> data;
    private final QueryContext context;
    private QueryRow(Map<String, Object> data, QueryContext context) {
        this.data = data; 
        this.context = context;
    }
    public static QueryRow create(Map<String, Object> data, Map<String, Object> rawData, QueryContext context) {
        return new QueryRow(rawData, context);
    }
    public Object get(String key) {
        Object value = data.get(key);
        if (value != null) {
            return value;
        }
        return data.get(key.toUpperCase());
    }
    public Object getRaw(String columnName) {
        return data.get(columnName.toUpperCase());
    }
    public void set(String key, Object value) {
        data.put(key, value);
    }
    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }
    public QueryContext getContext() {
        return context;
    }
    public boolean has(String key) {
        return data.containsKey(key) || data.containsKey(key.toUpperCase());
    }
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }
    public Integer getInteger(String key) {
        return get(key, Integer.class);
    }
    public Long getLong(String key) {
        return get(key, Long.class);
    }
    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/engine/query/QueryRowMapperImpl.java

```java
@Slf4j
public class QueryRowMapperImpl {
    public QueryRow mapRow(ResultSet rs, int rowNum, QueryContext context) throws SQLException {
        QueryDefinitionBuilder definition = context.getDefinition();
        Map<String, Object> rawData = extractRawData(rs);
        QueryRow row = QueryRow.create(rawData, rawData, context);
        if (definition.hasAttributes()) {
            for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
                AttributeDef<?> attr = entry.getValue();
                if (attr.virtual() && attr.hasCalculator()) {
                    try {
                        Object calculatedValue = attr.calculator().calculate(row, context);
                        row.set(entry.getKey(), calculatedValue);
                    } catch (Exception e) {
                        log.warn("Failed to calculate virtual attribute {}: {}", entry.getKey(), e.getMessage());
                        row.set(entry.getKey(), null);
                    }
                }
            }
        }
        return row;
    }
    private Map<String, Object> extractRawData(ResultSet rs) throws SQLException {
        Map<String, Object> rawData = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i).toUpperCase();
            String columnLabel = metaData.getColumnLabel(i).toUpperCase();
            Object value = rs.getObject(i);
            rawData.put(columnName, value);
            if (!columnName.equals(columnLabel)) {
                rawData.put(columnLabel, value);
            }
        }
        return rawData;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/engine/query/QuerySqlBuilder.java

```java
public class QuerySqlBuilder {
    private static final Logger log = LoggerFactory.getLogger(QuerySqlBuilder.class);
    public QuerySqlBuilder() {
        log.info("SqlBuilder initialized for Oracle 11g");
    }
    public SqlResult build(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());
        sql = QueryUtils.applyCriteria(sql, context, bindParams);
        sql = QueryUtils.applyFilters(sql, context, bindParams);
        sql = QueryUtils.applySorting(sql, context);
        if (context.hasPagination() && context.getDefinition().isPaginationEnabled()) {
            sql = QueryUtils.applyPagination(sql, context);
        }
        sql = QueryUtils.cleanPlaceholders(sql);
        return new SqlResult(sql, bindParams);
    }
    public String buildCountQuery(QueryContext context) {
        QueryDefinitionBuilder definition = context.getDefinition();
        String sql = definition.getSql();
        Map<String, Object> bindParams = new HashMap<>(context.getParams());
        sql = QueryUtils.applyCriteria(sql, context, bindParams);
        sql = QueryUtils.applyFilters(sql, context, bindParams);
        sql = QueryUtils.cleanPlaceholders(sql);
        return QueryUtils.wrapForCount(sql);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/example/OracleHRQueryConfig.java

```java
@Configuration
@RequiredArgsConstructor
public class OracleHRQueryConfig {
        private final QueryRegistryImpl queryRegistry;
        private final QueryExecutorImpl queryExecutor;
        @PostConstruct
        public void registerQueries() {
                queryRegistry.register(QueryDefinitionBuilder.builder("dynamic").sql("""
                                SELECT * from employees
                                                    """)
                                .parameter(ParamDef.name("deptId", Integer.class)
                                                .build())
                                .attribute(AttributeDef.name("asdasd").aliasName("XX").build())
                                .attribute(AttributeDef.name("fullName", String.class)
                                                .calculated((row, context) -> String.format("%s %s",
                                                                row.getString("FIRST_NAME"),
                                                                row.getString("LAST_NAME")))
                                                .build())
                                .rowProcessor((row, context) -> {
                                        row.set("DEPT_NAME", "zzz");
                                        return row;
                                })
                                .build());
                queryRegistry.register(employeesQuery());
                queryRegistry.register(departmentStatsQuery());
                queryRegistry.register(QueryDefinitionBuilder.builder("testUnion").sql(
                                """
                                                select employee_id,email
                                                from employees
                                                where department_id <> 100
                                                union
                                                select employee_id,email
                                                from employees
                                                where department_id <> 110
                                                                                """)
                                .attribute(AttributeDef.name("EMPLOYEE_ID", Integer.class)
                                                .build())
                                .parameter(ParamDef.name("jobId", String.class).required(false).build())
                                .build());
                queryRegistry.register(employeesSelectQuery());
                queryRegistry.register(departmentsSelectQuery());
                queryRegistry.register(jobsSelectQuery());
                queryRegistry.register(managersSelectQuery());
        }
        private QueryDefinitionBuilder employeesQuery() {
                return QueryDefinitionBuilder.builder("employees")
                                .sql("""
                                                SELECT
                                                    e.employee_id,
                                                    e.first_name,
                                                    e.last_name,
                                                    e.email,
                                                    e.phone_number,
                                                    e.hire_date,
                                                    e.job_id,
                                                    j.job_title,
                                                    e.salary,
                                                    e.commission_pct,
                                                    e.manager_id,
                                                    m.first_name || ' ' || m.last_name as manager_name,
                                                    e.department_id,
                                                    d.department_name,
                                                    l.city,
                                                    l.country_id
                                                FROM employees e
                                                LEFT JOIN jobs j ON e.job_id = j.job_id
                                                LEFT JOIN employees m ON e.manager_id = m.employee_id
                                                LEFT JOIN departments d ON e.department_id = d.department_id
                                                LEFT JOIN locations l ON d.location_id = l.location_id
                                                WHERE 1=1
                                                --departmentFilter
                                                --salaryFilter
                                                --hiredAfterFilter
                                                --departmentIdsFilter
                                                --employeeIdsFilter
                                                --jobIdsFilter
                                                --statusFilter
                                                """)
                                .description("Oracle HR Schema - Employee information with department and manager details")
                                .attribute(AttributeDef.name("employeeId", Integer.class)
                                                .aliasName("employee_id")
                                                .primaryKey(true)
                                                .label("Employee ID")
                                                .labelKey("employee.id.label")
                                                .width("100px")
                                                .build())
                                .attribute(AttributeDef.name("firstName", String.class)
                                                .aliasName("first_name")
                                                .label("First Name")
                                                .labelKey("employee.firstName.label")
                                                .width("150px")
                                                .flex("1")
                                                .build())
                                .attribute(AttributeDef.name("lastName", String.class)
                                                .aliasName("last_name")
                                                .label("Last Name")
                                                .labelKey("employee.lastName.label")
                                                .width("150px")
                                                .flex("1")
                                                .build())
                                .attribute(AttributeDef.name("email", String.class)
                                                .aliasName("email")
                                                .build())
                                .attribute(AttributeDef.name("phoneNumber", String.class)
                                                .aliasName("phone_number")
                                                .build())
                                .attribute(AttributeDef.name("hireDate", LocalDate.class)
                                                .aliasName("hire_date")
                                                .build())
                                .attribute(AttributeDef.name("jobId", String.class)
                                                .aliasName("job_id")
                                                .build())
                                .attribute(AttributeDef.name("jobTitle", String.class)
                                                .aliasName("job_title")
                                                .build())
                                .attribute(AttributeDef.name("city", String.class)
                                                .aliasName("city")
                                                .formatter(value -> value.toLowerCase())
                                                .build())
                                .attribute(AttributeDef.name("totalCompensation", BigDecimal.class)
                                                .calculated((row, context) -> {
                                                        System.out.println("totalCompensation");
                                                        BigDecimal salary = (BigDecimal) row.get("SALARY");
                                                        BigDecimal commission = (BigDecimal) row.get("COMMISSION_PCT");
                                                        if (salary == null)
                                                                return BigDecimal.ZERO;
                                                        if (commission == null)
                                                                return salary;
                                                        return salary.add(salary.multiply(commission));
                                                })
                                                .build())
                                .attribute(AttributeDef.name("testVirtual", String.class)
                                                .calculated((row, context) -> {
                                                        return "Hello World";
                                                }).build())
                                .attribute(AttributeDef.name("internalDebugInfo", String.class)
                                                .selected(false) 
                                                .calculated((row, context) -> "Debug: ID=" + row.get("EMPLOYEE_ID"))
                                                .build())
                                .parameter(ParamDef.name("deptId")
                                                .build())
                                .parameter(ParamDef.name("departmentIds", String.class)
                                                .build())
                                .parameter(ParamDef.name("employeeIds", List.class).build())
                                .parameter(ParamDef.name("jobIds", List.class).build())
                                .parameter(ParamDef.name("minSalary", BigDecimal.class).build())
                                .parameter(ParamDef.name("hiredAfter", LocalDate.class)
                                                .processor((value, ctx) -> {
                                                        System.out.println("processing hiredAfter: " + value);
                                                        if (value instanceof Integer days) {
                                                                return LocalDate.now().minusDays(days);
                                                        }
                                                        if (value == null) {
                                                                return null;
                                                        }
                                                        return QueryUtils.convertValue(value, LocalDate.class);
                                                })
                                                .build())
                                .criteria(CriteriaDef.name("departmentFilter")
                                                .sql("AND e.department_id = :deptId")
                                                .condition(ctx -> ctx.hasParam("deptId"))
                                                .build())
                                .criteria(CriteriaDef.name("salaryFilter")
                                                .sql("AND e.salary >= :minSalary")
                                                .condition(ctx -> ctx.hasParam("minSalary"))
                                                .build())
                                .criteria(CriteriaDef.name("hiredAfterFilter")
                                                .sql("AND e.hire_date > :hiredAfter")
                                                .condition(ctx -> ctx.hasParam("hiredAfter"))
                                                .build())
                                .criteria(CriteriaDef.name("departmentIdsFilter")
                                                .sql("AND e.department_id IN (:departmentIds)")
                                                .condition(ctx -> ctx.hasParam("departmentIds"))
                                                .build())
                                .criteria(CriteriaDef.name("employeeIdsFilter")
                                                .sql("AND e.employee_id IN (:employeeIds)")
                                                .condition(ctx -> ctx.hasParam("employeeIds"))
                                                .build())
                                .criteria(CriteriaDef.name("jobIdsFilter")
                                                .sql("AND e.job_id IN (:jobIds)")
                                                .condition(ctx -> ctx.hasParam("jobIds"))
                                                .build())
                                .preProcessor((context) -> {
                                        System.out.println("@@@@@@@@@preProcessor@@@@@@@@");
                                })
                                .rowProcessor((row, context) -> {
                                        System.out.println("@@@@@@@@@rowProcessor@@@@@@@@");
                                        return row;
                                })
                                .postProcessor((queryResult, context) -> {
                                        System.out.println("@@@@@@@@@postProcessor@@@@@@@@");
                                        return queryResult;
                                })
                                .defaultPageSize(20)
                                .maxPageSize(100)
                                .cache(true)
                                .build();
        }
        private QueryDefinitionBuilder departmentStatsQuery() {
                return QueryDefinitionBuilder.builder("departmentStats")
                                .sql("""
                                                SELECT
                                                    d.department_id,
                                                    d.department_name,
                                                    COUNT(e.employee_id) as employee_count,
                                                    AVG(e.salary) as avg_salary,
                                                    MIN(e.salary) as min_salary,
                                                    MAX(e.salary) as max_salary,
                                                    SUM(e.salary) as total_salary,
                                                    l.city,
                                                    l.state_province,
                                                    c.country_name
                                                FROM departments d
                                                LEFT JOIN employees e ON d.department_id = e.department_id
                                                LEFT JOIN locations l ON d.location_id = l.location_id
                                                LEFT JOIN countries c ON l.country_id = c.country_id
                                                WHERE 1=1
                                                --countryFilter
                                                GROUP BY
                                                    d.department_id,
                                                    d.department_name,
                                                    l.city,
                                                    l.state_province,
                                                    c.country_name
                                                HAVING COUNT(e.employee_id) > 0
                                                """)
                                .description("Department statistics with employee counts and salary information")
                                .attribute(AttributeDef.name("departmentId", Integer.class)
                                                .aliasName("department_id")
                                                .primaryKey(true)
                                                .build())
                                .attribute(AttributeDef.name("departmentName", String.class)
                                                .aliasName("department_name")
                                                .build())
                                .attribute(AttributeDef.name("employeeCount", Integer.class)
                                                .aliasName("employee_count")
                                                .build())
                                .attribute(AttributeDef.name("avgSalary", BigDecimal.class)
                                                .aliasName("avg_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("minSalary", BigDecimal.class)
                                                .aliasName("min_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("maxSalary", BigDecimal.class)
                                                .aliasName("max_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("totalSalary", BigDecimal.class)
                                                .aliasName("total_salary")
                                                .formatter(value -> value != null ? String.format("$%.2f", value)
                                                                : "N/A")
                                                .build())
                                .attribute(AttributeDef.name("city", String.class)
                                                .aliasName("city")
                                                .build())
                                .attribute(AttributeDef.name("stateProvince", String.class)
                                                .aliasName("state_province")
                                                .build())
                                .attribute(AttributeDef.name("countryName", String.class)
                                                .aliasName("country_name")
                                                .build())
                                .attribute(AttributeDef.name("departmentEmployees", List.class)
                                                .calculated((row, context) -> {
                                                        System.out.println("Fetching employees for department");
                                                        Integer deptId = (Integer) row.getRaw("DEPARTMENT_ID");
                                                        if (deptId == null) {
                                                                return List.of();
                                                        }
                                                        try {
                                                                System.out.println(
                                                                                "Fetching employees for department ID: "
                                                                                                + deptId);
                                                                QueryData result = queryExecutor.execute("employees")
                                                                                .withParam("departmentIds",
                                                                                                List.of(deptId))
                                                                                .withPagination(0, 100)
                                                                                .execute();
                                                                System.out.println("Found " + result.getRows().size()
                                                                                + " employees for dept " + deptId);
                                                                return result.getData();
                                                        } catch (Exception e) {
                                                                System.err.println(
                                                                                "Error fetching department employees: "
                                                                                                + e.getMessage());
                                                                return List.of();
                                                        }
                                                })
                                                .build())
                                .parameter(ParamDef.name("country")
                                                .build())
                                .criteria(CriteriaDef.name("countryFilter")
                                                .sql("AND c.country_name = :country")
                                                .condition(ctx -> ctx.hasParam("country"))
                                                .build())
                                .defaultPageSize(25)
                                .maxPageSize(100)
                                .build();
        }
        private QueryDefinitionBuilder employeesSelectQuery() {
                return QueryDefinitionBuilder.builder("employeesLov")
                                .sql("""
                                                SELECT
                                                    e.employee_id,
                                                    e.first_name || ' ' || e.last_name as full_name,
                                                    e.email,
                                                    d.department_name
                                                FROM employees e
                                                LEFT JOIN departments d ON e.department_id = d.department_id
                                                WHERE 1=1
                                                --departmentFilter
                                                --searchFilter
                                                """)
                                .description("Employee select for dropdowns with search and department filtering")
                                .asSelect("employee_id", "full_name")
                                .attribute(AttributeDef.name("employee_id", Integer.class)
                                                .aliasName("employee_id")
                                                .build())
                                .attribute(AttributeDef.name("full_name", String.class)
                                                .aliasName("full_name")
                                                .build())
                                .attribute(AttributeDef.name("email", String.class)
                                                .aliasName("email")
                                                .build())
                                .attribute(AttributeDef.name("department_name", String.class)
                                                .aliasName("department_name")
                                                .build())
                                .criteria(CriteriaDef.name("departmentFilter")
                                                .sql("AND d.department_id = :departmentId")
                                                .condition(ctx -> ctx.hasParam("departmentId"))
                                                .build())
                                .criteria(CriteriaDef.name("searchFilter")
                                                .sql("AND LOWER(e.first_name || ' ' || e.last_name) LIKE LOWER(:search)")
                                                .condition(ctx -> ctx.hasParam("search"))
                                                .build())
                                .parameter(ParamDef.name("departmentId")
                                                .build())
                                .parameter(ParamDef.name("search")
                                                .build())
                                .paginationEnabled(true)
                                .defaultPageSize(100)
                                .build();
        }
        private QueryDefinitionBuilder departmentsSelectQuery() {
                return QueryDefinitionBuilder.builder("departments")
                                .sql("""
                                                SELECT
                                                    d.department_id,
                                                    d.department_name,
                                                    l.city,
                                                    l.state_province,
                                                    c.country_name
                                                FROM departments d
                                                LEFT JOIN locations l ON d.location_id = l.location_id
                                                LEFT JOIN countries c ON l.country_id = c.country_id
                                                WHERE 1=1
                                                --locationFilter
                                                """)
                                .description("Department select with location information")
                                .asSelect("department_id", "department_name")
                                .attribute(AttributeDef.name("department_id", Integer.class)
                                                .aliasName("department_id")
                                                .build())
                                .attribute(AttributeDef.name("department_name", String.class)
                                                .aliasName("department_name")
                                                .build())
                                .attribute(AttributeDef.name("city", String.class)
                                                .aliasName("city")
                                                .build())
                                .attribute(AttributeDef.name("state_province", String.class)
                                                .aliasName("state_province")
                                                .build())
                                .attribute(AttributeDef.name("country_name", String.class)
                                                .aliasName("country_name")
                                                .build())
                                .criteria(CriteriaDef.name("locationFilter")
                                                .sql("AND l.location_id = :locationId")
                                                .condition(ctx -> ctx.hasParam("locationId"))
                                                .build())
                                .parameter(ParamDef.name("locationId")
                                                .build())
                                .build();
        }
        private QueryDefinitionBuilder jobsSelectQuery() {
                return QueryDefinitionBuilder.builder("jobs")
                                .sql("""
                                                SELECT
                                                    job_id,
                                                    job_title,
                                                    min_salary,
                                                    max_salary
                                                FROM jobs
                                                ORDER BY job_title
                                                """)
                                .description("Job titles for dropdowns")
                                .asSelect("job_id", "job_title")
                                .attribute(AttributeDef.name("job_id", String.class)
                                                .aliasName("job_id")
                                                .build())
                                .attribute(AttributeDef.name("job_title", String.class)
                                                .aliasName("job_title")
                                                .build())
                                .attribute(AttributeDef.name("min_salary", BigDecimal.class)
                                                .aliasName("min_salary")
                                                .build())
                                .attribute(AttributeDef.name("max_salary", BigDecimal.class)
                                                .aliasName("max_salary")
                                                .build())
                                .build();
        }
        private QueryDefinitionBuilder managersSelectQuery() {
                return QueryDefinitionBuilder.builder("managers")
                                .sql("""
                                                SELECT DISTINCT
                                                    m.employee_id,
                                                    m.first_name || ' ' || m.last_name as full_name,
                                                    m.email,
                                                    d.department_name
                                                FROM employees e
                                                INNER JOIN employees m ON e.manager_id = m.employee_id
                                                LEFT JOIN departments d ON m.department_id = d.department_id
                                                WHERE 1=1
                                                --searchFilter
                                                """)
                                .description("Managers only for selection")
                                .asSelect("employee_id", "full_name")
                                .attribute(AttributeDef.name("employee_id", Integer.class)
                                                .aliasName("employee_id")
                                                .build())
                                .attribute(AttributeDef.name("full_name", String.class)
                                                .aliasName("full_name")
                                                .build())
                                .attribute(AttributeDef.name("email", String.class)
                                                .aliasName("email")
                                                .build())
                                .attribute(AttributeDef.name("department_name", String.class)
                                                .aliasName("department_name")
                                                .build())
                                .criteria(CriteriaDef.name("searchFilter")
                                                .sql("AND LOWER(m.first_name || ' ' || m.last_name) LIKE LOWER(:search)")
                                                .condition(ctx -> ctx.hasParam("search"))
                                                .build())
                                .parameter(ParamDef.name("search")
                                                .build())
                                .defaultPageSize(50)
                                .build();
        }
}```

---

## src/main/java/com/balsam/oasis/common/registry/service/QueryService.java

```java
@Service
public class QueryService {
    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    private final QueryExecutorImpl queryExecutor;
    private final QueryRegistryImpl queryRegistry;
    public QueryService(QueryExecutorImpl queryExecutor, QueryRegistryImpl queryRegistry) {
        this.queryExecutor = queryExecutor;
        this.queryRegistry = queryRegistry;
    }
    public QueryData executeQuery(String queryName, QueryRequest request) {
        log.info("Executing query: {} with params: {}", queryName, request.getParams());
        QueryDefinitionBuilder queryDefinition = queryRegistry.get(queryName);
        if (queryDefinition == null) {
            throw new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "Query not found: " + queryName);
        }
        validateRequest(queryDefinition, request);
        QueryExecution execution = queryExecutor.execute(queryDefinition);
        if (request.getParams() != null) {
            request.getParams().forEach(execution::withParam);
        }
        if (request.getFilters() != null) {
            request.getFilters().forEach((key, filter) -> {
                if (filter.getValues() != null && !filter.getValues().isEmpty()) {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), filter.getValues());
                } else if (filter.getValue2() != null) {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), 
                            filter.getValue(), filter.getValue2());
                } else if (filter.getValue() != null) {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), filter.getValue());
                } else {
                    execution.withFilter(filter.getAttribute(), filter.getOperator(), null);
                }
            });
        }
        if (request.getSorts() != null) {
            request.getSorts().forEach(sort -> 
                execution.withSort(sort.getAttribute(), sort.getDirection())
            );
        }
        if (request.getPagination() != null) {
            execution.withPagination(request.getPagination().getOffset(), 
                    request.getPagination().getLimit());
        }
        return execution.execute();
    }
    public QueryData executeQuery(String queryName) {
        return executeQuery(queryName, QueryRequest.builder().build());
    }
    public QueryExecution createExecution(QueryDefinitionBuilder queryDefinition) {
        if (queryDefinition == null) {
            throw new QueryException(QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "Query definition cannot be null");
        }
        return queryExecutor.execute(queryDefinition);
    }
    public QueryDefinitionBuilder getQueryDefinition(String queryName) {
        return queryRegistry.get(queryName);
    }
    public boolean isQueryRegistered(String queryName) {
        return queryRegistry.get(queryName) != null;
    }
    public List<String> getRegisteredQueryNames() {
        return List.of();
    }
    public QueryData executeAsSelect(String queryName, QueryRequest request) {
        log.info("Executing query as select: {} with params: {}", queryName, request.getParams());
        QueryDefinitionBuilder queryDefinition = queryRegistry.get(queryName);
        if (queryDefinition == null) {
            throw new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "Query not found: " + queryName);
        }
        if (!queryDefinition.hasValueAttribute() || !queryDefinition.hasLabelAttribute()) {
            throw new QueryException(queryName, QueryException.ErrorCode.DEFINITION_ERROR,
                    "Query must define value and label attributes for select mode. Use asSelect() or valueAttribute()/labelAttribute() when building the query.");
        }
        return executeQuery(queryName, request);
    }
    private void validateRequest(QueryDefinitionBuilder queryDefinition, QueryRequest request) {
        if (request.getFilters() != null) {
            request.getFilters().forEach((key, filter) -> {
                var attr = queryDefinition.getAttribute(filter.getAttribute());
                if (attr == null) {
                    throw new QueryException(queryDefinition.getName(),
                            QueryException.ErrorCode.VALIDATION_ERROR,
                            "Unknown attribute for filter: " + filter.getAttribute());
                }
                if (!attr.filterable()) {
                    throw new QueryException(queryDefinition.getName(),
                            QueryException.ErrorCode.VALIDATION_ERROR,
                            "Attribute '" + filter.getAttribute() + "' is not filterable");
                }
            });
        }
        if (request.getSorts() != null) {
            request.getSorts().forEach(sort -> {
                var attr = queryDefinition.getAttribute(sort.getAttribute());
                if (attr == null) {
                    throw new QueryException(queryDefinition.getName(),
                            QueryException.ErrorCode.VALIDATION_ERROR,
                            "Unknown attribute for sort: " + sort.getAttribute());
                }
                if (!attr.sortable()) {
                    throw new QueryException(queryDefinition.getName(),
                            QueryException.ErrorCode.VALIDATION_ERROR,
                            "Attribute '" + sort.getAttribute() + "' is not sortable");
                }
            });
        }
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/util/QueryUtils.java

```java
public class QueryUtils {
    private static final Logger log = LoggerFactory.getLogger(QueryUtils.class);
    private static final Pattern BIND_PARAM_PATTERN = Pattern.compile(":(\\w+)");
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    private static final Map<Integer, Class<?>> SQL_TYPE_MAP = new HashMap<>();
    private static final Map<String, Class<?>> TYPE_NAME_MAP = new HashMap<>();
    static {
        initializeTypeMappings();
    }
    public static String replacePlaceholder(String sql, String placeholder, String replacement) {
        return sql.replace("--" + placeholder, replacement != null ? replacement : "");
    }
    public static String cleanPlaceholders(String sql) {
        return sql.replaceAll("--\\w+", "");
    }
    public static String wrapForCount(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") count_query";
    }
    public static Map<String, Object> extractBindParams(String sql, Map<String, Object> allParams) {
        Map<String, Object> bindParams = new HashMap<>();
        Matcher matcher = BIND_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (allParams.containsKey(paramName)) {
                bindParams.put(paramName, allParams.get(paramName));
            }
        }
        return bindParams;
    }
    public static boolean hasBindParameter(String sql, String paramName) {
        Pattern pattern = Pattern.compile(":" + Pattern.quote(paramName) + "\\b");
        return pattern.matcher(sql).find();
    }
    public static String applyFilters(String sql, QueryContext context, Map<String, Object> params) {
        if (context.getFilters() == null || context.getFilters().isEmpty()) {
            return sql;
        }
        StringBuilder filterClause = new StringBuilder();
        int paramIndex = 0;
        for (Filter filter : context.getFilters().values()) {
            AttributeDef<?> attr = context.getDefinition().getAttribute(filter.getAttribute());
            if (attr == null || !attr.filterable()) {
                log.warn("Attribute {} is not filterable or does not exist", filter.getAttribute());
                continue;
            }
            if (filterClause.length() > 0) {
                filterClause.append(" AND ");
            }
            String condition = buildFilterCondition(filter, attr, params, paramIndex++);
            filterClause.append(condition);
        }
        if (filterClause.length() > 0) {
            sql = "SELECT * FROM (" + sql + ") WHERE " + filterClause.toString();
        }
        return sql;
    }
    private static String buildFilterCondition(Filter filter, AttributeDef<?> attr,
            Map<String, Object> params, int index) {
        String column = attr.aliasName() != null ? attr.aliasName() : attr.name();
        String paramName = "filter_" + filter.getAttribute() + "_" + index;
        switch (filter.getOperator()) {
            case EQUALS:
                params.put(paramName, filter.getValue());
                return column + " = :" + paramName;
            case NOT_EQUALS:
                params.put(paramName, filter.getValue());
                return column + " != :" + paramName;
            case LIKE:
                params.put(paramName, filter.getValue());
                return "UPPER(" + column + ") LIKE UPPER(:" + paramName + ")";
            case NOT_LIKE:
                params.put(paramName, filter.getValue());
                return "UPPER(" + column + ") NOT LIKE UPPER(:" + paramName + ")";
            case IN:
                params.put(paramName, filter.getValues() != null ? filter.getValues() : filter.getValue());
                return column + " IN (:" + paramName + ")";
            case NOT_IN:
                params.put(paramName, filter.getValues() != null ? filter.getValues() : filter.getValue());
                return column + " NOT IN (:" + paramName + ")";
            case GREATER_THAN:
                params.put(paramName, filter.getValue());
                return column + " > :" + paramName;
            case GREATER_THAN_OR_EQUAL:
                params.put(paramName, filter.getValue());
                return column + " >= :" + paramName;
            case LESS_THAN:
                params.put(paramName, filter.getValue());
                return column + " < :" + paramName;
            case LESS_THAN_OR_EQUAL:
                params.put(paramName, filter.getValue());
                return column + " <= :" + paramName;
            case BETWEEN:
                params.put(paramName + "_1", filter.getValue());
                params.put(paramName + "_2", filter.getValue2());
                return column + " BETWEEN :" + paramName + "_1 AND :" + paramName + "_2";
            case IS_NULL:
                return column + " IS NULL";
            case IS_NOT_NULL:
                return column + " IS NOT NULL";
            default:
                throw new QueryException(QueryException.ErrorCode.PARAMETER_ERROR,
                    "Unsupported filter operator: " + filter.getOperator());
        }
    }
    public static String applySorting(String sql, QueryContext context) {
        if (context.getSorts() == null || context.getSorts().isEmpty()) {
            return sql;
        }
        String orderByClause = context.getSorts().stream()
            .map(sort -> {
                AttributeDef<?> attr = context.getDefinition().getAttribute(sort.getAttribute());
                if (attr == null || !attr.sortable()) {
                    log.warn("Attribute {} is not sortable or does not exist", sort.getAttribute());
                    return null;
                }
                String column = attr.aliasName() != null ? attr.aliasName() : attr.name();
                return column + " " + sort.getDirection().name();
            })
            .filter(s -> s != null)
            .collect(Collectors.joining(", "));
        if (!orderByClause.isEmpty()) {
            sql = replacePlaceholder(sql, "orderBy", "ORDER BY " + orderByClause);
        }
        return sql;
    }
    public static String applyCriteria(String sql, QueryContext context, Map<String, Object> params) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (definition.getCriteria() == null || definition.getCriteria().isEmpty()) {
            return sql;
        }
        for (Map.Entry<String, CriteriaDef> entry : definition.getCriteria().entrySet()) {
            CriteriaDef criteria = entry.getValue();
            if (criteria.condition() == null || criteria.condition().test(context)) {
                String placeholder = entry.getKey();
                String sqlFragment = criteria.sql();
                sql = replacePlaceholder(sql, placeholder, sqlFragment);
            }
        }
        return sql;
    }
    public static String applyPagination(String sql, QueryContext context) {
        if (context.getPagination() == null) {
            return sql;
        }
        int offset = context.getPagination().getOffset();
        Integer limit = context.getPagination().getLimit();
        return applyOracle11gPagination(sql, offset, limit);
    }
    private static String applyOracle11gPagination(String sql, int offset, Integer limit) {
        if (limit == null) {
            return sql;
        }
        StringBuilder paginated = new StringBuilder();
        paginated.append("SELECT * FROM (");
        paginated.append("SELECT query_.*, ROWNUM rnum_ FROM (");
        paginated.append(sql);
        paginated.append(") query_ WHERE ROWNUM <= ").append(offset + limit);
        paginated.append(") WHERE rnum_ > ").append(offset);
        return paginated.toString();
    }
    private static void initializeTypeMappings() {
        SQL_TYPE_MAP.put(Types.TINYINT, Byte.class);
        SQL_TYPE_MAP.put(Types.SMALLINT, Short.class);
        SQL_TYPE_MAP.put(Types.INTEGER, Integer.class);
        SQL_TYPE_MAP.put(Types.BIGINT, Long.class);
        SQL_TYPE_MAP.put(Types.FLOAT, Float.class);
        SQL_TYPE_MAP.put(Types.REAL, Float.class);
        SQL_TYPE_MAP.put(Types.DOUBLE, Double.class);
        SQL_TYPE_MAP.put(Types.NUMERIC, BigDecimal.class);
        SQL_TYPE_MAP.put(Types.DECIMAL, BigDecimal.class);
        SQL_TYPE_MAP.put(Types.CHAR, String.class);
        SQL_TYPE_MAP.put(Types.VARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.LONGVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.NCHAR, String.class);
        SQL_TYPE_MAP.put(Types.NVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.LONGNVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.CLOB, String.class);
        SQL_TYPE_MAP.put(Types.NCLOB, String.class);
        SQL_TYPE_MAP.put(Types.DATE, java.sql.Date.class);
        SQL_TYPE_MAP.put(Types.TIME, Time.class);
        SQL_TYPE_MAP.put(Types.TIMESTAMP, Timestamp.class);
        SQL_TYPE_MAP.put(Types.TIME_WITH_TIMEZONE, Time.class);
        SQL_TYPE_MAP.put(Types.TIMESTAMP_WITH_TIMEZONE, Timestamp.class);
        SQL_TYPE_MAP.put(Types.BOOLEAN, Boolean.class);
        SQL_TYPE_MAP.put(Types.BIT, Boolean.class);
        SQL_TYPE_MAP.put(Types.BINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.VARBINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.LONGVARBINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.BLOB, byte[].class);
        TYPE_NAME_MAP.put("NUMBER", BigDecimal.class);
        TYPE_NAME_MAP.put("VARCHAR2", String.class);
        TYPE_NAME_MAP.put("CHAR", String.class);
        TYPE_NAME_MAP.put("DATE", Timestamp.class);
        TYPE_NAME_MAP.put("TIMESTAMP", Timestamp.class);
        TYPE_NAME_MAP.put("CLOB", String.class);
        TYPE_NAME_MAP.put("BLOB", byte[].class);
    }
    public static Class<?> getJavaType(int sqlType) {
        Class<?> javaType = SQL_TYPE_MAP.get(sqlType);
        if (javaType == null) {
            log.warn("Unknown SQL type: {}, defaulting to Object", sqlType);
            return Object.class;
        }
        return javaType;
    }
    public static Class<?> getJavaType(String typeName) {
        if (typeName == null) {
            return Object.class;
        }
        String upperTypeName = typeName.toUpperCase();
        Class<?> javaType = TYPE_NAME_MAP.get(upperTypeName);
        if (javaType == null) {
            try {
                JDBCType jdbcType = JDBCType.valueOf(upperTypeName);
                javaType = getJavaType(jdbcType.getVendorTypeNumber());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown type name: {}, defaulting to Object", typeName);
                return Object.class;
            }
        }
        return javaType;
    }
    @SuppressWarnings("unchecked")
    public static <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        if (targetType == String.class) {
            return (T) value.toString();
        }
        if (Number.class.isAssignableFrom(targetType)) {
            return convertToNumber(value, targetType);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) convertToBoolean(value);
        }
        if (java.util.Date.class.isAssignableFrom(targetType)) {
            return convertToDate(value, targetType);
        }
        if (LocalDate.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalDate(value);
        }
        if (LocalDateTime.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalDateTime(value);
        }
        if (LocalTime.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalTime(value);
        }
        try {
            return targetType.cast(value);
        } catch (ClassCastException e) {
            log.warn("Cannot convert {} to {}", value.getClass(), targetType);
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    private static <T> T convertToNumber(Object value, Class<T> targetType) {
        Number number;
        if (value instanceof Number) {
            number = (Number) value;
        } else if (value instanceof String) {
            try {
                number = new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return (T) Byte.valueOf(number.byteValue());
        } else if (targetType == Short.class || targetType == short.class) {
            return (T) Short.valueOf(number.shortValue());
        } else if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(number.intValue());
        } else if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(number.longValue());
        } else if (targetType == Float.class || targetType == float.class) {
            return (T) Float.valueOf(number.floatValue());
        } else if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(number.doubleValue());
        } else if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return (T) value;
            }
            return (T) new BigDecimal(number.toString());
        }
        return null;
    }
    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value instanceof String) {
            String str = value.toString().toLowerCase();
            return "true".equals(str) || "yes".equals(str) || "y".equals(str) || "1".equals(str);
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private static <T> T convertToDate(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        long millis;
        if (value instanceof java.util.Date) {
            millis = ((java.util.Date) value).getTime();
        } else if (value instanceof Number) {
            millis = ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                millis = Timestamp.valueOf(value.toString()).getTime();
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
        if (targetType == java.sql.Date.class) {
            return (T) new java.sql.Date(millis);
        } else if (targetType == Time.class) {
            return (T) new Time(millis);
        } else if (targetType == Timestamp.class) {
            return (T) new Timestamp(millis);
        } else if (targetType == java.util.Date.class) {
            return (T) new java.util.Date(millis);
        }
        return null;
    }
    private static LocalDate convertToLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        } else if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalDate();
        } else if (value instanceof String) {
            return LocalDate.parse(value.toString());
        }
        return null;
    }
    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        } else if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime()).toLocalDateTime();
        } else if (value instanceof String) {
            return LocalDateTime.parse(value.toString());
        }
        return null;
    }
    private static LocalTime convertToLocalTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else if (value instanceof Time) {
            return ((Time) value).toLocalTime();
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalTime();
        } else if (value instanceof String) {
            return LocalTime.parse(value.toString());
        }
        return null;
    }
    public static Class<?> extractJavaType(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        int sqlType = metaData.getColumnType(columnIndex);
        String typeName = metaData.getColumnTypeName(columnIndex);
        Class<?> javaType = getJavaType(typeName);
        if (javaType != Object.class) {
            return javaType;
        }
        return getJavaType(sqlType);
    }
    public static Object getResultSetValue(ResultSet rs, int columnIndex, Class<?> targetType) throws SQLException {
        Object value = rs.getObject(columnIndex);
        if (value == null || rs.wasNull()) {
            return null;
        }
        if (targetType != null && targetType != Object.class) {
            return convertValue(value, targetType);
        }
        return value;
    }
    public static void validateQuery(QueryDefinitionBuilder queryDef) {
        String queryName = queryDef.getName();
        Set<String> sqlBindParams = extractBindParameters(queryDef.getSql());
        Set<String> criteriaBindParams = new HashSet<>();
        if (queryDef.getCriteria() != null) {
            for (Map.Entry<String, CriteriaDef> entry : queryDef.getCriteria().entrySet()) {
                CriteriaDef criteria = entry.getValue();
                if (criteria.sql() != null) {
                    Set<String> params = extractBindParameters(criteria.sql());
                    criteriaBindParams.addAll(params);
                }
            }
        }
        Set<String> allBindParams = new HashSet<>();
        allBindParams.addAll(sqlBindParams);
        allBindParams.addAll(criteriaBindParams);
        Set<String> definedParams = new HashSet<>();
        if (queryDef.getParameters() != null) {
            definedParams.addAll(queryDef.getParameters().keySet());
        }
        Set<String> systemParams = Set.of("offset", "limit", "_start", "_end");
        Set<String> undefinedParams = new HashSet<>(allBindParams);
        undefinedParams.removeAll(definedParams);
        undefinedParams.removeAll(systemParams);
        Set<String> filterGeneratedParams = new HashSet<>();
        if (queryDef.getAttributes() != null) {
            for (String attrName : queryDef.getAttributes().keySet()) {
                filterGeneratedParams.add(attrName);
                filterGeneratedParams.add(attrName + "_eq");
                filterGeneratedParams.add(attrName + "_ne");
                filterGeneratedParams.add(attrName + "_gt");
                filterGeneratedParams.add(attrName + "_gte");
                filterGeneratedParams.add(attrName + "_lt");
                filterGeneratedParams.add(attrName + "_lte");
                filterGeneratedParams.add(attrName + "_like");
                filterGeneratedParams.add(attrName + "_in");
                filterGeneratedParams.add(attrName + "_between_1");
                filterGeneratedParams.add(attrName + "_between_2");
                filterGeneratedParams.add(attrName + "_null");
                filterGeneratedParams.add(attrName + "_notnull");
            }
        }
        undefinedParams.removeAll(filterGeneratedParams);
        if (!undefinedParams.isEmpty()) {
            String errorMsg = String.format("""
                    Query '%s' uses undefined bind parameters: %s
                    Defined parameters: %s
                    Used in SQL: %s
                    Used in criteria: %s
                    Make sure all :paramName references have corresponding ParamDef definitions.""",
                    queryName, undefinedParams, definedParams, sqlBindParams, criteriaBindParams);
            throw new IllegalStateException(errorMsg);
        }
    }
    public static Set<String> findUnusedParameters(QueryDefinitionBuilder queryDef) {
        Set<String> sqlBindParams = extractBindParameters(queryDef.getSql());
        Set<String> criteriaBindParams = new HashSet<>();
        if (queryDef.getCriteria() != null) {
            for (Map.Entry<String, CriteriaDef> entry : queryDef.getCriteria().entrySet()) {
                CriteriaDef criteria = entry.getValue();
                if (criteria.sql() != null) {
                    Set<String> params = extractBindParameters(criteria.sql());
                    criteriaBindParams.addAll(params);
                }
            }
        }
        Set<String> allBindParams = new HashSet<>();
        allBindParams.addAll(sqlBindParams);
        allBindParams.addAll(criteriaBindParams);
        Set<String> definedParams = new HashSet<>();
        if (queryDef.getParameters() != null) {
            definedParams.addAll(queryDef.getParameters().keySet());
        }
        Set<String> systemParams = Set.of("offset", "limit", "_start", "_end");
        Set<String> unusedParams = new HashSet<>(definedParams);
        unusedParams.removeAll(allBindParams);
        unusedParams.removeAll(systemParams);
        return unusedParams;
    }
    public static Set<String> extractBindParameters(String sql) {
        Set<String> params = new HashSet<>();
        if (sql == null || sql.isEmpty()) {
            return params;
        }
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return params;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/builder/QueryResponseBuilder.java

```java
@Slf4j
public class QueryResponseBuilder {
    private final ResponseFormatter formatter = new ResponseFormatter();
    public ResponseEntity<QueryResponse> build(QueryData result, String queryName) {
        QueryResponse response = buildFormattedResponse(result);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    public ResponseEntity<QueryResponse> buildSingle(QueryData result, String queryName) {
        QueryResponse response = buildFormattedSingleResponse(result);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    public ResponseEntity<QueryResponse> buildSelectResponse(QueryData queryResult) {
        QueryResponse response = buildFormattedSelectResponse(queryResult);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    private QueryResponse buildFormattedResponse(QueryData result) {
        Object securityContext = result.getContext() != null ? result.getContext().getSecurityContext() : null;
        QueryDefinitionBuilder definition = result.getContext() != null ? result.getContext().getDefinition() : null;
        List<?> formattedData;
        if (definition != null && !result.getRows().isEmpty()) {
            formattedData = formatter.formatRows(result.getRows(), definition, securityContext);
        } else {
            formattedData = result.getData();
        }
        return QueryResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .count(result.getCount())
                .success(result.isSuccess())
                .build();
    }
    private QueryResponse buildFormattedSingleResponse(QueryData result) {
        if (result == null || result.getRows().isEmpty()) {
            throw new QueryException("No data found", "NOT_FOUND", (String) null);
        }
        Object securityContext = result.getContext() != null ? result.getContext().getSecurityContext() : null;
        QueryDefinitionBuilder definition = result.getContext() != null ? result.getContext().getDefinition() : null;
        Object formattedData;
        if (definition != null) {
            formattedData = formatter.formatRow(result.getRows().get(0), definition, securityContext);
        } else {
            formattedData = result.getRows().get(0).toMap();
        }
        return QueryResponse.builder()
                .data(formattedData)
                .metadata(result.getMetadata())
                .success(result.isSuccess())
                .build();
    }
    private QueryResponse buildFormattedSelectResponse(QueryData queryResult) {
        QueryDefinitionBuilder definition = queryResult.getContext().getDefinition();
        String valueAttr = definition.getValueAttribute();
        String labelAttr = definition.getLabelAttribute();
        if (valueAttr == null || labelAttr == null) {
            throw new QueryException("Select query must have value and label attributes defined",
                    "INVALID_SELECT_DEFINITION", definition.getName());
        }
        Object securityContext = queryResult.getContext() != null ? queryResult.getContext().getSecurityContext() : null;
        List<SelectItem> selectItems = new ArrayList<>();
        for (QueryRow row : queryResult.getRows()) {
            Map<String, Object> formattedRow = formatter.formatRow(row, definition, securityContext);
            String value = String.valueOf(formattedRow.get(valueAttr));
            String label = String.valueOf(formattedRow.get(labelAttr));
            selectItems.add(SelectItem.of(value, label));
        }
        QueryMetadata selectMetadata = null;
        if (queryResult.hasMetadata() && queryResult.getMetadata().getPagination() != null) {
            selectMetadata = QueryMetadata.builder()
                    .pagination(queryResult.getMetadata().getPagination())
                    .build();
        }
        return QueryResponse.builder()
                .data(selectItems)
                .metadata(selectMetadata)
                .count(queryResult.getCount())
                .success(true)
                .build();
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/controller/QueryController.java

```java
@RestController
@RequestMapping("/api/query/v2")
@Tag(name = "Query API", description = "Query Registration System API")
public class QueryController {
    private static final Logger log = LoggerFactory.getLogger(QueryController.class);
    private final QueryService queryService;
    private final QueryRequestParser requestParser;
    private final QueryResponseBuilder responseBuilder;
    public QueryController(QueryService queryService,
            QueryRequestParser requestParser,
            QueryResponseBuilder responseBuilder) {
        this.queryService = queryService;
        this.requestParser = requestParser;
        this.responseBuilder = responseBuilder;
    }
    @GetMapping("/{queryName}")
    @Operation(summary = "Execute a query", description = "Execute a registered query with filters, sorting, and pagination")
    public ResponseEntity<?> executeQuery(
            @PathVariable @Parameter(description = "Name of the registered query") String queryName,
            @RequestParam(defaultValue = "0") @Parameter(description = "Start index for pagination") int _start,
            @RequestParam(defaultValue = "50") @Parameter(description = "End index for pagination") int _end,
            @RequestParam(defaultValue = "full") @Parameter(description = "Metadata level: full, minimal, none") String _meta,
            @RequestParam MultiValueMap<String, String> allParams) {
        log.info("Executing query: {} with params: {}", queryName, allParams);
        try {
            QueryDefinitionBuilder queryDefinition = queryService.getQueryDefinition(queryName);
            if (queryDefinition == null) {
                log.error("Query not found: {}", queryName);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse(
                                new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND, "Query not found: " + queryName)));
            }
            QueryRequest queryRequest = requestParser.parse(allParams, _start, _end, _meta, queryDefinition);
            QueryData result = queryService.executeQuery(queryName, queryRequest);
            return responseBuilder.build(result, queryName);
        } catch (QueryException e) {
            log.error("Query execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error executing query: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }
    @PostMapping("/{queryName}")
    @Operation(summary = "Execute a query with body", description = "Execute a query with complex parameters in request body")
    public ResponseEntity<?> executeQueryWithBody(
            @PathVariable String queryName,
            @RequestBody QueryRequestBody body) {
        log.info("Executing query with body: {}", queryName);
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .params(body.getParams())
                    .filters(body.getFilters())
                    .sorts(body.getSorts())
                    .pagination(body.getStart(), body.getEnd())
                    .build();
            QueryData result = queryService.executeQuery(queryName, queryRequest);
            return responseBuilder.build(result, queryName);
        } catch (QueryException e) {
            log.error("Query execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error executing query: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }
    @GetMapping("/{queryName}/find-by-key")
    @Operation(summary = "Find by key", description = "Find a single record using key criteria")
    public ResponseEntity<?> findByKey(
            @PathVariable @Parameter(description = "Name of the registered query") String queryName,
            @RequestParam(defaultValue = "false") @Parameter(description = "Include metadata") boolean _meta,
            @RequestParam MultiValueMap<String, String> keyParams) {
        log.info("Finding by key for query: {} with params: {}", queryName, keyParams);
        try {
            QueryDefinitionBuilder queryDefinition = queryService.getQueryDefinition(queryName);
            if (queryDefinition == null) {
                log.error("Query not found: {}", queryName);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse(
                                new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND, "Query not found: " + queryName)));
            }
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : keyParams.entrySet()) {
                if (!entry.getKey().startsWith("_")) { 
                    params.put(entry.getKey(), entry.getValue().get(0));
                }
            }
            QueryRequest queryRequest = QueryRequest.builder()
                    .params(params)
                    .pagination(0, 1) 
                    .build();
            QueryData result = queryService.executeQuery(queryName, queryRequest);
            return responseBuilder.buildSingle(result, queryName);
        } catch (QueryException e) {
            log.error("Find-by-key execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error in find-by-key: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }
    @GetMapping("/{queryName}/metadata")
    @Operation(summary = "Get query metadata", description = "Get metadata for a registered query without executing it")
    public ResponseEntity<?> getQueryMetadata(@PathVariable String queryName) {
        try {
            return ResponseEntity.ok(Map.of(
                    "queryName", queryName,
                    "message", "Metadata endpoint - to be implemented"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }
    private HttpStatus determineHttpStatus(QueryException e) {
        String errorCode = e.getErrorCode();
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR", "DEFINITION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "SECURITY_ERROR" -> HttpStatus.FORBIDDEN;
            case "TIMEOUT_ERROR" -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    private QueryResponse buildErrorResponse(Exception e) {
        String errorCode = "INTERNAL_ERROR";
        String errorMessage = e.getMessage();
        if (e instanceof QueryException qe) {
            errorCode = qe.getErrorCode();
        }
        return QueryResponse.error(errorCode, errorMessage);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/controller/SelectController.java

```java
@RestController
@RequestMapping("/api/select/v2")
@Tag(name = "Select API", description = "List of Values endpoints for dropdowns and select components")
public class SelectController {
    private static final Logger log = LoggerFactory.getLogger(SelectController.class);
    private final QueryService queryService;
    private final QueryResponseBuilder responseBuilder;
    private final QueryRequestParser requestParser;
    public SelectController(QueryService queryService,
            QueryResponseBuilder responseBuilder,
            QueryRequestParser requestParser) {
        this.queryService = queryService;
        this.responseBuilder = responseBuilder;
        this.requestParser = requestParser;
    }
    @GetMapping("/{selectName}")
    @Operation(summary = "Get list of values", description = "Execute a select query for dropdowns/selects")
    public ResponseEntity<?> getListOfValues(
            @PathVariable @Parameter(description = "Name of the registered select") String selectName,
            @RequestParam(required = false) @Parameter(description = "IDs to fetch (for default values)") List<String> id,
            @RequestParam(required = false) @Parameter(description = "Search term to filter results") String search,
            @RequestParam(defaultValue = "0") @Parameter(description = "Start index for pagination") int _start,
            @RequestParam(defaultValue = "100") @Parameter(description = "End index for pagination") int _end,
            @RequestParam MultiValueMap<String, String> allParams) {
        log.info("Executing select: {} with ids: {}, search: {}, pagination: {}-{}",
                selectName, id, search, _start, _end);
        try {
            QueryDefinitionBuilder queryDefinition = queryService.getQueryDefinition(selectName);
            if (queryDefinition == null) {
                log.error("Select query not found: {}", selectName);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(buildErrorResponse(new QueryException(
                                selectName, QueryException.ErrorCode.QUERY_NOT_FOUND, "Select query not found: " + selectName)));
            }
            QueryRequest queryRequest = requestParser.parse(allParams, _start, _end, "none", queryDefinition);
            if (!queryDefinition.hasValueAttribute() || !queryDefinition.hasLabelAttribute()) {
                log.warn("Query {} not configured for select mode, using default attributes", selectName);
            }
            String valueAttr = queryDefinition.getValueAttribute() != null ? queryDefinition.getValueAttribute() : "value";
            String labelAttr = queryDefinition.getLabelAttribute() != null ? queryDefinition.getLabelAttribute() : "label";
            if (id != null && !id.isEmpty()) {
                log.debug("Fetching by IDs: {}", id);
                queryRequest.getFilters().put(valueAttr,
                    QueryContext.Filter.builder()
                        .attribute(valueAttr)
                        .operator(FilterOp.IN)
                        .values(id.stream().map(s -> (Object) s).toList())
                        .build());
            }
            else if (search != null && !search.isEmpty()) {
                log.debug("Searching with term: {}", search);
                boolean hasSearchParam = queryDefinition.getParameters().containsKey("search");
                boolean hasSearchCriteria = queryDefinition.getCriteria().containsKey("search") ||
                        queryDefinition.getCriteria().containsKey("searchFilter");
                if (hasSearchParam || hasSearchCriteria) {
                    queryRequest.getParams().put("search", "%" + search + "%");
                } else {
                    queryRequest.getFilters().put(labelAttr,
                        QueryContext.Filter.builder()
                            .attribute(labelAttr)
                            .operator(FilterOp.LIKE)
                            .value("%" + search + "%")
                            .build());
                }
            }
            QueryData queryResult = queryService.executeAsSelect(selectName, queryRequest);
            return responseBuilder.buildSelectResponse(queryResult);
        } catch (QueryException e) {
            log.error("Select execution failed: {}", e.getMessage());
            return ResponseEntity
                    .status(determineHttpStatus(e))
                    .body(buildErrorResponse(e));
        } catch (Exception e) {
            log.error("Unexpected error executing select: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(e));
        }
    }
    private HttpStatus determineHttpStatus(QueryException e) {
        String errorCode = e.getErrorCode();
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR", "DEFINITION_ERROR", "LOV_NOT_SUPPORTED" -> HttpStatus.BAD_REQUEST;
            case "SECURITY_ERROR" -> HttpStatus.FORBIDDEN;
            case "TIMEOUT_ERROR" -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    private QueryResponse buildErrorResponse(Exception e) {
        String errorCode = "INTERNAL_ERROR";
        String errorMessage = e.getMessage();
        if (e instanceof QueryException qe) {
            errorCode = qe.getErrorCode();
        }
        return QueryResponse.error(errorCode, errorMessage);
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/dto/request/QueryRequest.java

```java
@Data
@Builder
public class QueryRequest {
    private Map<String, Object> params;
    private Map<String, QueryContext.Filter> filters;
    private List<QueryContext.SortSpec> sorts;
    private Pagination pagination;
    private String metadataLevel;
    private Set<String> selectedFields;
    public static class QueryRequestBuilder {
        public QueryRequestBuilder pagination(int start, int end) {
            this.pagination = Pagination.builder()
                    .start(start)
                    .end(end)
                    .build();
            return this;
        }
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/dto/request/QueryRequestBody.java

```java
@Data
public class QueryRequestBody {
    private Map<String, Object> params;
    private Map<String, QueryContext.Filter> filters;
    private List<QueryContext.SortSpec> sorts;
    private int start = 0;
    private int end = 50;
    private boolean includeMetadata = true;
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/dto/response/QueryResponse.java

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse {
    private Object data;
    private Integer count;
    private QueryMetadata metadata;
    @Builder.Default
    private boolean success = true;
    private String errorCode;
    private String errorMessage;
    private long timestamp;
    public static QueryResponse success(List<?> data, QueryMetadata metadata, Integer count) {
        return QueryResponse.builder()
                .data(data)
                .metadata(metadata)
                .count(count)
                .success(true)
                .build();
    }
    public static QueryResponse success(List<?> data, Integer count) {
        return QueryResponse.builder()
                .data(data)
                .count(count)
                .success(true)
                .build();
    }
    public static QueryResponse success(Object singleData) {
        return QueryResponse.builder()
                .data(singleData)
                .count(1)
                .success(true)
                .build();
    }
    public static QueryResponse error(String errorCode, String errorMessage) {
        return QueryResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/formatter/ResponseFormatter.java

```java
@Slf4j
public class ResponseFormatter {
    public List<Map<String, Object>> formatRows(List<QueryRow> rows, QueryDefinitionBuilder definition, Object securityContext) {
        List<Map<String, Object>> formattedData = new ArrayList<>();
        for (QueryRow row : rows) {
            Map<String, Object> formattedRow = formatRow(row, definition, securityContext);
            formattedData.add(formattedRow);
        }
        return formattedData;
    }
    public Map<String, Object> formatRow(QueryRow row, QueryDefinitionBuilder definition, Object securityContext) {
        Map<String, Object> formattedData = new HashMap<>();
        Set<String> definedAttributes = new HashSet<>();
        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();
            definedAttributes.add(attrName.toUpperCase());
            if (attr.isSecured() && securityContext != null) {
                Boolean allowed = attr.securityRule().apply(securityContext);
                if (!Boolean.TRUE.equals(allowed)) {
                    formattedData.put(attrName, null);
                    continue;
                }
            }
            Object value;
            if (attr.aliasName() != null && !attr.virtual()) {
                value = row.getRaw(attr.aliasName());
            } else if (attr.virtual()) {
                value = row.get(attrName);
            } else {
                value = row.getRaw(attrName.toUpperCase());
                if (value == null) {
                    value = row.get(attrName);
                }
            }
            if (value != null && attr.hasFormatter()) {
                try {
                    AttributeFormatter formatter = attr.formatter();
                    value = formatter.format(value);
                } catch (Exception e) {
                    log.warn("Failed to format attribute {}: {}", attrName, e.getMessage());
                }
            }
            formattedData.put(attrName, value);
        }
        return formattedData;
    }
}```

---

## src/main/java/com/balsam/oasis/common/registry/web/parser/QueryRequestParser.java

```java
public class QueryRequestParser {
    private static final Pattern FILTER_PATTERN = Pattern.compile("^filter\\.(.+?)(?:\\.(\\w+))?$");
    private static final Pattern SORT_PATTERN = Pattern.compile("^([^.]+)\\.(asc|desc)$");
    public QueryRequest parse(MultiValueMap<String, String> allParams, int start, int end, String metadataLevel) {
        return parse(allParams, start, end, metadataLevel, null);
    }
    public QueryRequest parse(MultiValueMap<String, String> allParams, int start, int end, String metadataLevel,
            QueryDefinitionBuilder queryDefinition) {
        Map<String, Object> params = new HashMap<>();
        Map<String, QueryContext.Filter> filters = new LinkedHashMap<>();
        List<QueryContext.SortSpec> sorts = new ArrayList<>();
        Set<String> selectedFields = null;
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            String paramName = entry.getKey();
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            String value = values.get(0); 
            if (paramName.equals("_select")) {
                if (selectedFields == null) {
                    selectedFields = new HashSet<>();
                }
                String fieldsValue = values.get(0);
                if (fieldsValue != null && !fieldsValue.isEmpty()) {
                    selectedFields.addAll(Arrays.asList(fieldsValue.split(",")));
                }
                continue;
            }
            if (paramName.startsWith("_")) {
                continue;
            }
            Matcher filterMatcher = FILTER_PATTERN.matcher(paramName);
            if (filterMatcher.matches()) {
                String attribute = filterMatcher.group(1);
                String opPart = filterMatcher.group(2);
                if (opPart != null && opPart.equals("op")) {
                    String valueKey = "filter." + attribute + ".value";
                    String valueKey2 = "filter." + attribute + ".value2";
                    FilterOp op = FilterOp.valueOf(value.toUpperCase());
                    Object filterValue = null;
                    Object filterValue2 = null;
                    if (allParams.containsKey(valueKey)) {
                        Class<?> attrType = getAttributeType(queryDefinition, attribute);
                        filterValue = parseValue(allParams.getFirst(valueKey), attrType);
                    }
                    if (allParams.containsKey(valueKey2)) {
                        Class<?> attrType = getAttributeType(queryDefinition, attribute);
                        filterValue2 = parseValue(allParams.getFirst(valueKey2), attrType);
                    }
                    filters.put(attribute, QueryContext.Filter.builder()
                            .attribute(attribute)
                            .operator(op)
                            .value(filterValue)
                            .value2(filterValue2)
                            .build());
                } else if (opPart != null) {
                    try {
                        FilterOp op = FilterOp.fromUrlShortcut(opPart);
                        Class<?> attrType = getAttributeType(queryDefinition, attribute);
                        if (op == FilterOp.IN || op == FilterOp.NOT_IN) {
                            List<Object> valuesList;
                            if (value.contains(",")) {
                                valuesList = Arrays.stream(value.split(","))
                                        .map(String::trim)
                                        .map(v -> parseValue(v, attrType))
                                        .collect(Collectors.toList());
                            } else {
                                valuesList = Collections.singletonList(parseValue(value, attrType));
                            }
                            filters.put(attribute, QueryContext.Filter.builder()
                                    .attribute(attribute)
                                    .operator(op)
                                    .values(valuesList)
                                    .build());
                        } else {
                            if (filters.containsKey(attribute)) {
                                System.err.println("Warning: Multiple filters on attribute '" + attribute +
                                        "'. Only the last one will be applied.");
                            }
                            filters.put(attribute, QueryContext.Filter.builder()
                                    .attribute(attribute)
                                    .operator(op)
                                    .value(parseValue(value, attrType))
                                    .build());
                        }
                    } catch (IllegalArgumentException e) {
                        throw new QueryException(queryDefinition.getName(),
                                QueryException.ErrorCode.VALIDATION_ERROR,
                                "Invalid filter operator '" + opPart + "' for attribute '" + attribute + "'");
                    }
                } else {
                    Class<?> attrType = getAttributeType(queryDefinition, attribute);
                    parseSimpleFilter(attribute, values, filters, attrType);
                }
                continue;
            }
            if (paramName.equals("sort")) {
                parseSortParameter(value, sorts);
                continue;
            }
            if (value != null && !value.trim().isEmpty()) {
                Class<?> paramType = getParamType(queryDefinition, paramName);
                if (paramType != null) {
                    if (List.class.isAssignableFrom(paramType)) {
                        if (value.contains(",")) {
                            List<String> valueList = Arrays.stream(value.split(","))
                                    .map(String::trim)
                                    .collect(Collectors.toList());
                            params.put(paramName, valueList);
                        } else {
                            params.put(paramName, Collections.singletonList(value.trim()));
                        }
                    } else {
                        params.put(paramName, parseValue(value, paramType));
                    }
                }
            }
        }
        return QueryRequest.builder()
                .params(params)
                .filters(filters)
                .sorts(sorts)
                .pagination(start, end)
                .metadataLevel(metadataLevel)
                .selectedFields(selectedFields)
                .build();
    }
    private void parseSimpleFilter(String attribute, List<String> values, Map<String, QueryContext.Filter> filters,
            Class<?> targetType) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> nonEmptyValues = values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.toList());
        if (nonEmptyValues.isEmpty()) {
            return;
        }
        if (nonEmptyValues.size() == 1) {
            filters.put(attribute, QueryContext.Filter.builder()
                    .attribute(attribute)
                    .operator(FilterOp.EQUALS)
                    .value(parseValue(nonEmptyValues.get(0), targetType))
                    .build());
        } else {
            List<Object> parsedValues = nonEmptyValues.stream()
                    .map(v -> parseValue(v, targetType))
                    .collect(Collectors.toList());
            filters.put(attribute, QueryContext.Filter.builder()
                    .attribute(attribute)
                    .operator(FilterOp.IN)
                    .values(parsedValues)
                    .build());
        }
    }
    private void parseSortParameter(String value, List<QueryContext.SortSpec> sorts) {
        String[] sortSpecs = value.split(",");
        for (String spec : sortSpecs) {
            spec = spec.trim();
            Matcher sortMatcher = SORT_PATTERN.matcher(spec);
            if (sortMatcher.matches()) {
                String attribute = sortMatcher.group(1);
                String direction = sortMatcher.group(2);
                sorts.add(QueryContext.SortSpec.builder()
                        .attribute(attribute)
                        .direction(SortDir.fromUrlParam(direction))
                        .build());
            } else {
                sorts.add(QueryContext.SortSpec.builder()
                        .attribute(spec)
                        .direction(SortDir.ASC)
                        .build());
            }
        }
    }
    private Class<?> getParamType(QueryDefinitionBuilder queryDefinition, String paramName) {
        if (queryDefinition == null) {
            return null;
        }
        ParamDef<?> paramDef = queryDefinition.getParam(paramName);
        return paramDef != null ? paramDef.type() : null;
    }
    private Class<?> getAttributeType(QueryDefinitionBuilder queryDefinition, String attributeName) {
        if (queryDefinition == null) {
            return null;
        }
        AttributeDef<?> attributeDef = queryDefinition.getAttribute(attributeName);
        return attributeDef != null ? attributeDef.type() : null;
    }
    private Object parseValue(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (targetType != null) {
            return convertToType(value, targetType);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
        }
        try {
            if (value.length() == 10 && value.charAt(4) == '-' && value.charAt(7) == '-') {
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
        }
        try {
            if (value.contains("T")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
        }
        return value;
    }
    private Object convertToType(String value, Class<?> targetType) {
        if (targetType == null || value == null) {
            return value;
        }
        return QueryUtils.convertValue(value, targetType);
    }
}```

---

