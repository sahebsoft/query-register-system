# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Query Registration System - Development Rules & Tech Stack

## Common Development Commands

### Build and Test
```bash
# Clean build
./mvnw clean compile

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=QueryExecutorIntegrationTest
./mvnw test -Dtest=AttributeDefTest
./mvnw test -Dtest=AttributeDefDefaultsTest
./mvnw test -Dtest=AttributeDefHeaderStyleTest

# Run tests with pattern
./mvnw test -Dtest=*ControllerTest
./mvnw test -Dtest=*Test

# Build without tests (faster iterations)
./mvnw clean install -DskipTests
./mvnw package -DskipTests

# Generate test coverage report
./mvnw jacoco:report
```

### Running the Application
```bash
# Kill existing process on port 8080 and run
lsof -ti:8080 | xargs kill -9 2>/dev/null; ./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debug enabled (port 5005)
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Database Configuration
```properties
# Oracle Database (application.properties)
spring.datasource.url=jdbc:oracle:thin:@localhost:31521:XE
spring.datasource.username=hr
spring.datasource.password=hr
query.registration.database-dialect=ORACLE_11G
```

### API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## High-Level Architecture

### Two Parallel Implementations
1. **Main Query System** (`com.balsam.oasis.common.registry.*`) - Primary implementation, focus here
2. **Select System** (`com.balsam.oasis.common.registry.select.*`) - Newer simplified API

Both share core components but have separate execution paths. Default to Main Query system unless specifically working on Select features.

### Core Architecture Layers

1. **Definition Layer** (`domain.definition`)
   - Immutable domain models using `@Value` with Lombok
   - Generic types for type-safety: `AttributeDef<T>`, `ParamDef`
   - Key classes: AttributeDef, ParamDef, CriteriaDef

2. **Execution Layer** (`engine.*`)
   - Stateless execution engine with JdbcTemplate
   - Dynamic SQL generation with named parameters (`:paramName`)
   - Oracle dialect support (11g ROWNUM, 12c+ FETCH/OFFSET)
   - Key classes: QueryExecutorImpl, QuerySqlBuilder, QueryRowMapperImpl

3. **Processing Layer** (`processor.*`)
   - Functional interfaces for extensibility
   - Type-safe processors: `AttributeProcessor<T>`, `ParamProcessor<T>`
   - Execution order: PreProcessor → RowProcessor → PostProcessor
   - Composition pattern for complex logic

4. **REST API Layer** (`web.*`)
   - QueryController for endpoint generation
   - QueryRequestParser for parameter/filter/sort parsing
   - QueryResponseBuilder for response formatting

5. **Configuration Layer** (`config.*`)
   - Spring Boot auto-configuration
   - @EnableQueryRegistration annotation
   - QueryProperties for runtime configuration

### Critical Registration Flow
1. **Build Phase**: `QueryDefinitionBuilder.build()` → creates immutable QueryDefinition → validates via QueryDefinitionValidator
2. **Registration Phase**: `QueryExecutor.registerQuery()` or `QueryRegistry.register()` → adds to runtime registry
3. **Execution Phase**: QueryExecutor looks up registered queries → executes with runtime context
4. **IMPORTANT**: Never register queries globally during build - causes side effects and test pollution

### SQL Placeholder System
Dynamic SQL with comment placeholders replaced at runtime:
```sql
SELECT * FROM employees 
WHERE active = 'Y' 
--statusFilter      -- Dynamic criteria injection
--dateFilter        -- Dynamic criteria injection  
--orderBy          -- ORDER BY clause insertion
```

## Tech Stack

### Core Dependencies
- **Spring Boot 3.5.5** with Java 21
- **spring-boot-starter-jdbc**: Core JDBC operations
- **spring-boot-starter-web**: REST API support
- **spring-boot-starter-validation**: Bean validation
- **Lombok**: Immutable objects with @Value
- **Guava 32.1.3**: Collections and utilities
- **Oracle JDBC Driver**: ojdbc8

### Database
- **Oracle Database**: 11g and 12c+ support
- **HikariCP**: Connection pooling (included)
- **Named Parameters**: Always use `:paramName` (never `?`)

## Important Implementation Details

### Type System and Generics
- All attribute definitions use generics: `AttributeDef<T>`
- Processors are type-safe: `AttributeProcessor<T>`, `ParamProcessor<T>`
- Type converters handle Oracle-specific types (NUMBER → BigDecimal)

### Metadata Cache System
- **MetadataCache**: Attached to QueryDefinition after build (mutable)
- **MetadataCacheBuilder**: Pre-warms cache with column metadata
- **Cache warming**: Happens during registration in QueryExecutor

### Processor Execution Order
1. **PreProcessors**: Modify QueryContext before SQL execution
2. **RowProcessors**: Process each row during result mapping
3. **PostProcessors**: Modify final QueryResult after all rows

### Virtual/Calculated Attributes
- Virtual attributes: `virtual(true)` - not from database
- Calculated attributes: Use `calculated()` lambda
- Virtual attributes default to non-filterable/non-sortable

### Key Files to Understand First
1. **QueryDefinition.java** - Core immutable query model
2. **QueryDefinitionBuilder.java** - Fluent builder (validates but doesn't register)
3. **QueryExecutorImpl.java** - Runtime execution engine with local registry
4. **QuerySqlBuilder.java** - Dynamic SQL generation with placeholder handling
5. **QueryController.java** - REST endpoint implementation
6. **QueryRequestParser.java** - Request parameter parsing
7. **CriteriaUtils.java** - Dynamic criteria SQL generation
8. **OracleHRQueryConfig.java** - Example query configurations

## REST API Patterns

### GET Endpoint Format
```
GET /api/query/{queryName}?
    _start=0&_end=50                              # Pagination
    name=value                                     # Parameters
    filter.field=value                             # Simple filter
    filter.field.op=LIKE&filter.field.value=%pat% # Complex filter
    sort=field.desc,field2.asc                    # Sorting
    _meta=full                                     # Metadata level
```

### Filter Operators
- `.eq` - Equals (default)
- `.like` - SQL LIKE pattern
- `.in` - IN clause (comma-separated)
- `.gt`, `.gte`, `.lt`, `.lte` - Comparisons
- `.between` - Range (value1,value2)
- `.null`, `.notnull` - Null checks

## Current Implementation Status

### Working Features
1. **Query Definition**: QueryDefinitionBuilder with inline builders
2. **Dynamic Criteria**: CriteriaDef with condition predicates
3. **Type Safety**: Generic types ensure compile-time safety
4. **Row Processing**: Full processor chain implementation
5. **Oracle Support**: Both 11g and 12c+ dialects
6. **REST API**: Basic endpoint with parameter/filter parsing
7. **Metadata Cache**: Column metadata pre-warming

### In Progress
- Security layer implementation
- Enhanced caching providers
- Metadata in REST responses

## Development Guidelines

### When Modifying Code
1. Check git status/diff before making changes
2. Follow existing patterns in neighboring files
3. Use existing utilities (don't reinvent)
4. Always use named parameters for SQL
5. Maintain immutability for definitions
6. Never register queries globally in builders

### Testing
- Unit tests in `src/test/java`
- Integration tests require Oracle database
- Run specific tests during development
- Always verify with `./mvnw test` before committing