# Query Registration System - Library Specification

## 1. Overview

### 1.1 Purpose

A Spring Boot library providing a declarative query registration system with JdbcTemplate that supports metadata, pagination, dynamic criteria, row processing, and automatic REST API publication with GET endpoints.

### 1.2 Current Implementation Status

#### ✅ Implemented Features
- SQL queries with comment-based placeholders (`--placeholderName`)
- Attribute name mapping (frontend names vs DB column names using `aliasName`)
- Builder pattern for QueryDefinition
- Automatic type conversion between SQL and Java types
- Generic attribute typing with `AttributeDef<T>`
- Pre/post processors and row processors
- Basic REST GET endpoints via QueryController
- Parameter validation and default values
- Dynamic criteria with bind parameters
- Database dialect support (Oracle 11g and 12c+)
- Integration with Spring Boot auto-configuration

#### 🚧 Partially Implemented
- Calculated and virtual attributes (structure exists, needs full implementation)
- Security integration (basic structure, needs SecurityContext implementation)
- Complex query support (SQL builder supports it, needs testing)
- Caching (CacheConfig exists, needs cache provider implementation)

#### ❌ Not Yet Implemented
- AttributeBuilder, ParamBuilder, CriteriaBuilder (only QueryDefinitionBuilder exists)
- Full security layer (SecurityContext, SecurityProvider, filters)
- Cache providers (CaffeineCacheProvider, CacheKeyGenerator)
- Filter and Sort parsers as separate components
- Export formats (JSON only)
- Comprehensive metadata in responses
- Query registry and discovery

## 2. Core Components

### 2.1 QueryDefinition

```java
public class QueryDefinition {
    private String name;
    private String sql;
    private Map<String, AttributeDef> attributes;
    private Map<String, ParamDef> params;
    private Map<String, CriteriaDef> criteria;
    private List<PreProcessor> preProcessors;
    private List<RowProcessor> rowProcessors;
    private List<PostProcessor> postProcessors;
    private Map<String, Calculator> calculators;
    private List<ValidationRule> validationRules;
    private CacheConfig cacheConfig;
}
```

### 2.2 AttributeDef (Current Implementation)

```java
@Value
public class AttributeDef<T> {
    String name;                    // Frontend/API name
    String aliasName;              // Database column name
    Class<T> type;                 // Java type with generics
    boolean filterable;
    boolean sortable;
    boolean virtual;              // Not from database
    boolean primaryKey;
    
    // Processors
    AttributeProcessor<T> processor;  // Single processor for all transformations
    AttributeFormatter<T> formatter;  // String formatting
    
    // Security
    Function<Object, Boolean> securityRule;
    
    String description;
}
```

### 2.3 ParamDef (Current Implementation)

```java
@Value
public class ParamDef<T> {
    String name;
    Class<T> type;              // Type-safe parameter type
    T defaultValue;             // Type-safe default value
    boolean required;
    Validator validator;
    ParamProcessor<T> processor; // Type-safe processor
    String description;
    // Note: genericType for collections not yet implemented
}
```

### 2.4 CriteriaDef

```java
public class CriteriaDef {
    private String name;
    private String sql;
    private Predicate<QueryContext> condition;
    private Set<String> bindParams;
    private Processor processor;
    private boolean dynamic;
    private CriteriaGenerator generator;
}
```

## 3. Builder API

### 3.1 Current Implementation - QueryDefinitionBuilder

```java
public class QueryDefinitionBuilder {
    // Core methods
    public QueryDefinitionBuilder name(String name);
    public QueryDefinitionBuilder sql(String sql);
    public QueryDefinitionBuilder dialect(DatabaseDialect dialect);
    
    // Inline attribute definition (current approach)
    public <T> QueryDefinitionBuilder attribute(String name, Class<T> type, 
                                                Function<AttributeDef.Builder<T>, AttributeDef.Builder<T>> customizer);
    
    // Parameter definition
    public <T> QueryDefinitionBuilder param(String name, Class<T> type,
                                            Function<ParamDef.ParamBuilder<T>, ParamDef.ParamBuilder<T>> customizer);
    
    // Criteria definition
    public QueryDefinitionBuilder criteria(String name, String sql);
    public QueryDefinitionBuilder criteria(String name, String sql, 
                                          Predicate<QueryContext> condition);
    
    // Processors
    public QueryDefinitionBuilder preProcessor(PreProcessor processor);
    public QueryDefinitionBuilder rowProcessor(RowProcessor processor);
    public QueryDefinitionBuilder postProcessor(PostProcessor processor);
    
    // Cache configuration
    public QueryDefinitionBuilder cache(CacheConfig config);
    
    // Build
    public QueryDefinition build();
}
```

### 3.2 Future Enhancement - Separate Builders

```java
// PLANNED: Fluent builder chain for attributes
public class AttributeBuilder<T> {
    public AttributeBuilder<T> dbColumn(String column);
    public AttributeBuilder<T> filterable(boolean filterable);
    public AttributeBuilder<T> sortable(boolean sortable);
    public AttributeBuilder<T> virtual(boolean virtual);
    public AttributeBuilder<T> primaryKey(boolean pk);
    public AttributeBuilder<T> secure(Function<SecurityContext, Boolean> rule);
    public AttributeBuilder<T> processor(AttributeProcessor<T> processor);
    public AttributeBuilder<T> formatter(AttributeFormatter<T> formatter);
    public QueryDefinitionBuilder and();  // Return to parent builder
}

// PLANNED: Similar builders for Param and Criteria
```

## 4. Processing Interfaces

### 4.1 Current Processing Interfaces

```java
// Implemented interfaces
@FunctionalInterface
public interface PreProcessor {
    void process(QueryContext context);
}

@FunctionalInterface
public interface RowProcessor {
    Row process(Row row, QueryContext context);
}

@FunctionalInterface
public interface PostProcessor {
    QueryResult process(QueryResult result, QueryContext context);
}

@FunctionalInterface
public interface AttributeProcessor<T> {
    T process(T value, Row row, QueryContext context);
}

@FunctionalInterface
public interface AttributeFormatter<T> {
    String format(T value);
}

@FunctionalInterface
public interface ParamProcessor<T> {
    T process(T value, QueryContext context);
}

@FunctionalInterface
public interface Validator {
    boolean validate(Object value);
}

@FunctionalInterface
public interface CriteriaGenerator {
    String generate(QueryContext context);
}
```

### 4.2 Planned Interfaces

```java
// PLANNED: Additional processing interfaces
@FunctionalInterface
public interface Calculator {
    Object calculate(Object value, Row row, QueryContext context);
}

@FunctionalInterface
public interface Converter<S, T> {
    T convert(S source);
}

@FunctionalInterface
public interface SecurityRule {
    boolean hasAccess(SecurityContext context);
}
```

### 4.2 Row Interface

```java
public interface Row {
    // Basic accessors
    Object get(String attributeName);
    <T> T get(String attributeName, Class<T> type);

    // Type-specific accessors
    String getString(String attributeName);
    Integer getInteger(String attributeName);
    Long getLong(String attributeName);
    BigDecimal getBigDecimal(String attributeName);
    LocalDate getLocalDate(String attributeName);
    LocalDateTime getLocalDateTime(String attributeName);
    Boolean getBoolean(String attributeName);

    // Raw DB access
    Object getRaw(String columnName);

    // Virtual fields
    Object getVirtual(String name);
    <T> T getVirtual(String name, Class<T> type);
    <T> T getVirtual(String name, T defaultValue);

    // Setters
    void set(String attributeName, Object value);
    void setVirtual(String name, Object value);

    // Context access
    QueryContext getContext();
}
```

## 5. Query Execution

### 5.1 QueryExecutor

```java
public interface QueryExecutor {
    QueryExecution execute(String queryName);
    QueryExecution execute(QueryDefinition definition);
    QueryExecution prepare(QueryDefinition definition);
}
```

### 5.2 QueryExecution Builder

```java
public class QueryExecution {
    // Parameter methods
    public QueryExecution withParam(String name, Object value);
    public QueryExecution withParams(Map<String, Object> params);

    // Filter methods
    public QueryExecution withFilter(String attributeName, FilterOp op, Object value);
    public QueryExecution withFilter(String attributeName, FilterOp op, Object value1, Object value2);
    public QueryExecution withFilters(Map<String, Filter> filters);
    public QueryExecution filterIf(boolean condition, String attr, FilterOp op, Object value);

    // Sort methods
    public QueryExecution withSort(String attributeName, SortDir direction);
    public QueryExecution withSort(List<SortSpec> sorts);

    // Pagination
    public QueryExecution withPagination(int start, int end);

    // Execution
    public QueryExecution validate();
    public QueryResult execute();
}
```

## 6. REST API

### 6.1 GET Endpoint Format

```
GET /api/query/{queryName}?
    // Pagination
    _start=0&
    _end=50&

    // Parameters
    param.scoreMultiplier=2.5&
    param.includeInactive=false&
    param.minOrderDate=2023-01-01&

    // Filters (multiple formats supported)
    filter.name=John&                    // Simple equals
    filter.name.op=LIKE&filter.name.value=%John%&  // With operator
    filter.status=ACTIVE,PENDING&        // IN operator (comma-separated)
    filter.joinDate.gte=2023-01-01&     // Range operators
    filter.joinDate.lte=2024-12-31&
    filter.lifetimeValue.gt=5000&

    // Sorting
    sort=lifetimeValue.desc,name.asc&

    // Include/exclude metadata
    _meta=full  // full, minimal, none

    // Response format is JSON only
```

### 6.2 Filter Operator Shortcuts

```
.eq     - equals
.ne     - not equals
.gt     - greater than
.gte    - greater than or equal
.lt     - less than
.lte    - less than or equal
.like   - SQL LIKE
.in     - IN (comma-separated values)
.between - BETWEEN (requires two values)
.null   - IS NULL
.notnull - IS NOT NULL
```

### 6.3 Response Format

```json
{
    "data": [
        {
            "id": 123,
            "name": "John Smith",
            "email": "j***@company.com",
            "status": "ACTIVE",
            "joinDate": "2023-05-15",
            "lastActive": "2024-12-20T14:30:00",
            "totalOrders": 67,
            "lifetimeValue": "$25,750.00",
            "averageOrderValue": "$384.33",
            "loyaltyScore": 3375,
            "membershipTier": "GOLD",
            "retentionScore": 92
        }
    ],
    "metadata": {
        "pagination": {
            "start": 0,
            "end": 50,
            "total": 245,
            "hasNext": true,
            "hasPrevious": false
        },
        "attributes": {
            "name": {
                "type": "String",
                "filterable": true,
                "sortable": true
            },
            "lifetimeValue": {
                "type": "Currency",
                "filterable": true,
                "sortable": true,
                "virtual": true
            },
            "email": {
                "type": "String",
                "filterable": false,
                "sortable": false,
                "restricted": true
            },
            "membershipTier": {
                "type": "String",
                "filterable": true,
                "sortable": false,
                "virtual": true
            }
        },
        "appliedCriteria": [
            {
                "name": "statusFilter",
                "sql": "AND u.account_status IN (:statuses)",
                "params": {
                    "statuses": ["ACTIVE", "PENDING"]
                }
            },
            {
                "name": "emailVerifiedFilter",
                "sql": "AND u.email_verified = :emailVerified",
                "params": {
                    "emailVerified": true
                }
            },
            {
                "name": "securityFilter",
                "sql": "AND u.department_id IN (SELECT id FROM departments WHERE manager_id = :currentUserId)",
                "params": {
                    "currentUserId": 456
                }
            }
        ],
        "appliedFilters": {
            "name": {
                "operator": "LIKE",
                "value": "John%"
            },
            "status": {
                "operator": "IN",
                "value": ["ACTIVE", "PENDING"]
            },
            "lifetimeValue": {
                "operator": "GREATER_THAN",
                "value": 5000
            }
        },
        "appliedSort": [
            {
                "field": "lifetimeValue",
                "direction": "DESC"
            },
            {
                "field": "name",
                "direction": "ASC"
            }
        ],
        "parameters": {
            "scoreMultiplier": {
                "value": 2.5,
                "defaultValue": 1.5,
                "type": "Double"
            },
            "includeInactive": {
                "value": false,
                "defaultValue": false,
                "type": "Boolean"
            }
        },
        "summary": {
            "averageLifetimeValue": "$8,543.21",
            "topTier": "PLATINUM",
            "customMetrics": {}
        },
        "performance": {
            "executionTimeMs": 145,
            "rowsFetched": 50,
            "totalRowsScanned": 1250,
            "cacheHit": false,
            "queryPlan": "INDEX_SCAN"
        }
    },
    "links": {
        "self": "/api/query/userDashboard?_start=0&_end=50&filter.name=John",
        "next": "/api/query/userDashboard?_start=50&_end=100&filter.name=John",
        "previous": null,
        "first": "/api/query/userDashboard?_start=0&_end=50&filter.name=John",
        "last": "/api/query/userDashboard?_start=200&_end=245&filter.name=John"
    }
}
```

## 7. Configuration

### 7.1 Auto-Configuration

```java
@Configuration
@EnableQueryRegistration
public class QueryConfig {

    @Bean
    public QueryRegistrationConfigurer configurer() {
        return new QueryRegistrationConfigurer()
            .scanPackage("com.example.queries")
            .enableRestApi(true)
            .restApiPrefix("/api/query")
            .enableCache(true)
            .defaultPageSize(50)
            .maxPageSize(1000)
            .enableMetrics(true)
            .enableSecurity(true)
            .enableSwaggerDocs(true);
    }

    @Bean
    public GlobalProcessors globalProcessors() {
        return GlobalProcessors.builder()
            .addCalculator("currencyFormatter", new CurrencyCalculator())
            .addCalculator("percentageFormatter", new PercentageCalculator())
            .addConverter(LocalDate.class, new LocalDateConverter())
            .addConverter(LocalDateTime.class, new LocalDateTimeConverter())
            .addValidator("email", new EmailValidator())
            .addValidator("phone", new PhoneValidator())
            .build();
    }
}
```

### 7.2 Properties Configuration

```yaml
query.registration:
  enabled: true
  rest:
    enabled: true
    prefix: /api/query
    default-page-size: 50
    max-page-size: 1000
    enable-cors: true
    enable-compression: true
    # JSON response format only
    parameter-format: standard  # standard, graphql-like
  cache:
    enabled: true
    provider: caffeine  # caffeine, redis, hazelcast
    default-ttl: 5m
    max-entries: 1000
  security:
    enabled: true
    check-permissions: true
    mask-sensitive-data: true
    audit-queries: true
  metadata:
    include-by-default: full  # full, minimal, none
    include-query-plan: false
    include-execution-stats: true
  validation:
    strict-mode: true
    validate-params: true
    validate-filters: true
  jdbc:
    fetch-size: 100
    query-timeout: 30s
    enable-sql-logging: true
```

## 8. Security Integration

### 8.1 Security Context

```java
public interface SecurityContext {
    String getCurrentUserId();
    Set<String> getUserRoles();
    boolean hasRole(String role);
    boolean hasAnyRole(String... roles);
    boolean hasPermission(String permission);
    Map<String, Object> getAttributes();
}
```

### 8.2 Attribute Security

```java
@Bean
public QueryDefinition secureQuery() {
    return QueryDefinition.builder("secureData")
        .attribute("salary")
            .secure(ctx -> ctx.hasAnyRole("HR", "ADMIN"))
            .processor(value -> ctx.hasRole("HR") ? value : "***")
            .build()
        .attribute("ssn")
            .secure(ctx -> ctx.hasPermission("VIEW_SSN"))
            .processor(value -> maskSSN(value))
            .build()
        .criteria("departmentFilter")
            .sql("AND department_id = :userDepartment")
            .condition(ctx -> !ctx.hasRole("ADMIN"))
            .processor(ctx -> ctx.setParam("userDepartment",
                ctx.getSecurityContext().getAttribute("departmentId")))
            .build()
        .build();
}
```

## 9. Advanced Features

### 9.1 Complex Query Support

```java
// Supports CTEs, UNION, Window Functions
@Bean
public QueryDefinition analyticsQuery() {
    return QueryDefinition.builder("analytics")
        .sql("""
            WITH RECURSIVE hierarchy AS (
                SELECT id, parent_id, name, 0 as level
                FROM categories
                WHERE parent_id IS NULL
                --rootFilter

                UNION ALL

                SELECT c.id, c.parent_id, c.name, h.level + 1
                FROM categories c
                INNER JOIN hierarchy h ON c.parent_id = h.id
                --levelFilter
            ),
            aggregated_data AS (
                SELECT
                    h.*,
                    COUNT(p.id) as product_count,
                    SUM(p.price) as total_value,
                    ROW_NUMBER() OVER (PARTITION BY h.level ORDER BY COUNT(p.id) DESC) as rank
                FROM hierarchy h
                LEFT JOIN products p ON p.category_id = h.id
                GROUP BY h.id, h.parent_id, h.name, h.level
                --havingClause
            )
            SELECT * FROM aggregated_data
            WHERE 1=1
            --finalFilters
            """)
        .build();
}
```

### 9.2 Dynamic Row Mapper

```java
public class DynamicRowMapper implements RowMapper<Map<String, Object>> {
    private final QueryDefinition definition;
    private final QueryContext context;

    @Override
    public Map<String, Object> mapRow(ResultSet rs, int rowNum) {
        Map<String, Object> row = new HashMap<>();

        definition.getAttributes().forEach((name, attr) -> {
            try {
                // Check security
                if (attr.getSecurityRule() != null &&
                    !attr.getSecurityRule().apply(context.getSecurityContext())) {
                    row.put(name, null);
                    return;
                }

                // Get value from ResultSet
                Object rawValue = rs.getObject(attr.getDbColumn());

                // Apply converter
                Object convertedValue = attr.getConverter() != null ?
                    attr.getConverter().convert(rawValue, attr.getType()) :
                    rawValue;

                // Apply processor
                if (attr.getProcessor() != null) {
                    convertedValue = attr.getProcessor().process(convertedValue);
                }

                // Apply calculator for virtual fields
                if (attr.isVirtual() && attr.getCalculator() != null) {
                    Row rowContext = new RowImpl(row, rs, context);
                    convertedValue = attr.getCalculator()
                        .calculate(convertedValue, rowContext, context);
                }

                // Apply formatter
                if (attr.getFormatter() != null) {
                    convertedValue = attr.getFormatter().format(convertedValue);
                }

                row.put(name, convertedValue);

            } catch (SQLException e) {
                throw new QueryExecutionException("Error mapping attribute: " + name, e);
            }
        });

        // Process virtual attributes
        definition.getVirtualAttributes().forEach((name, attr) -> {
            if (attr.getCalculator() != null) {
                Row rowContext = new RowImpl(row, rs, context);
                Object value = attr.getCalculator()
                    .calculate(null, rowContext, context);
                row.put(name, value);
            }
        });

        return row;
    }
}
```

## 10. Usage Examples

### 10.1 Simple Query Definition

```java
@Bean
public QueryDefinition simpleUserQuery() {
    return QueryDefinition.builder("users")
        .sql("SELECT * FROM users WHERE active = true ")
        .attribute("id").dbColumn("user_id").type(Long.class).primaryKey(true).build()
        .attribute("name").dbColumn("full_name").type(String.class)
            .filterable(true).sortable(true).build()
        .attribute("email").dbColumn("email").type(String.class)
            .filterable(true).build()
        .build();
}
```

### 10.2 REST API Usage

```bash
# Simple query
GET /api/query/users?_start=0&_end=10

# With filters and sorting
GET /api/query/userDashboard?
    filter.name.like=%John%&
    filter.status=ACTIVE,PENDING&
    filter.joinDate.gte=2023-01-01&
    filter.lifetimeValue.gt=5000&
    sort=lifetimeValue.desc,name.asc&
    param.scoreMultiplier=2.5&
    _start=0&_end=50&_meta=full
```

### 10.3 Programmatic Usage

```java
// Simple execution
QueryResult result = queryExecutor.execute("users")
    .withFilter("name", FilterOp.LIKE, "%John%")
    .withPagination(0, 10)
    .execute();

// Complex execution with all features
QueryResult result = queryExecutor.execute("userDashboard")
    .withParam("scoreMultiplier", 2.5)
    .withFilter("status", FilterOp.IN, Arrays.asList("ACTIVE", "PENDING"))
    .withFilter("joinDate", FilterOp.BETWEEN,
        LocalDate.of(2023, 1, 1), LocalDate.now())
    .filterIf(request.hasVipFilter(),
        "membershipTier", FilterOp.EQUALS, "GOLD")
    .withSort("lifetimeValue", SortDir.DESC)
    .withSort("name", SortDir.ASC)
    .withPagination(0, 50)
    .validate()
    .execute();

// Access results
result.getRows().forEach(row -> {
    Long id = row.getLong("id");
    String name = row.getString("name");
    BigDecimal ltv = row.getBigDecimal("lifetimeValue");
    String tier = row.getVirtual("membershipTier", "BRONZE");
});
```

## 11. Testing Support

### 11.1 Test Configuration

```java
@TestConfiguration
public class QueryTestConfig {

    @Bean
    @Primary
    public QueryExecutor mockQueryExecutor() {
        return MockQueryExecutor.builder()
            .mockQuery("userDashboard")
                .returns(TestDataBuilder.users())
                .withMetadata(TestDataBuilder.userMetadata())
            .build();
    }
}
```

### 11.2 Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class QueryApiTest {

    @Test
    void testQueryExecution() {
        mockMvc.perform(get("/api/query/userDashboard")
                .param("filter.name", "John")
                .param("filter.status", "ACTIVE")
                .param("sort", "lifetimeValue.desc")
                .param("_start", "0")
                .param("_end", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].name").value("John Smith"))
            .andExpect(jsonPath("$.metadata.appliedCriteria").isArray())
            .andExpect(jsonPath("$.metadata.appliedCriteria[0].name").exists())
            .andExpect(jsonPath("$.metadata.appliedFilters.name.value").value("John"))
            .andExpect(jsonPath("$.metadata.pagination.total").isNumber());
    }
}
```

## 12. Migration from Direct JDBC

### 12.1 Before (Direct JdbcTemplate)

```java
String sql = "SELECT * FROM users WHERE status = ? AND created_date > ? ORDER BY name LIMIT ?, ?";
List<User> users = jdbcTemplate.query(sql,
    new Object[]{"ACTIVE", startDate, offset, limit},
    new BeanPropertyRowMapper<>(User.class));
```

### 12.2 After (Query Registration)

```java
// Define once
@Bean
public QueryDefinition userQuery() {
    return QueryDefinition.builder("activeUsers")
        .sql("SELECT * FROM users WHERE 1=1 --statusFilter --dateFilter")
        .attribute("id").dbColumn("id").type(Long.class).build()
        .attribute("name").dbColumn("name").type(String.class).sortable(true).build()
        .criteria("statusFilter").sql("AND status = :status").build()
        .criteria("dateFilter").sql("AND created_date > :startDate").build()
        .build();
}

// Use many times
QueryResult result = queryExecutor.execute("activeUsers")
    .withParam("status", "ACTIVE")
    .withParam("startDate", startDate)
    .withSort("name", SortDir.ASC)
    .withPagination(offset, offset + limit)
    .execute();
```

## 13. Performance Considerations

### 13.1 Query Optimization

- Prepared statements with parameter binding
- Connection pooling integration
- Result set streaming for large data
- Configurable fetch size
- Query timeout management

### 13.2 Caching Strategy

- Query result caching with configurable TTL
- Cache key generation based on params/filters
- Cache invalidation hooks
- Support for multiple cache providers

### 13.3 Monitoring

- Query execution metrics
- Slow query logging
- Filter/criteria usage statistics
- Cache hit/miss ratios

## 14. Error Handling

### 14.1 Exception Hierarchy

```java
QueryException (base)
├── QueryDefinitionException
├── QueryValidationException
├── QueryExecutionException
├── QuerySecurityException
└── QueryTimeoutException
```

### 14.2 Error Response Format

```json
{
    "error": {
        "code": "QUERY_VALIDATION_ERROR",
        "message": "Required parameter 'minOrderDate' is missing",
        "details": {
            "queryName": "userDashboard",
            "missingParams": ["minOrderDate"],
            "invalidFilters": [],
            "timestamp": "2024-12-20T10:30:00Z"
        }
    }
}
```

## 15. Future Development Phases

### Phase 1: Complete Core Builders (Priority: High)
- [ ] Implement `AttributeBuilder<T>` for fluent attribute configuration
- [ ] Implement `ParamBuilder<T>` for parameter configuration
- [ ] Implement `CriteriaBuilder` for dynamic criteria
- [ ] Create `VirtualAttributeBuilder` for calculated fields
- [ ] Add builder validation at compile-time

### Phase 2: Security Layer (Priority: High)
- [ ] Create `SecurityContext` interface
- [ ] Implement `SecurityContextProvider` for Spring Security integration
- [ ] Add `AttributeSecurityFilter` for field-level security
- [ ] Create `CriteriaSecurityFilter` for row-level security
- [ ] Implement role-based access control (RBAC)
- [ ] Add audit logging for sensitive queries

### Phase 3: Caching Implementation (Priority: Medium)
- [ ] Create `QueryCacheManager` interface
- [ ] Implement `CaffeineCacheProvider` as default
- [ ] Add `RedisCacheProvider` for distributed caching
- [ ] Create `CacheKeyGenerator` with context awareness
- [ ] Implement cache invalidation strategies
- [ ] Add cache warm-up capabilities

### Phase 4: Enhanced REST API (Priority: Medium)
- [ ] Add comprehensive metadata in responses
- [x] JSON response format only
- [ ] Add GraphQL-like query syntax support
- [ ] Create OpenAPI/Swagger documentation generator
- [ ] Add pagination links (HATEOAS)
- [ ] Implement field projection/selection

### Phase 5: Advanced Query Features (Priority: Low)
- [ ] Add support for stored procedures
- [ ] Implement query composition (combining queries)
- [ ] Add support for multiple result sets
- [ ] Create query templates with placeholders
- [ ] Implement query versioning
- [ ] Add query migration tools

### Phase 6: Monitoring & Observability (Priority: Low)
- [ ] Integrate with Micrometer metrics
- [ ] Add distributed tracing support
- [ ] Create query performance analyzer
- [ ] Implement slow query logging
- [ ] Add query usage statistics
- [ ] Create health check endpoints

### Phase 7: Developer Experience (Priority: Low)
- [ ] Create IntelliJ IDEA plugin for query completion
- [ ] Add Spring Boot DevTools integration
- [ ] Create query testing framework
- [ ] Add mock query executor for testing
- [ ] Generate TypeScript types from queries
- [ ] Create query documentation generator

This specification provides a complete blueprint for the Query Registration System library, documenting both the current implementation and the roadmap for future enhancements.
