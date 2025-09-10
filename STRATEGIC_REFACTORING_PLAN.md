# Strategic Code Reduction Architecture Plan

## Executive Summary
This document outlines strategic architectural changes for the Query Registration System that can reduce code by **35-40%** (~3,500-4,000 lines) while improving maintainability, WITHOUT changing the current builder pattern.

## Current State Analysis
- **Total Lines**: 9,798 lines of Java code
- **Largest Files**: 
  - BaseRowMapper: 494 lines
  - SelectDefinitionBuilder: 478 lines (duplicate system)
  - AttributeDef: 465 lines
  - QueryDefinitionBuilder: 353 lines
- **Pain Points**: Code duplication, verbose immutable object creation, complex row mapping

## Strategic Refactoring Solutions

### 1. **Record-Based Immutable Models** _(Save ~1,000 lines)_
Replace Lombok @Value classes with Java Records for cleaner immutable objects:

**Current (Lombok @Value):**
```java
@Value
@Builder
public class QueryDefinition {
    String name;
    String sql;
    Map<String, AttributeDef<?>> attributes;
    // ... 30+ fields with getters
}
```

**Proposed (Java Records):**
```java
public record QueryDefinition(
    String name, 
    String sql, 
    Map<String, AttributeDef<?>> attributes,
    Map<String, ParamDef> params,
    // ... other fields
) {
    // Compact canonical constructor for validation
    public QueryDefinition {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(sql, "sql cannot be null");
        attributes = Map.copyOf(attributes);
        params = Map.copyOf(params);
    }
}
```

**Impact**: 
- QueryDefinition: 221 → 50 lines
- AttributeDef: 465 → 100 lines  
- ParamDef: ~200 → 40 lines
- CriteriaDef: ~150 → 30 lines
- All domain objects become more concise

### 2. **Unified Generic Processor Chain** _(Save ~800 lines)_
Consolidate 5 separate processor interfaces into a single generic pipeline:

**Current (Multiple Interfaces):**
```java
public interface PreProcessor {
    void process(QueryContext context);
}
public interface RowProcessor {
    Row process(Row row, QueryContext context);
}
public interface PostProcessor {
    QueryResult process(QueryResult result, QueryContext context);
}
// Plus AttributeProcessor, ParamProcessor
```

**Proposed (Unified Pipeline):**
```java
public interface Processor<T, R> {
    R process(T input, ProcessorContext ctx);
    
    default Processor<T, R> andThen(Processor<R, ?> after) {
        return (input, ctx) -> after.process(process(input, ctx), ctx);
    }
}

// Type-safe processor types
public class Processors {
    public static Processor<QueryContext, QueryContext> pre(Consumer<QueryContext> fn) {
        return (ctx, _) -> { fn.accept(ctx); return ctx; };
    }
    
    public static Processor<Row, Row> row(Function<Row, Row> fn) {
        return (row, _) -> fn.apply(row);
    }
}
```

**Impact**: Reduce processor boilerplate by 80%, enable functional composition

### 3. **Dynamic Proxy-Based Row Mapping** _(Save ~600 lines)_
Replace manual row mapping with dynamic proxies:

**Current (Manual Mapping):**
```java
public class BaseRowMapper<T> implements RowMapper<T> {
    // 494 lines of manual mapping logic
    public T mapRow(ResultSet rs, int rowNum, QueryContext context) {
        // Complex extraction logic
        // Type conversion
        // Attribute processing
        // Virtual attribute calculation
    }
}
```

**Proposed (Dynamic Proxy):**
```java
public interface RowProxy {
    static <T> T create(Class<T> type, ResultSet rs, QueryDefinition def) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class[]{type},
            new RowInvocationHandler(rs, def)
        );
    }
}

// Define row interfaces
public interface EmployeeRow extends Row {
    String getName();
    BigDecimal getSalary();
    
    @Virtual("salary * 0.1")
    BigDecimal getBonus();
}
```

**Impact**: Auto-generate mapping logic at runtime, eliminate BaseRowMapper

### 4. **Consolidate Duplicate Select System** _(Save ~750 lines)_
Merge the parallel Select system into the main Query system:

**Current**: Two parallel implementations
- Main Query System: `com.balsam.oasis.common.registry.*`
- Select System: `com.balsam.oasis.common.registry.select.*`

**Proposed**: Single unified system with a "mode" flag
```java
public enum QueryMode {
    FULL,    // Full query with all features
    SIMPLE   // Simplified select mode
}

QueryDefinition.builder()
    .mode(QueryMode.SIMPLE)  // Opt into simplified behavior
    .build();
```

**Impact**: Remove SelectDefinitionBuilder (478 lines) and related classes

### 5. **Spring-Native Features Adoption** _(Save ~500 lines)_

#### Replace Custom Type Conversion
**Current**: Custom TypeConverter (241 lines)
**Proposed**: Use Spring's `ConversionService`
```java
@Bean
public ConversionService conversionService() {
    DefaultConversionService service = new DefaultConversionService();
    service.addConverter(new StringToBigDecimalConverter());
    return service;
}
```

#### Replace Custom Validation
**Current**: Custom ValidationRule and validators
**Proposed**: Use Bean Validation API
```java
public class QueryParams {
    @NotNull @Min(0)
    private BigDecimal minSalary;
    
    @Pattern(regexp = "[A-Z]+")
    private String status;
}
```

#### Replace Custom Caching
**Current**: Custom MetadataCache implementation
**Proposed**: Use Spring Cache abstraction
```java
@Cacheable(value = "metadata", key = "#definition.name")
public MetadataCache buildCache(QueryDefinition definition) {
    // Build cache
}
```

### 6. **Simplify QueryDefinition Creation** _(Save ~200 lines)_
Keep builders but reduce repetitive code:

**Current**: Verbose with/copy methods in QueryDefinition
```java
public QueryDefinition withMetadataCache(MetadataCache cache) {
    return QueryDefinition.builder()
        .name(this.name)
        .description(this.description)
        .sql(this.sql)
        // ... copy 20+ fields
        .metadataCache(cache)
        .build();
}
```

**Proposed**: Use builder copy constructor
```java
public QueryDefinition withMetadataCache(MetadataCache cache) {
    return this.toBuilder()
        .metadataCache(cache)
        .build();
}
```

### 7. **Functional SQL Building** _(Save ~150 lines)_
Simplify SQL construction with functional approach:

**Current**: Imperative SQL building
**Proposed**: Functional SQL builder
```java
public class SqlTemplate {
    public static String build(String base, Consumer<SqlBuilder> customizer) {
        SqlBuilder builder = new SqlBuilder(base);
        customizer.accept(builder);
        return builder.build();
    }
}

// Usage
String sql = SqlTemplate.build("SELECT * FROM emp", sql -> {
    sql.where("salary > :min");
    sql.orderBy("name");
});
```

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
- Convert domain objects to Records
- Implement unified Processor interface
- **Estimated Reduction**: 1,800 lines

### Phase 2: Core Refactoring (Week 2)
- Implement dynamic proxy row mapping
- Consolidate Select system into main system
- **Estimated Reduction**: 1,350 lines

### Phase 3: Spring Integration (Week 3)
- Replace custom type conversion with Spring's
- Adopt Spring validation
- Use Spring Cache abstraction
- **Estimated Reduction**: 500 lines

### Phase 4: Optimization (Week 4)
- Simplify QueryDefinition builders
- Implement functional SQL building
- Code cleanup and optimization
- **Estimated Reduction**: 350 lines

## Expected Outcomes

### Metrics
- **Total Code Reduction**: ~4,000 lines (40% reduction)
- **New Total**: ~5,800 lines
- **Complexity Reduction**: 35% fewer classes
- **Test Coverage**: Maintained at current levels

### Benefits
1. **Cleaner Architecture**: Modern Java features (Records, Proxies)
2. **Better Maintainability**: Less boilerplate, more focused code
3. **Improved Performance**: Dynamic proxies can be cached
4. **Spring Integration**: Leverage battle-tested Spring components
5. **Backward Compatibility**: Builder pattern preserved

### Risks & Mitigations
1. **Risk**: Records require Java 14+
   - **Mitigation**: Project already uses Java 21
   
2. **Risk**: Dynamic proxies add runtime complexity
   - **Mitigation**: Comprehensive testing, proxy caching
   
3. **Risk**: Spring dependencies increase
   - **Mitigation**: Already using Spring Boot, minimal new deps

## Conclusion
This strategic refactoring plan maintains the builder pattern while achieving significant code reduction through:
- Modern Java features (Records, Proxies)
- Spring framework capabilities
- Functional programming patterns
- Architectural consolidation

The changes are incremental and can be implemented in phases, ensuring system stability throughout the refactoring process.