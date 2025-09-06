# Query and Select Module Unification Plan

## Executive Summary

The Query and Select modules have ~85% code duplication with only minor differences in their specific use cases. This plan outlines a phased approach to extract shared utilities and interfaces.

## Current State Analysis

### Module Comparison

| Component | Query Module | Select Module | Similarity |
|-----------|--------------|---------------|------------|
| Context | QueryContext (300+ lines) | SelectContext (150+ lines) | 95% |
| Definition | QueryDefinition | SelectDefinition | 85% |
| Builder | QueryDefinitionBuilder | SelectDefinitionBuilder | 90% |
| Executor | QueryExecutorImpl | SelectExecutorImpl | 85% |
| SQL Builder | SqlBuilder | SelectSqlBuilder | 80% |
| Registry | QueryRegistry | SelectRegistry | 100% |
| Result | QueryResult | SelectResponse | 60% |

### Key Differences

1. **Purpose**: Query is for complex data retrieval; Select is for dropdown/LOV components
2. **Attributes**: Query uses dynamic attributes map; Select uses fixed value/label/additions
3. **Filtering**: Query supports complex filters; Select supports simple ID/search
4. **Results**: Query returns rows; Select returns structured items

## Proposed Architecture

```
com.balsam.oasis.common.registry/
├── core/
│   ├── base/                    # NEW: Shared base classes
│   │   ├── BaseContext.java
│   │   ├── BaseDefinition.java
│   │   ├── BaseBuilder.java
│   │   ├── BaseExecutor.java
│   │   ├── BaseSqlBuilder.java
│   │   └── BaseRegistry.java
│   ├── definition/              # Existing shared definitions
│   ├── execution/               # Existing shared execution
│   └── result/                  # Existing shared results
├── query/                       # Query-specific implementation
│   └── (extends base classes)
├── select/                      # Select-specific implementation
│   └── (extends base classes)
└── shared/                      # NEW: Shared utilities
    ├── SqlUtils.java
    ├── PaginationUtils.java
    ├── CriteriaUtils.java
    └── ValidationUtils.java
```

## Implementation Phases

### Phase 1: Create Base Infrastructure (Week 1)

#### 1.1 Create BaseContext
```java
@Data
@SuperBuilder
public abstract class BaseContext<D extends BaseDefinition> {
    protected D definition;
    protected Map<String, Object> params = new HashMap<>();
    protected Pagination pagination;
    protected Map<String, Object> metadata = new HashMap<>();
    protected List<AppliedCriteria> appliedCriteria = new ArrayList<>();
    protected Long startTime;
    protected Long endTime;
    protected boolean includeMetadata = true;
    protected boolean auditEnabled = true;
    protected boolean cacheEnabled = true;
    protected String cacheKey;
    protected Integer totalCount;
    
    // Common methods
    public void startExecution() {
        this.startTime = System.currentTimeMillis();
    }
    
    public void endExecution() {
        this.endTime = System.currentTimeMillis();
    }
    
    public long getExecutionTime() {
        return (startTime != null && endTime != null) ? endTime - startTime : 0;
    }
    
    public void setParam(String name, Object value) {
        params.put(name, value);
    }
    
    public boolean hasParam(String name) {
        return params.containsKey(name) && params.get(name) != null;
    }
    
    @Data
    @Builder
    public static class Pagination {
        private int start;
        private int end;
        private int total;
        private boolean hasNext;
        private boolean hasPrevious;
        
        public int getPageSize() {
            return end - start;
        }
    }
    
    @Data
    @Builder
    public static class AppliedCriteria {
        private String name;
        private String sql;
        private Map<String, Object> params;
        private String reason;
    }
}
```

#### 1.2 Create BaseDefinition
```java
@Data
@SuperBuilder
public abstract class BaseDefinition {
    protected String name;
    protected String description;
    protected String sql;
    protected Map<String, ParamDef<?>> params;
    protected Map<String, CriteriaDef> criteria;
    protected List<Function<Object, Object>> preProcessors;
    protected List<Function<Object, Object>> rowProcessors;
    protected List<Function<Object, Object>> postProcessors;
    protected List<ValidationRule> validationRules;
    protected CacheConfig cacheConfig;
    protected int defaultPageSize = 100;
    protected int maxPageSize = 1000;
    protected boolean auditEnabled = true;
    protected boolean metricsEnabled = true;
    protected Integer queryTimeout;
    
    public boolean hasParams() {
        return params != null && !params.isEmpty();
    }
    
    public boolean hasCriteria() {
        return criteria != null && !criteria.isEmpty();
    }
}
```

#### 1.3 Create BaseExecutor
```java
public abstract class BaseExecutor<D extends BaseDefinition, 
                                   C extends BaseContext<D>, 
                                   E extends BaseExecution<C, R>, 
                                   R> {
    
    protected final NamedParameterJdbcTemplate jdbcTemplate;
    protected final BaseRegistry<D> registry;
    protected final BaseSqlBuilder<D, C> sqlBuilder;
    
    protected BaseExecutor(NamedParameterJdbcTemplate jdbcTemplate,
                          BaseRegistry<D> registry,
                          BaseSqlBuilder<D, C> sqlBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.registry = registry;
        this.sqlBuilder = sqlBuilder;
    }
    
    public E execute(String name) {
        D definition = registry.get(name);
        if (definition == null) {
            throw new QueryExecutionException("Definition not found: " + name);
        }
        return createExecution(definition);
    }
    
    protected abstract E createExecution(D definition);
    
    @Transactional(readOnly = true)
    protected R doExecute(C context) {
        context.startExecution();
        try {
            // Apply defaults
            applyDefaultParams(context);
            
            // Build SQL
            SqlResult sqlResult = sqlBuilder.build(context);
            
            // Execute count if needed
            if (context.hasPagination() && context.isIncludeMetadata()) {
                int totalCount = executeCountQuery(context);
                context.setTotalCount(totalCount);
                updatePaginationMetadata(context);
            }
            
            // Execute main query
            List<?> items = executeMainQuery(sqlResult, context);
            
            // Build response
            R response = buildResponse(items, context);
            
            context.endExecution();
            
            if (context.isAuditEnabled()) {
                audit(context, items.size());
            }
            
            return response;
            
        } finally {
            context.endExecution();
        }
    }
    
    protected abstract R buildResponse(List<?> items, C context);
    protected abstract List<?> executeMainQuery(SqlResult sql, C context);
    protected abstract int executeCountQuery(C context);
    protected abstract void applyDefaultParams(C context);
    protected abstract void updatePaginationMetadata(C context);
    protected abstract void audit(C context, int resultCount);
}
```

### Phase 2: Refactor Query Module (Week 2)

1. **QueryContext extends BaseContext**
   - Remove duplicated fields and methods
   - Keep query-specific: filters, sorts, attributes

2. **QueryDefinition extends BaseDefinition**
   - Remove duplicated fields
   - Keep query-specific: attributes map with filtering/sorting

3. **QueryExecutorImpl extends BaseExecutor**
   - Remove duplicated execution logic
   - Keep query-specific: filter/sort handling

### Phase 3: Refactor Select Module (Week 2)

1. **SelectContext extends BaseContext**
   - Remove duplicated fields and methods
   - Keep select-specific: ids, searchTerm

2. **SelectDefinition extends BaseDefinition**
   - Remove duplicated fields
   - Keep select-specific: valueAttribute, labelAttribute, additionAttributes

3. **SelectExecutorImpl extends BaseExecutor**
   - Remove duplicated execution logic
   - Keep select-specific: ID filtering, search wrapping

### Phase 4: Extract Shared Utilities (Week 3)

#### 4.1 SqlUtils
```java
public class SqlUtils {
    
    public static String replacePlaceholder(String sql, String placeholder, String replacement) {
        return sql.replace("--" + placeholder, replacement != null ? replacement : "");
    }
    
    public static Map<String, Object> extractBindParams(String sql, Map<String, Object> allParams) {
        Map<String, Object> bindParams = new HashMap<>();
        Pattern pattern = Pattern.compile(":(\\w+)");
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (allParams.containsKey(paramName)) {
                bindParams.put(paramName, allParams.get(paramName));
            }
        }
        return bindParams;
    }
    
    public static String wrapForCount(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") count_query";
    }
}
```

#### 4.2 PaginationUtils
```java
public class PaginationUtils {
    
    public static String applyPagination(String sql, Pagination pagination, 
                                         DatabaseDialect dialect, 
                                         Map<String, Object> params) {
        if (dialect.getName().startsWith("ORACLE")) {
            if (dialect.getName().equals("ORACLE_12C")) {
                sql = sql + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
                params.put("offset", pagination.getStart());
                params.put("limit", pagination.getPageSize());
            } else {
                sql = wrapWithRownum(sql, pagination, params);
            }
        } else {
            sql = sql + " LIMIT :limit OFFSET :offset";
            params.put("limit", pagination.getPageSize());
            params.put("offset", pagination.getStart());
        }
        return sql;
    }
    
    private static String wrapWithRownum(String sql, Pagination pagination, 
                                         Map<String, Object> params) {
        sql = "SELECT * FROM (" +
              "  SELECT inner_query.*, ROWNUM rnum FROM (" +
              "    " + sql +
              "  ) inner_query" +
              "  WHERE ROWNUM <= :endRow" +
              ") WHERE rnum > :startRow";
        params.put("startRow", pagination.getStart());
        params.put("endRow", pagination.getEnd());
        return sql;
    }
}
```

#### 4.3 CriteriaUtils
```java
public class CriteriaUtils {
    
    public static String applyCriteria(String sql, 
                                       Map<String, CriteriaDef> criteria,
                                       BaseContext<?> context,
                                       Map<String, Object> params,
                                       boolean trackApplied) {
        if (criteria == null || criteria.isEmpty()) {
            return sql;
        }
        
        for (CriteriaDef criteriaDef : criteria.values()) {
            String placeholder = "--" + criteriaDef.getName();
            if (sql.contains(placeholder)) {
                boolean shouldApply = shouldApplyCriteria(criteriaDef, context, params);
                
                if (shouldApply) {
                    sql = sql.replace(placeholder, criteriaDef.getSql());
                    
                    if (trackApplied) {
                        context.getAppliedCriteria().add(
                            BaseContext.AppliedCriteria.builder()
                                .name(criteriaDef.getName())
                                .sql(criteriaDef.getSql())
                                .params(SqlUtils.extractBindParams(criteriaDef.getSql(), params))
                                .reason("Criteria condition met")
                                .build()
                        );
                    }
                } else {
                    sql = sql.replace(placeholder, "");
                }
            }
        }
        
        return sql;
    }
    
    private static boolean shouldApplyCriteria(CriteriaDef criteria, 
                                               BaseContext<?> context,
                                               Map<String, Object> params) {
        // Check condition
        if (criteria.getCondition() != null) {
            // Note: This needs adaptation for the context type
            return criteria.getCondition().test(context);
        }
        
        // Check if all bind parameters are present
        String criteriaSql = criteria.getSql();
        if (criteriaSql != null) {
            Pattern pattern = Pattern.compile(":(\\w+)");
            Matcher matcher = pattern.matcher(criteriaSql);
            while (matcher.find()) {
                String paramName = matcher.group(1);
                if (!params.containsKey(paramName)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
```


## Benefits

### Immediate Benefits
- **Code Reduction**: ~40% reduction in total code
- **Maintenance**: Single place to fix bugs and add features
- **Consistency**: Uniform behavior across modules
- **Testing**: Shared test utilities and patterns

### Long-term Benefits
- **Extensibility**: Easy to add new query types (e.g., TreeQuery, GraphQuery)
- **Performance**: Centralized optimization opportunities
- **Features**: New features automatically available to both modules
- **Quality**: Better test coverage through shared test infrastructure

## Risk Mitigation

### Backward Compatibility
- Keep all existing public APIs unchanged
- Use adapter pattern where necessary
- Deprecate rather than remove old methods

### Testing Strategy
- Create parallel test suite during refactoring
- Run both old and new implementations in parallel
- Compare results for validation

### Rollback Plan
- Phase-based implementation allows partial rollback
- Keep original classes until fully validated
- Feature flag to switch between implementations

## Success Metrics

1. **Code Metrics**
   - Lines of code reduced by 40%
   - Test coverage maintained at >80%
   - Zero breaking changes

2. **Performance Metrics**
   - Query execution time ±5% of current
   - Memory usage ±10% of current
   - No increase in database connections

3. **Quality Metrics**
   - All existing tests pass
   - No new bugs in production
   - Improved maintainability score

## Timeline

| Week | Phase | Deliverables |
|------|-------|--------------|
| 1 | Base Infrastructure | Base classes created and tested |
| 2 | Module Refactoring | Query and Select modules using base classes |
| 3 | Shared Utilities | Common utilities extracted and integrated |
| 4 | Testing & Migration | Full test coverage, documentation, migration guide |

## Next Steps

1. Review and approve this plan
2. Create feature branch for refactoring
3. Begin Phase 1 implementation
4. Set up parallel testing infrastructure
5. Schedule code reviews for each phase

## Appendix: Detailed Class Mappings

### Current Duplication Analysis

| Functionality | Query Lines | Select Lines | Shared Potential |
|--------------|-------------|--------------|------------------|
| Context Management | 350 | 150 | 300 |
| SQL Building | 400 | 280 | 350 |
| Execution Flow | 500 | 400 | 450 |
| Registry | 100 | 100 | 100 |
| Builders | 600 | 450 | 500 |
| **Total** | **1950** | **1380** | **1700** |

### Estimated Final Structure

| Component | Lines | Reduction |
|-----------|-------|-----------|
| Base Classes | 800 | - |
| Query-Specific | 600 | 70% |
| Select-Specific | 400 | 70% |
| Shared Utilities | 300 | - |
| **Total** | **2100** | **36%** |

This refactoring will reduce total code by approximately 1230 lines (36%) while improving maintainability and consistency.