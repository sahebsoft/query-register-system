# Query Registration System - Refactoring TODO List

## Overview
This document tracks the implementation progress of the strategic refactoring plan to reduce codebase by ~4,000 lines (40%).

---

## Phase 1: Foundation (Week 1)
**Goal**: Convert domain objects to Records and implement unified Processor interface  
**Expected Reduction**: 1,800 lines

### Domain Objects to Records
- [ ] Convert `QueryDefinition` to Record (221 → 50 lines)
  - [ ] Create new Record-based QueryDefinition
  - [ ] Update all references
  - [ ] Test compatibility
  - [ ] Remove old Lombok version
  
- [x] Convert `AttributeDef` to Record (465 → 314 lines) ✅ **COMPLETED**
  - [x] Create AttributeDefRecord with builder
  - [x] Migrate type-safe builder pattern
  - [ ] Update all usages
  - [ ] Remove old implementation
  
- [x] Convert `ParamDef` to Record (194 → 160 lines) ✅ **COMPLETED**
  - [x] Create ParamDefRecord
  - [x] Maintain generic type safety
  - [ ] Update references
  - [ ] Remove old version
  
- [x] Convert `CriteriaDef` to Record (94 → 91 lines) ✅ **COMPLETED**
  - [x] Create CriteriaDefRecord
  - [x] Preserve condition predicates
  - [ ] Update usages
  - [ ] Remove old implementation

### Unified Processor Chain
- [x] Design unified `Processor<T,R>` interface ✅ **COMPLETED**
- [ ] Create `ProcessorContext` wrapper
- [x] Implement `Processors` utility class ✅ **COMPLETED**
- [ ] Migrate `PreProcessor` implementations
- [ ] Migrate `RowProcessor` implementations  
- [ ] Migrate `PostProcessor` implementations
- [ ] Migrate `AttributeProcessor` implementations
- [ ] Migrate `ParamProcessor` implementations
- [ ] Remove old processor interfaces
- [ ] Update `QueryExecutorImpl` to use new processors

---

## Phase 2: Core Refactoring (Week 2)
**Goal**: Implement dynamic proxy row mapping and consolidate Select system  
**Expected Reduction**: 1,350 lines

### Dynamic Proxy Row Mapping
- [ ] Design `RowProxy` interface
- [ ] Implement `RowInvocationHandler`
- [ ] Create `@Virtual` annotation for calculated fields
- [ ] Create `@Column` annotation for mapping
- [ ] Build proxy cache mechanism
- [ ] Create row interface examples
- [ ] Migrate existing row mappers
- [ ] Remove `BaseRowMapper` (494 lines)
- [ ] Update `QueryExecutorImpl` row mapping

### Consolidate Select System
- [ ] Add `QueryMode` enum (FULL, SIMPLE)
- [ ] Update `QueryDefinition` to support mode
- [ ] Migrate Select-specific features to main system
- [ ] Update example configurations
- [ ] Remove `SelectDefinitionBuilder` (478 lines)
- [ ] Remove other Select-specific classes
- [ ] Update tests for consolidated system

---

## Phase 3: Spring Integration (Week 3)
**Goal**: Replace custom implementations with Spring features  
**Expected Reduction**: 500 lines

### Spring ConversionService
- [ ] Configure Spring `ConversionService` bean
- [ ] Create custom converters for special types
- [ ] Replace `TypeConverter` usages (241 lines)
- [ ] Remove `TypeConverter` class
- [ ] Update row mapping to use ConversionService

### Spring Validation
- [ ] Add Bean Validation annotations to models
- [ ] Configure `Validator` bean
- [ ] Replace custom `ValidationRule` implementations
- [ ] Update `QueryDefinitionValidator`
- [ ] Remove redundant validation code

### Spring Cache
- [ ] Configure Spring Cache abstraction
- [ ] Add `@Cacheable` annotations
- [ ] Replace `MetadataCache` implementation
- [ ] Configure cache eviction policies
- [ ] Remove custom cache code

---

## Phase 4: Optimization (Week 4)
**Goal**: Final optimizations and cleanup  
**Expected Reduction**: 350 lines

### Simplify QueryDefinition Builders
- [ ] Add `toBuilder()` method to Records
- [ ] Simplify `withMetadataCache()` method
- [ ] Simplify `withAttributes()` method
- [ ] Remove repetitive copying code
- [ ] Optimize builder validation

### Functional SQL Building
- [ ] Create `SqlTemplate` class
- [ ] Implement functional SQL builder
- [ ] Add fluent API for SQL construction
- [ ] Migrate existing SQL building logic
- [ ] Remove verbose SQL construction code

### Code Cleanup
- [ ] Remove unused imports
- [ ] Delete commented code
- [ ] Consolidate duplicate utilities
- [ ] Optimize error handling
- [ ] Update documentation

---

## Testing & Validation

### Unit Tests
- [ ] Update tests for Record-based models
- [ ] Test unified processor chain
- [ ] Test dynamic proxy mapping
- [ ] Test Spring integration components
- [ ] Ensure 100% backward compatibility

### Integration Tests
- [ ] Test full query execution flow
- [ ] Test REST API endpoints
- [ ] Test with Oracle database
- [ ] Performance benchmarks
- [ ] Load testing

### Documentation
- [ ] Update CLAUDE.md with new patterns
- [ ] Document Record usage
- [ ] Document proxy mapping
- [ ] Update example configurations
- [ ] Create migration guide

---

## Metrics Tracking

### Current State
- **Total Lines**: 9,798
- **Number of Classes**: ~60
- **Test Coverage**: [Current %]

### Target State
- **Total Lines**: ~5,800 (-40%)
- **Number of Classes**: ~40 (-35%)
- **Test Coverage**: [Maintain/Improve]

### Progress Tracking
| Phase | Status | Lines Reduced | Target | Actual |
|-------|--------|---------------|--------|--------|
| Phase 1 | In Progress | 188 | 1,800 | 188 |
| Phase 2 | Not Started | 0 | 1,350 | - |
| Phase 3 | Not Started | 0 | 500 | - |
| Phase 4 | Not Started | 0 | 350 | - |
| **Total** | **4.7%** | **188** | **4,000** | **188** |

### Lines Saved So Far:
- AttributeDef: 465 → 314 = **151 lines saved**
- ParamDef: 194 → 160 = **34 lines saved**  
- CriteriaDef: 94 → 91 = **3 lines saved**
- **Total: 188 lines saved**

---

## Risk Log

| Risk | Impact | Mitigation | Status |
|------|--------|------------|--------|
| Record compatibility | Medium | Java 21 already in use | ✓ |
| Dynamic proxy performance | Low | Implement caching | Pending |
| Breaking changes | High | Comprehensive testing | Pending |
| Spring dependency conflicts | Low | Version management | Pending |

---

## Notes & Decisions

### Decisions Made
- Keep builder pattern (no annotation-driven approach)
- Use Java Records for immutability
- Leverage Spring Boot existing features

### Open Questions
- [ ] Proxy caching strategy?
- [ ] Migration approach for existing queries?
- [ ] Deprecation timeline for old APIs?

### Blockers
- None currently identified

---

## Review Checkpoints

- [ ] Week 1 Review: Foundation complete
- [ ] Week 2 Review: Core refactoring complete
- [ ] Week 3 Review: Spring integration complete
- [ ] Week 4 Review: Final optimization complete
- [ ] Final Review: All goals achieved

---

*Last Updated: [Date]*  
*Next Review: [Date]*