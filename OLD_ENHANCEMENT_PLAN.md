# Query Registration System - Architecture Enhancement Plan

## Executive Summary

This document outlines a comprehensive refactoring plan to transform the Query Registration System into a well-architected library following SOLID principles, with improved package organization and removal of technical debt.

## Current State Analysis

### Major Issues Identified

1. **Single Responsibility Violations**: Classes handling multiple concerns (QueryExecutorImpl manages 7+ responsibilities)
2. **Open/Closed Violations**: Hard-coded dialects, filter operations, and type conversions
3. **Interface Segregation Issues**: Fat interfaces and data classes with too many concerns
4. **Dependency Inversion Problems**: Direct instantiation of concrete classes, static global state
5. **Package Organization**: Mixed abstraction layers, no clear separation between domain and infrastructure
6. **Backwards Compatibility Code**: Deprecated patterns in AttributeDef and ParamDef
7. **Static Global State**: QueryDefinitionValidator uses static registry

## Phase 1: Core Architecture Refactoring (Week 1-2)

### 1.1 Extract Database Dialect Strategy Pattern

**Current Problem**: SqlBuilder has hard-coded dialect logic with massive Oracle 11g specific code (300+ lines)

**Solution**:
```
com.balsam.oasis.common.registry.dialect/
├── DatabaseDialect.java (interface)
├── DialectFactory.java
├── impl/
│   ├── StandardSqlDialect.java
│   ├── Oracle11gDialect.java
│   ├── Oracle12cDialect.java
│   └── PostgreSqlDialect.java
└── sql/
    ├── PaginationStrategy.java
    ├── FilterBuilder.java
    └── SortBuilder.java
```

**Tasks**:
- [ ] Create DatabaseDialect interface with methods: buildPagination, buildFilter, buildSort, wrapQuery
- [ ] Extract Oracle 11g logic to Oracle11gDialect
- [ ] Extract Oracle 12c+ logic to Oracle12cDialect
- [ ] Create DialectFactory for dialect selection
- [ ] Remove dialect conditionals from SqlBuilder

### 1.2 Separate Query Execution Concerns

**Current Problem**: QueryExecutorImpl handles too many responsibilities

**Solution**: Command Pattern with specialized executors
```
com.balsam.oasis.common.registry.execution/
├── QueryCommand.java (interface)
├── QueryCommandExecutor.java
├── commands/
│   ├── ExecuteQueryCommand.java
│   ├── CountQueryCommand.java
│   └── MetadataQueryCommand.java
├── pipeline/
│   ├── QueryPipeline.java
│   ├── PreProcessingStage.java
│   ├── ExecutionStage.java
│   ├── RowMappingStage.java
│   └── PostProcessingStage.java
└── mapping/
    ├── RowMapperStrategy.java (interface)
    ├── DynamicRowMapperStrategy.java
    └── OptimizedRowMapperStrategy.java
```

**Tasks**:
- [ ] Create QueryCommand interface
- [ ] Extract execution logic to ExecuteQueryCommand
- [ ] Create QueryPipeline for processing stages
- [ ] Extract row mapping to strategy pattern
- [ ] Inject strategies via dependency injection

### 1.3 Remove Static Global State

**Current Problem**: QueryDefinitionValidator uses static registry

**Solution**: Inject validators as services
```
com.balsam.oasis.common.registry.validation/
├── QueryValidator.java (interface)
├── ValidationContext.java
├── impl/
│   ├── DuplicateValidator.java
│   ├── BindParameterValidator.java
│   ├── AttributeValidator.java
│   └── CompositeValidator.java
└── ValidationResult.java
```

**Tasks**:
- [ ] Remove static GLOBAL_QUERY_REGISTRY
- [ ] Create QueryValidator interface
- [ ] Inject QueryRegistry into validators
- [ ] Create ValidationContext for passing state
- [ ] Implement composite validator pattern

## Phase 2: Domain Model Improvements (Week 2-3)

### 2.1 Split QueryDefinition Responsibilities

**Current Problem**: QueryDefinition is a fat data class with too many concerns

**Solution**: Aggregate pattern with focused domain objects
```
com.balsam.oasis.common.registry.domain/
├── query/
│   ├── Query.java (aggregate root)
│   ├── QueryId.java (value object)
│   └── QueryMetadata.java
├── schema/
│   ├── QuerySchema.java
│   ├── Attribute.java
│   ├── Parameter.java
│   └── Criteria.java
├── execution/
│   ├── ExecutionPolicy.java
│   ├── CachePolicy.java
└── sql/
    ├── SqlTemplate.java
    └── SqlPlaceholder.java
```

**Tasks**:
- [ ] Create Query aggregate root
- [ ] Extract schema concerns to QuerySchema
- [ ] Create focused policy objects
- [ ] Remove backwards compatibility code from AttributeDef/ParamDef
- [ ] Implement value objects for strong typing

### 2.2 Improve Type Safety with Generics

**Current Problem**: Loose typing with Function<Object, Object> in processors

**Solution**: Properly typed processor interfaces
```
com.balsam.oasis.common.registry.processing/
├── ProcessorChain.java<T>
├── typed/
│   ├── TypedPreProcessor.java<T>
│   ├── TypedRowProcessor.java<T, R>
│   ├── TypedPostProcessor.java<T>
│   └── TypedAttributeProcessor.java<T, R>
└── factory/
    └── ProcessorFactory.java
```

**Tasks**:
- [ ] Create properly typed processor interfaces
- [ ] Implement ProcessorChain with type safety
- [ ] Remove Object type casting
- [ ] Add compile-time type checking

## Phase 3: Infrastructure Layer Separation (Week 3-4)

### 3.1 Reorganize Package Structure

**Current Structure**: Mixed concerns in packages

**New Structure**:
```
com.balsam.oasis.common.registry/
├── api/                     # Public API interfaces
│   ├── QueryExecutor.java
│   ├── QueryRegistry.java
│   └── QueryRegistry.java
├── domain/                  # Core domain logic
│   ├── model/
│   ├── service/
│   └── repository/
├── application/             # Application services
│   ├── command/
│   ├── query/
│   └── dto/
├── infrastructure/          # Technical implementations
│   ├── persistence/
│   ├── cache/
│   ├── dialect/
│   └── mapping/
├── web/                     # Web layer
│   ├── rest/
│   ├── converter/
│   └── exception/
└── config/                  # Spring configuration
```

**Tasks**:
- [ ] Move interfaces to api package
- [ ] Separate domain from infrastructure
- [ ] Extract web concerns from core
- [ ] Create clear module boundaries

### 3.2 Extract Request/Response Handling

**Current Problem**: QueryController mixes HTTP concerns with business logic

**Solution**: Hexagonal architecture with ports and adapters
```
com.balsam.oasis.common.registry.adapter/
├── inbound/
│   ├── rest/
│   │   ├── QueryRestController.java
│   │   ├── RequestMapper.java
│   │   └── ResponseMapper.java
│   └── graphql/
│       └── QueryGraphQLController.java
└── outbound/
    ├── jdbc/
    │   └── JdbcQueryAdapter.java
    └── cache/
        └── CacheAdapter.java
```

**Tasks**:
- [ ] Create inbound ports (interfaces)
- [ ] Implement REST adapter
- [ ] Extract request/response mapping
- [ ] Prepare for GraphQL support

## Phase 4: Advanced Features & Optimizations (Week 4-5)

### 4.1 Implement Filter Operation Strategy

**Current Problem**: Large switch statement for filter operations

**Solution**: Strategy pattern with registrable operations
```
com.balsam.oasis.common.registry.filter/
├── FilterOperation.java (interface)
├── FilterOperationRegistry.java
├── operations/
│   ├── EqualsOperation.java
│   ├── LikeOperation.java
│   ├── InOperation.java
│   ├── BetweenOperation.java
│   └── ComparisonOperations.java
└── builder/
    └── FilterExpressionBuilder.java
```

**Tasks**:
- [ ] Create FilterOperation interface
- [ ] Implement individual operations
- [ ] Create operation registry
- [ ] Remove switch statement
- [ ] Support custom operations

### 4.2 Improve Cache Architecture

**Current Problem**: Cache logic mixed with execution

**Solution**: Decorator pattern for caching
```
com.balsam.oasis.common.registry.cache/
├── QueryCache.java (interface)
├── CacheKey.java
├── CachePolicy.java
├── decorator/
│   ├── CachingQueryExecutor.java
│   └── CachingQueryRegistry.java
└── provider/
    ├── CaffeineCache.java
    ├── RedisCache.java
    └── NoOpCache.java
```

**Tasks**:
- [ ] Extract cache to decorator
- [ ] Implement cache providers
- [ ] Create cache key generation
- [ ] Add cache metrics

### 4.3 Add Extensibility Points

**Solution**: Plugin architecture for extensions
```
com.balsam.oasis.common.registry.extension/
├── QueryExtension.java (interface)
├── ExtensionPoint.java
├── ExtensionRegistry.java
└── hooks/
    ├── PreExecutionHook.java
    ├── PostExecutionHook.java
    └── ErrorHook.java
```

**Tasks**:
- [ ] Define extension points
- [ ] Create plugin system
- [ ] Add lifecycle hooks
- [ ] Document extension API

## Phase 5: Testing & Documentation (Week 5-6)

### 5.1 Improve Test Architecture

**Tasks**:
- [ ] Create test fixtures factory
- [ ] Remove static test dependencies
- [ ] Add integration test suite
- [ ] Implement contract testing
- [ ] Add performance benchmarks

### 5.2 Documentation & Migration

**Tasks**:
- [ ] Create migration guide from current version
- [ ] Document new architecture
- [ ] Add code examples
- [ ] Create extension developer guide

## Implementation Priority Matrix

| Phase | Priority | Risk | Impact | Effort |
|-------|----------|------|--------|---------|
| 1.1 Dialect Strategy | High | Medium | High | Medium |
| 1.2 Execution Separation | High | High | High | High |
| 1.3 Remove Static State | High | Low | Medium | Low |
| 2.1 Domain Model | Medium | Medium | High | Medium |
| 2.2 Type Safety | Medium | Low | Medium | Low |
| 3.1 Package Reorganization | High | Low | High | Medium |
| 3.2 Request Handling | Low | Low | Medium | Medium |
| 4.1 Filter Strategy | Medium | Low | Medium | Low |
| 4.2 Cache Architecture | Low | Low | Low | Medium |
| 4.3 Extensibility | Low | Low | High | Medium |

## Breaking Changes to Remove

1. **Static Registry in QueryDefinitionValidator** - Replace with injected service
2. **Backwards compatibility in AttributeDef.name()** - Remove static factory method
3. **Backwards compatibility in ParamDef.name()** - Remove static factory method
4. **Direct QueryExecutor registration methods** - Already removed, ensure complete cleanup
5. **Function<Object, Object> processors** - Replace with typed interfaces

## Success Metrics

- **Code Quality**:
  - Cyclomatic complexity < 10 per method
  - Class cohesion > 0.8
  - Package coupling < 0.3
  
- **SOLID Compliance**:
  - Each class has single responsibility
  - New features require no core modifications
  - All dependencies inject via interfaces
  
- **Performance**:
  - Query execution < 100ms for standard queries
  - Metadata cache hit rate > 90%
  - Zero memory leaks

## Migration Strategy

### Step 1: Parallel Development
- Develop new architecture alongside current
- Mark current implementation as @Deprecated
- Provide adapters for backward compatibility

### Step 2: Gradual Migration
- Migrate one module at a time
- Run both implementations in parallel
- Validate results match

### Step 3: Cutover
- Switch to new implementation
- Keep deprecated code for 1 version
- Remove in next major version

## Timeline

- **Week 1-2**: Core architecture refactoring
- **Week 2-3**: Domain model improvements  
- **Week 3-4**: Infrastructure separation
- **Week 4-5**: Advanced features
- **Week 5-6**: Testing and documentation
- **Week 7**: Migration and release

## Risk Mitigation

| Risk | Mitigation Strategy |
|------|-------------------|
| Breaking existing code | Provide compatibility layer |
| Performance regression | Benchmark before/after |
| Complex migration | Incremental approach |
| Missing features | Feature flag system |

## Conclusion

This enhancement plan transforms the Query Registration System from a monolithic, tightly-coupled implementation to a modular, extensible architecture following SOLID principles. The phased approach minimizes risk while delivering immediate value through improved maintainability and testability.

The key improvements focus on:
1. **Separation of Concerns**: Each class has one clear responsibility
2. **Extensibility**: New features without modifying core
3. **Testability**: Mockable dependencies and clear boundaries
4. **Performance**: Optimized caching and execution strategies
5. **Maintainability**: Clear package structure and documentation

By following this plan, the library will be ready for long-term evolution and enterprise adoption.