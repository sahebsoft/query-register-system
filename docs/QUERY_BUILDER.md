# Query Builder Documentation

## Overview
The `QueryDefinitionBuilder` provides a fluent API for defining SQL queries with dynamic criteria, filtering, sorting, and pagination support.

## Basic Usage

```java
QueryDefinitionBuilder query = QueryDefinitionBuilder.builder("employees")
    .sql("SELECT * FROM employees WHERE 1=1 --filters")
    .description("Employee query with filters")
    .build();
```

## Core Components

### 1. Attributes Definition
Attributes define the columns/fields returned by your query.

```java
.attribute(AttributeDef.name("employeeId", Integer.class)
    .aliasName("employee_id")     // Database column name
    .primaryKey(true)              // Mark as primary key
    .label("Employee ID")          // Display label
    .width("100px")               // Display width
    .build())
```

#### Virtual/Calculated Attributes
```java
.attribute(AttributeDef.name("fullName", String.class)
    .calculated((row, context) -> 
        row.getString("firstName") + " " + row.getString("lastName"))
    .build())
```

#### Formatted Attributes
```java
.attribute(AttributeDef.name("salary", BigDecimal.class)
    .aliasName("salary")
    .formatter(value -> String.format("$%.2f", value))
    .build())
```

### 2. Parameters
Define query parameters that can be passed at execution time.

```java
.parameter(ParamDef.name("deptId", Integer.class)
    .required(true)                // Makes parameter mandatory
    .defaultValue(100)              // Default if not provided
    .processor((value, ctx) -> {    // Custom processing
        return value != null ? value : 100;
    })
    .build())
```

### 3. Dynamic Criteria
Add conditional SQL fragments that activate based on context.

```java
.criteria(CriteriaDef.name("departmentFilter")
    .sql("AND department_id = :deptId")
    .condition(ctx -> ctx.hasParam("deptId"))  // Only apply if param exists
    .build())
```

SQL with placeholders:
```sql
SELECT * FROM employees 
WHERE 1=1
--departmentFilter
--salaryFilter
--dateFilter
```

### 4. Processors

#### Pre-Processors (before query execution)
```java
.preProcessor(context -> {
    // Modify context before execution
    if (!context.hasParam("status")) {
        context.addParam("status", "ACTIVE");
    }
})
```

#### Row Processors (for each row)
```java
.rowProcessor((row, context) -> {
    // Transform each row
    String status = row.getString("status");
    row.set("statusLabel", status.equals("A") ? "Active" : "Inactive");
    return row;
})
```

#### Post-Processors (after all rows)
```java
.postProcessor((result, context) -> {
    // Add summary information
    return result.toBuilder()
        .summary(Map.of("totalRows", result.size()))
        .build();
})
```

### 5. Pagination & Performance

```java
.defaultPageSize(20)           // Default rows per page
.maxPageSize(100)              // Maximum allowed page size
.paginationEnabled(true)       // Enable/disable pagination
.fetchSize(100)                // JDBC fetch size
.queryTimeout(30)              // Query timeout in seconds
```

### 6. Caching

```java
.cache(true)                              // Enable caching
.cacheTTL(Duration.ofMinutes(5))         // Cache duration
.cacheKey(params -> params.toString())    // Custom cache key
```

### 7. Dynamic Attributes
Automatically discover and include columns not explicitly defined.

```java
.dynamic()                          // Use CAMEL case naming
.dynamic(NamingStrategy.LOWER)     // Use lower_case naming
```

## Complete Example

```java
QueryDefinitionBuilder employeeQuery = QueryDefinitionBuilder.builder("employees")
    .sql("""
        SELECT 
            e.employee_id,
            e.first_name,
            e.last_name,
            e.salary,
            d.department_name
        FROM employees e
        LEFT JOIN departments d ON e.department_id = d.department_id
        WHERE 1=1
        --departmentFilter
        --salaryFilter
        --orderBy
        """)
    .description("Employee query with department info")
    
    // Attributes
    .attribute(AttributeDef.name("employeeId", Integer.class)
        .aliasName("employee_id")
        .primaryKey(true)
        .build())
    .attribute(AttributeDef.name("firstName", String.class)
        .aliasName("first_name")
        .build())
    .attribute(AttributeDef.name("lastName", String.class)
        .aliasName("last_name")
        .build())
    .attribute(AttributeDef.name("salary", BigDecimal.class)
        .formatter(val -> String.format("$%.2f", val))
        .build())
    .attribute(AttributeDef.name("fullName", String.class)
        .calculated((row, ctx) -> 
            row.getString("firstName") + " " + row.getString("lastName"))
        .build())
    
    // Parameters
    .parameter(ParamDef.name("deptId").build())
    .parameter(ParamDef.name("minSalary", BigDecimal.class).build())
    
    // Criteria
    .criteria(CriteriaDef.name("departmentFilter")
        .sql("AND e.department_id = :deptId")
        .condition(ctx -> ctx.hasParam("deptId"))
        .build())
    .criteria(CriteriaDef.name("salaryFilter")
        .sql("AND e.salary >= :minSalary")
        .condition(ctx -> ctx.hasParam("minSalary"))
        .build())
    
    // Processors
    .preProcessor(ctx -> {
        System.out.println("Query starting: " + ctx.getDefinition().getName());
    })
    .postProcessor((result, ctx) -> {
        return result.toBuilder()
            .summary(Map.of("recordCount", result.size()))
            .build();
    })
    
    // Configuration
    .defaultPageSize(50)
    .maxPageSize(200)
    .cache(true)
    .cacheTTL(Duration.ofMinutes(10))
    
    .build();
```

## Select Definition Builder

Special builder for dropdown/select queries:

```java
QueryDefinitionBuilder select = SelectDefinitionBuilder.builder("departments")
    .sql("SELECT dept_id, dept_name FROM departments")
    .valueAttribute("dept_id", Integer.class)   // Value field
    .labelAttribute("dept_name")                // Display field
    .build();
```

## Registration

After building, register with the QueryRegistry:

```java
@Autowired
private QueryRegistry queryRegistry;

queryRegistry.register(employeeQuery);
```

## Best Practices

1. **Always use named parameters** (`:paramName`) not positional (`?`)
2. **Define primary keys** for proper row identification
3. **Use criteria placeholders** (`--criteriaName`) for dynamic SQL
4. **Validate parameters** with processors
5. **Set appropriate timeouts** for long-running queries
6. **Use caching** for frequently accessed, stable data
7. **Define fetch size** based on expected result size