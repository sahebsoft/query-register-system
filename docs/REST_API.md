# REST API Documentation

## Base URLs
- Query API: `/api/query/v2`
- Select API: `/api/select/v2`

## Query Endpoints

### Execute Query
Execute a registered query with filters, sorting, and pagination.

#### Request
```
GET /api/query/v2/{queryName}
```

#### Query Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `{param}` | Any | Depends | Query-specific parameters | `deptId=100` |
| `_start` | Integer | No | Pagination start index (default: 0) | `_start=0` |
| `_end` | Integer | No | Pagination end index (default: 50) | `_end=50` |
| `_meta` | String | No | Metadata level: none, basic, full | `_meta=full` |
| `filter.{field}` | String | No | Simple filter | `filter.status=ACTIVE` |
| `filter.{field}.op` | String | No | Filter operator | `filter.salary.op=gt` |
| `filter.{field}.value` | Any | No | Filter value | `filter.salary.value=50000` |
| `sort` | String | No | Sort specification | `sort=lastName.asc,firstName.desc` |

#### Filter Operators
- `eq` - Equals (default)
- `ne` - Not equals
- `gt` - Greater than
- `gte` - Greater than or equal
- `lt` - Less than
- `lte` - Less than or equal
- `like` - Pattern match (use % for wildcard)
- `in` - In list (comma-separated)
- `between` - Range (two comma-separated values)
- `null` - Is null
- `notnull` - Is not null

#### Response
```json
{
  "data": [
    {
      "employeeId": 100,
      "firstName": "John",
      "lastName": "Doe",
      "salary": 75000,
      "departmentName": "IT"
    }
  ],
  "metadata": {
    "pagination": {
      "start": 0,
      "end": 50,
      "total": 250,
      "hasNext": true,
      "hasPrevious": false
    },
    "attributes": [...],
    "performance": {
      "executionTimeMs": 45,
      "rowsFetched": 50
    }
  },
  "summary": {
    "customKey": "customValue"
  }
}
```

### List Queries
Get all registered queries.

```
GET /api/query/v2
```

Response:
```json
{
  "queries": [
    {
      "name": "employees",
      "description": "Employee query with filters",
      "parameters": ["deptId", "minSalary"],
      "attributes": ["employeeId", "firstName", "lastName"]
    }
  ]
}
```

## Select Endpoints (for Dropdowns)

### Get List of Values
Execute a select query optimized for dropdowns.

#### Request
```
GET /api/select/v2/{selectName}
```

#### Query Parameters
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `id` | List | No | IDs to fetch (for default values) | `id=1&id=2&id=3` |
| `search` | String | No | Search term | `search=john` |
| `_start` | Integer | No | Pagination start | `_start=0` |
| `_end` | Integer | No | Pagination end | `_end=100` |
| Other params | Any | No | Query-specific parameters | `locationId=10` |

#### Response
```json
{
  "items": [
    {
      "value": "100",
      "label": "Information Technology",
      "additions": {
        "city": "San Francisco",
        "country": "USA"
      }
    },
    {
      "value": "200",
      "label": "Human Resources",
      "additions": {
        "city": "New York",
        "country": "USA"
      }
    }
  ],
  "total": 15,
  "hasMore": false
}
```

## Complete Examples

### 1. Simple Query
```bash
GET /api/query/v2/employees?deptId=100
```

### 2. Query with Filters and Sorting
```bash
GET /api/query/v2/employees?
  deptId=100&
  filter.salary.op=gte&
  filter.salary.value=50000&
  filter.status=ACTIVE&
  sort=lastName.asc,firstName.asc&
  _start=0&
  _end=20
```

### 3. Query with Complex Filters
```bash
GET /api/query/v2/employees?
  filter.departmentId.op=in&
  filter.departmentId.value=10,20,30&
  filter.hireDate.op=between&
  filter.hireDate.value=2020-01-01,2023-12-31&
  filter.email.op=like&
  filter.email.value=%@company.com
```

### 4. Select Query with Search
```bash
GET /api/select/v2/departments?
  search=tech&
  _start=0&
  _end=50
```

### 5. Select Query with Default Values
```bash
GET /api/select/v2/employees?
  id=100&id=200&id=300
```

## Error Responses

### 400 Bad Request
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed: Required parameter missing: deptId",
  "queryName": "employees",
  "timestamp": 1699123456789,
  "violations": [
    "Required parameter missing: deptId",
    "Invalid filter operator: unknown"
  ]
}
```

### 404 Not Found
```json
{
  "code": "QUERY_NOT_FOUND",
  "message": "Query not found: unknown_query",
  "queryName": "unknown_query",
  "timestamp": 1699123456789
}
```

### 500 Internal Server Error
```json
{
  "code": "EXECUTION_ERROR",
  "message": "Query execution failed: Database connection error",
  "queryName": "employees",
  "timestamp": 1699123456789
}
```

## Request Headers

### Optional Headers
```
Accept: application/json
X-Request-ID: unique-request-id
X-User-Context: user-context-json
```

## Response Headers
```
Content-Type: application/json
X-Query-Time: 45
X-Total-Count: 250
X-Cache-Status: HIT|MISS
```

## Pagination Patterns

### 1. Offset/Limit Style
```
?_start=100&_end=150  // Skip 100, take 50
```

### 2. Page Number Style (converted internally)
```
?page=3&pageSize=50  // Converted to _start=100&_end=150
```

## Filtering Patterns

### 1. Simple Filter (equals)
```
?filter.status=ACTIVE
```

### 2. Operator-based Filter
```
?filter.salary.op=gte&filter.salary.value=50000
```

### 3. Multiple Values (IN)
```
?filter.department.op=in&filter.department.value=IT,HR,Finance
```

### 4. Range (BETWEEN)
```
?filter.age.op=between&filter.age.value=25,65
```

### 5. Pattern Matching (LIKE)
```
?filter.name.op=like&filter.name.value=John%
```

### 6. Null Checks
```
?filter.endDate.op=null
?filter.manager.op=notnull
```

## Sorting Patterns

### 1. Single Sort
```
?sort=lastName.asc
```

### 2. Multiple Sorts
```
?sort=department.asc,salary.desc,lastName.asc
```

### 3. Default Sort (no direction specified defaults to ASC)
```
?sort=lastName
```

## Best Practices

1. **Always use pagination** for large datasets
2. **Specify only needed fields** using field selection
3. **Use appropriate metadata level** (none for production, full for debugging)
4. **Cache responses** on client when appropriate
5. **Use batch requests** for multiple queries when possible
6. **Include request IDs** for tracking
7. **Handle errors gracefully** with proper error messages
8. **Use filters** instead of fetching all data and filtering client-side

## Rate Limiting

API endpoints may be rate-limited. Check response headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1699124000
```

## CORS Configuration

The API supports CORS for browser-based applications:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Request-ID
```