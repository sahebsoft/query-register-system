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
mvnw clean compile

# Run all tests
mvnw test

# Run a specific test class
mvnw test -Dtest=QueryExecutorIntegrationTest

# Run tests with specific pattern
mvnw test -Dtest=*ControllerTest

# Build without tests
mvnw clean install -DskipTests

# Generate test coverage report
mvnw jacoco:report
```

### Running the Application
```bash
# Run with Maven
mvnw spring-boot:run

# Run with specific profile
mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debug enabled
mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Database Access
```bash
# Oracle Database connection
# Configure in application.properties
# spring.datasource.url=jdbc:oracle:thin:@localhost:1521:XE
# spring.datasource.username=hr
# spring.datasource.password=hr
```

### API Documentation
```bash
# Swagger UI: http://localhost:8080/swagger-ui.html
# OpenAPI JSON: http://localhost:8080/v3/api-docs
```

## High-Level Architecture

### Core Architecture Pattern
The system follows a layered architecture with clear separation of concerns:

1. **Definition Layer** (`core.definition`): Immutable domain models that define queries, attributes, parameters, and criteria. All definitions use @Value with Lombok for immutability. Includes generic typing for type-safety (AttributeDef<T>, ParamDef<T>).

2. **Execution Layer** (`core.execution`): Stateless execution engine that processes query definitions. Uses JdbcTemplate for database operations, implements dynamic SQL generation with named parameters, and supports Oracle database dialects (11g and 12c+).

3. **Processing Layer** (`processor`): Functional interfaces for extensibility - PreProcessor, RowProcessor, PostProcessor, AttributeProcessor<T>, AttributeFormatter<T>, ParamProcessor<T>, Validator, CriteriaGenerator. Allows composition of processing logic with type-safe generics.

4. **REST API Layer** (`rest`): Basic REST endpoint generation for queries via QueryController. Handles parameter parsing, basic filter operations, and pagination. Response building with QueryResponseBuilder (metadata support is planned).

5. **Configuration Layer** (`config`): Spring Boot auto-configuration with @EnableQueryRegistration annotation. Includes QueryProperties for configuration, GlobalProcessors for shared logic. Security integration is partially implemented.

### Key Design Patterns

- **Immutable Objects**: All definitions are immutable using @Value with Lombok (QueryDefinition, AttributeDef<T>, ParamDef<T>, CriteriaDef)
- **Builder Pattern**: Currently using inline builders with lambda customizers. Separate fluent builders (AttributeBuilder, ParamBuilder, CriteriaBuilder) are planned
- **Generic Types**: Type-safe definitions with generics (AttributeDef<T>, ParamDef<T>, processors with <T>)
- **Functional Composition**: Processors can be composed for complex logic using functional interfaces
- **Dynamic SQL Generation**: Comment-based placeholders (`--placeholderName`) in SQL replaced at runtime by SqlBuilder
- **Named Parameters**: Always use `:paramName` instead of `?` for SQL injection prevention
- **Database Dialect Support**: DatabaseDialect enum for multi-database compatibility

### SQL Placeholder System
SQL queries use comment placeholders that are dynamically replaced:
- `--orderBy`: Where ORDER BY clause is added
- `--criteriaName`: Custom criteria injection points

Example:
```sql
SELECT * FROM users 
WHERE active = true 
--statusFilter
--dateFilter
--orderBy
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
- **Oracle Database**: Support for both 11g and 12c+ versions
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
- Currently using inline builders with lambda customizers for attributes and params
- Validate at build(), not during construction  
- Future: Implement separate fluent builders that return parent for chaining

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

#### Current Structure
```
src/main/java/com/balasam/oasis/common/query/
├── core/
│   ├── definition/        # Immutable definitions (AttributeDef<T>, ParamDef<T>, etc.)
│   ├── execution/         # Execution engine (QueryExecutor, SqlBuilder, DynamicRowMapper)
│   └── result/           # Result handling (QueryResult, Row, RowImpl)
├── builder/              # QueryDefinitionBuilder (inline builders for now)
├── processor/            # Processing interfaces with generics
│   └── impl/            # Common processor implementations
├── rest/                 # REST controllers and request/response handling
├── config/               # Auto-configuration and properties
├── exception/            # Custom exception hierarchy
└── example/             # Example query configurations
```

#### Planned Structure (Future)
```
src/main/java/com/balasam/oasis/common/query/
├── core/                 # Core domain (as is)
├── builder/              # Expanded with AttributeBuilder, ParamBuilder, CriteriaBuilder
├── processor/            # Processing interfaces (as is)
├── rest/                 # REST API with JSON responses
├── config/               # Configuration (as is)
├── security/             # NEW - Security layer (SecurityContext, filters)
├── cache/                # NEW - Caching layer (CacheManager, providers)
├── exception/            # Exception hierarchy (as is)
└── example/             # Examples (as is)
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

### Current Implementation
1. **Query Definition**: Use QueryDefinitionBuilder with inline builders for attributes/params
2. **Dynamic Criteria**: CriteriaDef with condition predicates working
3. **Type Safety**: Generic types (AttributeDef<T>, ParamDef<T>) ensure compile-time safety
4. **Row Processing**: Chain processors - Pre -> Row -> Post implemented
5. **Database Support**: Oracle dialects supported (11g with ROWNUM, 12c+ with FETCH/OFFSET)
6. **REST API**: Basic QueryController with parameter/filter parsing

### Planned Features
1. **QueryRegistry**: Implement query discovery and registration
2. **Virtual Attributes**: Complete virtual fields implementation
3. **Metadata**: Add comprehensive metadata in REST responses
4. **Caching**: Implement Caffeine cache provider with configurable TTL
5. **Security**: Add SecurityContext and field-level security
6. **Validation**: Enhance multi-level validation (parameter, filter, execution)