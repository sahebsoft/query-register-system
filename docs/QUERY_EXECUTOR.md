# Query Executor Documentation

## Overview
The `QueryExecutor` is the runtime engine that executes registered queries with parameters, filters, sorting, and pagination.

## Architecture

```
QueryExecutor (Interface)
    └── QueryExecutorImpl
        ├── QuerySqlBuilder (SQL generation)
        ├── QueryRowMapper (Result mapping)
        ├── MetadataCache (Column metadata)
        └── NamedParameterJdbcTemplate (JDBC execution)
```

## Basic Usage

```java
@Autowired
private QueryExecutor queryExecutor;

// Execute by query name
QueryResult result = queryExecutor.execute("employees")
    .withParam("deptId", 100)
    .withPagination(0, 50)
    .execute();
```

## QueryExecution API

### 1. Parameter Binding

```java
// Single parameter
.withParam("deptId", 100)

// Multiple parameters
.withParams(Map.of(
    "deptId", 100,
    "minSalary", 50000
))
```

### 2. Dynamic Filtering

```java
// Simple filter
.withFilter("salary", FilterOp.GREATER_THAN, 50000)

// IN clause
.withFilter("status", FilterOp.IN, List.of("ACTIVE", "PENDING"))

// BETWEEN
.withFilter("hireDate", FilterOp.BETWEEN, startDate, endDate)

// Multiple filters
.withFilters(Map.of(
    "department", Filter.builder()
        .attribute("departmentId")
        .operator(FilterOp.EQUALS)
        .value(100)
        .build()
))
```

#### Available Filter Operators
- `EQUALS` (=)
- `NOT_EQUALS` (!=)
- `GREATER_THAN` (>)
- `GREATER_THAN_OR_EQUAL` (>=)
- `LESS_THAN` (<)
- `LESS_THAN_OR_EQUAL` (<=)
- `LIKE` (pattern matching)
- `IN` (list of values)
- `BETWEEN` (range)
- `IS_NULL`
- `IS_NOT_NULL`

### 3. Sorting

```java
// Single sort
.withSort("lastName", SortDir.ASC)

// Multiple sorts
.withSort(List.of(
    SortSpec.builder()
        .attribute("department")
        .direction(SortDir.ASC)
        .build(),
    SortSpec.builder()
        .attribute("salary")
        .direction(SortDir.DESC)
        .build()
))
```

### 4. Pagination

```java
// Using start/end indices
.withPagination(0, 50)    // First 50 records

// Using offset/limit
.withOffsetLimit(100, 25) // Skip 100, take 25
```

### 5. Field Selection

```java
// Select specific fields
.select("employeeId", "firstName", "lastName")

// Or using Set
.selectFields(Set.of("employeeId", "email"))
```

### 6. Security Context

```java
.withSecurityContext(userContext)
```

### 7. Metadata Control

```java
.includeMetadata(true)  // Include execution metadata
.withCaching(false)     // Disable caching for this execution
```

## Execution Methods

### Standard Execution
```java
QueryResult result = queryExecutor.execute("employees")
    .withParam("deptId", 100)
    .execute();
```

### Single Row Execution
```java
QueryRow employee = queryExecutor.execute("employees")
    .withParam("employeeId", 12345)
    .executeSingle();  // Returns single row or null
```

### Async Execution
```java
CompletableFuture<QueryResult> future = queryExecutor.execute("employees")
    .withParam("deptId", 100)
    .executeAsync();
```

## QueryResult Structure

```java
public class QueryResult {
    List<QueryRow> rows;        // Query results
    QueryMetadata metadata;     // Execution metadata
    Map<String, Object> summary;// Summary data
    Long executionTimeMs;       // Execution time
    boolean success;            // Success flag
    Integer count;              // Row count
}
```

### Accessing Results

```java
QueryResult result = queryExecutor.execute("employees").execute();

// Get rows
List<QueryRow> rows = result.getRows();

// Get as maps
List<Map<String, Object>> data = result.getData();

// Check if empty
if (result.isEmpty()) {
    // No results
}

// Get first row
QueryRow firstRow = result.getFirstRow();

// Get metadata
QueryMetadata metadata = result.getMetadata();
```

## QueryRow Access

```java
QueryRow row = result.getFirstRow();

// Generic access
Object value = row.get("employeeId");

// Type-safe access
Integer id = row.get("employeeId", Integer.class);
String name = row.getString("firstName");
BigDecimal salary = row.getBigDecimal("salary");
LocalDate hireDate = row.getLocalDate("hireDate");

// With defaults
String dept = row.getString("department", "Unknown");

// Check existence
if (row.hasAttribute("bonus")) {
    BigDecimal bonus = row.getBigDecimal("bonus");
}

// Access raw column data
Object rawValue = row.getRaw("EMPLOYEE_ID");

// Convert to Map
Map<String, Object> rowMap = row.toMap();
```

## Complete Example

```java
@Service
public class EmployeeService {
    @Autowired
    private QueryExecutor queryExecutor;
    
    public List<Map<String, Object>> getActiveEmployees(
            Integer departmentId, 
            BigDecimal minSalary,
            int page, 
            int pageSize) {
        
        try {
            QueryResult result = queryExecutor.execute("employees")
                // Parameters
                .withParam("status", "ACTIVE")
                .withParam("deptId", departmentId)
                
                // Filters
                .withFilter("salary", FilterOp.GREATER_THAN_OR_EQUAL, minSalary)
                
                // Sorting
                .withSort("lastName", SortDir.ASC)
                .withSort("firstName", SortDir.ASC)
                
                // Pagination
                .withOffsetLimit(page * pageSize, pageSize)
                
                // Field selection
                .select("employeeId", "firstName", "lastName", 
                       "email", "salary", "departmentName")
                
                // Metadata
                .includeMetadata(true)
                
                // Execute
                .execute();
            
            // Process results
            if (result.isSuccess()) {
                // Log metadata
                log.info("Query executed in {} ms, returned {} rows",
                    result.getExecutionTimeMs(),
                    result.getCount());
                
                // Return data
                return result.getData();
            } else {
                log.error("Query failed: {}", result.getErrorMessage());
                return Collections.emptyList();
            }
            
        } catch (QueryException e) {
            log.error("Query execution failed: {}", e.getMessage());
            throw new ServiceException("Failed to fetch employees", e);
        }
    }
    
    public EmployeeDetails getEmployeeById(Integer employeeId) {
        QueryRow row = queryExecutor.execute("employeeDetails")
            .withParam("employeeId", employeeId)
            .executeSingle();
            
        if (row == null) {
            throw new NotFoundException("Employee not found: " + employeeId);
        }
        
        return EmployeeDetails.builder()
            .id(row.getInteger("employeeId"))
            .firstName(row.getString("firstName"))
            .lastName(row.getString("lastName"))
            .email(row.getString("email"))
            .salary(row.getBigDecimal("salary"))
            .hireDate(row.getLocalDate("hireDate"))
            .departmentName(row.getString("departmentName"))
            .build();
    }
}
```

## Validation

The executor automatically validates:
- Required parameters are provided
- Filters reference valid attributes
- Sort fields are sortable
- Page size doesn't exceed maximum

```java
try {
    QueryResult result = queryExecutor.execute("employees")
        .withParam("requiredParam", null) // Will fail if required
        .validate() // Explicit validation
        .execute();
} catch (QueryValidationException e) {
    List<String> violations = e.getViolations();
    // Handle validation errors
}
```

## Transaction Support

```java
@Transactional(readOnly = true)
public QueryResult executeInTransaction(String queryName) {
    return queryExecutor.execute(queryName).execute();
}
```

## Performance Tips

1. **Use field selection** to reduce data transfer
2. **Enable caching** for stable data
3. **Set appropriate fetch size** for large results
4. **Use pagination** for large datasets
5. **Add indexes** for filtered/sorted columns
6. **Use prepared statements** (automatic with named parameters)
7. **Set query timeouts** to prevent long-running queries