# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Query Registration System - Development Rules & Tech Stack

## Development Plan

Read @PLAN.md for current dev plan, use it as check list, update it when you complete each step.
Read @SPEC.md for detailed requirements and specifications.

## Common Development Commands

### Build and Test
```bash
# Build the project
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=QueryExecutorIntegrationTest

# Run tests with specific pattern
mvn test -Dtest=*ControllerTest

# Build without tests
mvn clean install -DskipTests

# Generate test coverage report
mvn jacoco:report
```

### Running the Application
```bash
# Run with Maven
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debug enabled
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Database Access
```bash
# H2 Console is available at http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# Username: sa
# Password: (empty)
```

### API Documentation
```bash
# Swagger UI: http://localhost:8080/swagger-ui.html
# OpenAPI JSON: http://localhost:8080/v3/api-docs
```

## High-Level Architecture

### Core Architecture Pattern
The system follows a layered architecture with clear separation of concerns:

1. **Definition Layer** (`core.definition`): Immutable domain models that define queries, attributes, parameters, and criteria. All definitions use the Builder pattern with validation at build time.

2. **Execution Layer** (`core.execution`): Stateless execution engine that processes query definitions. Uses JdbcTemplate for database operations and implements dynamic SQL generation with named parameters.

3. **Processing Layer** (`processor`): Functional interfaces for extensibility - PreProcessor, RowProcessor, PostProcessor, Calculator, Validator, Converter, Formatter. Allows composition of processing logic.

4. **REST API Layer** (`rest`): Automatic REST endpoint generation for queries. Handles parameter parsing, filter operations, pagination, and response building with comprehensive metadata.

5. **Configuration Layer** (`config`): Spring Boot auto-configuration with @EnableQueryRegistration annotation. Manages global processors, caching, security integration.

### Key Design Patterns

- **Immutable Objects**: All definitions are immutable with builders (QueryDefinition, AttributeDef, ParamDef, CriteriaDef)
- **Fluent Builder Pattern**: Hierarchical builders that return parent for continuation (QueryDefinitionBuilder -> AttributeBuilder -> QueryDefinitionBuilder)
- **Functional Composition**: Processors and calculators can be composed for complex logic
- **Dynamic SQL Generation**: Comment-based placeholders (`--placeholderName`) in SQL replaced at runtime
- **Named Parameters**: Always use `:paramName` instead of `?` for SQL injection prevention

### SQL Placeholder System
SQL queries use comment placeholders that are dynamically replaced:
- `--filters`: Where filter conditions are injected
- `--orderBy`: Where ORDER BY clause is added
- `--limit`: Where LIMIT/OFFSET is added
- `--criteriaName`: Custom criteria injection points

Example:
```sql
SELECT * FROM users 
WHERE active = true 
--statusFilter
--dateFilter
--orderBy
--limit
```

## Tech Stack

### Core Dependencies
- **Spring Boot 3.5.5** with Java 21
- **spring-boot-starter-jdbc**: Core JDBC operations
- **spring-boot-starter-web**: REST API support
- **spring-boot-starter-validation**: Bean validation
- **spring-boot-starter-cache**: Caching with Caffeine
- **spring-boot-starter-security**: Optional security integration

### Database
- **H2 Database**: Default for development
- **PostgreSQL/MySQL**: Production support via TestContainers
- **HikariCP**: Connection pooling (included with Spring Boot)
- **P6Spy**: SQL logging in development

### Utilities
- **Lombok 1.18.30**: Reduce boilerplate
- **Guava 32.1.3**: Immutable collections and utilities
- **Apache Commons Lang3**: String and general utilities
- **Vavr 0.10.4**: Functional programming support
- **Caffeine 3.1.8**: High-performance caching

### Testing
- **spring-boot-starter-test**: Core testing support
- **TestContainers 1.19.3**: Integration testing with real databases
- **AssertJ 3.24.2**: Fluent assertions
- **Mockito 5.2.0**: Mocking framework
- **Rest Assured 5.4.0**: REST API testing

## Coding Rules

### 1. Architecture Rules
- Use immutable objects for all definitions with @Value or records
- Separate concerns: Definition, Execution, Result
- Use sealed classes for finite sets (FilterOp, SortDir)
- Fail fast with validation at build time using Preconditions

### 2. SQL & Database Rules
- Always use named parameters (`:paramName`), never positional (`?`)
- SQL in text blocks with proper formatting
- Use Spring's JdbcTemplate, not raw JDBC
- Batch operations for multiple rows

### 3. Builder Pattern Rules
- Fluent builders return self or parent for chaining
- Validate at build(), not during construction
- Child builders return parent via build() method

### 4. Functional Programming Rules
- Use functional interfaces for extensibility
- Prefer Optional over null
- Use Stream API for transformations
- Compose functions for complex logic

### 5. Error Handling Rules
- Custom exceptions with context (QueryException hierarchy)
- Use @Valid and Bean Validation
- Never catch generic Exception, be specific
- Include query name and context in exceptions

### 6. REST API Rules
- Use proper HTTP status codes (@ResponseStatus)
- @RequestParam for GET endpoints with MultiValueMap
- Support filter operators via dot notation (filter.name.like)
- Always include metadata in responses

### 7. Testing Rules
- Test behavior, not implementation
- Use TestContainers for database tests
- Mock external dependencies with @MockBean
- Test naming: shouldDoX_whenCondition

### 8. Performance Rules
- Always use pagination (max 1000 records)
- Prepare statements once and cache
- Stream large results with queryForStream
- Configure appropriate fetch size

### 9. Security Rules
- Never concatenate SQL strings
- Validate all inputs with Preconditions
- Apply security at definition time
- Use SecurityContext for role-based access

### 10. Code Organization
```
src/main/java/com/balasam/oasis/common/query/
├── core/
│   ├── definition/        # Immutable definitions
│   ├── execution/         # Execution engine
│   └── result/           # Result handling
├── builder/              # All builder classes
├── processor/            # Processing interfaces
├── rest/                 # REST controllers
├── config/               # Configuration
└── exception/            # Custom exceptions
```

## REST API Patterns

### GET Endpoint Format
```
GET /api/query/{queryName}?
    _start=0&_end=50                         # Pagination
    param.name=value                         # Parameters
    filter.field=value                       # Simple filter
    filter.field.op=LIKE&filter.field.value=pattern  # Complex filter
    sort=field.desc,field2.asc              # Sorting
    _meta=full                               # Metadata level
```

### Filter Operators
- `.eq` - equals
- `.like` - SQL LIKE
- `.in` - IN (comma-separated)
- `.gt`, `.gte`, `.lt`, `.lte` - Comparisons
- `.between` - Range
- `.null`, `.notnull` - Null checks

## Important Implementation Notes

1. **QueryRegistry**: All queries must be registered as Spring beans or via QueryRegistry
2. **Dynamic Criteria**: Use CriteriaDef with condition predicates for conditional SQL
3. **Virtual Attributes**: Calculated fields not from database, use dependencies
4. **Row Processing**: Chain processors - Pre -> Row -> Post
5. **Metadata**: Always include comprehensive metadata in REST responses
6. **Caching**: Query results cached by default with Caffeine, configurable TTL
7. **Validation**: All inputs validated at multiple levels (parameter, filter, execution)