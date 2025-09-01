query Registration System - Complete Implementation Plan

Based on the SPEC.md and CLAUDE.md requirements, I will implement a production-ready Query Registration  
 System with the following structure:

1.  ✅ Maven Dependencies Update  


- ✅ Add all required dependencies (spring-boot-starter-jdbc, web, validation, cache, security)
- ✅ Add Lombok, Guava, Apache Commons, Vavr for utilities
- ✅ Add testing dependencies (TestContainers, AssertJ, RestAssured)
- ✅ Configure P6Spy for SQL logging in development  


2.  ✅ Core Domain Models (com.balasam.oasis.common.query.core.definition)  


- ✅ QueryDefinition - Immutable query definition with builder
- ✅ AttributeDef - Attribute metadata including mapping, security, filtering
- ✅ ParamDef - Parameter definitions with validation
- ✅ CriteriaDef - Dynamic SQL criteria definitions
- ✅ Row interface and RowImpl - Row data access abstraction
- ✅ QueryContext - Execution context with security and parameters
- ✅ QueryResult - Immutable result container with metadata

3.  ✅ Builder Pattern Implementation (com.balasam.oasis.common.query.builder)

- ✅ QueryDefinitionBuilder - Main builder for query definitions
- ✅ AttributeBuilder - Fluent builder for attributes
- ✅ VirtualAttributeBuilder - Builder for calculated/virtual attributes
- ✅ ParamBuilder - Parameter configuration builder
- ✅ CriteriaBuilder - Dynamic criteria builder  


4.  ✅ Processing Interfaces (com.balasam.oasis.common.query.processor)

- ✅ PreProcessor, RowProcessor, PostProcessor - Core processors
- ✅ Calculator, Validator, Converter, Formatter - Field processors
- ✅ CriteriaGenerator - Dynamic SQL generation
- ✅ SecurityRule - Attribute-level security
- ✅ Default implementations for common cases  


5.  Query Execution Engine (com.balasam.oasis.common.query.execution)

- QueryExecutor - Main execution interface
- QueryExecution - Fluent execution builder
- DynamicRowMapper - ResultSet to Row mapping with type conversion
- SqlBuilder - Dynamic SQL construction with placeholders
- FilterParser - Parse and apply filters
- SortParser - Handle sorting specifications
- PaginationHandler - Pagination logic  


6.  REST API Layer (com.balasam.oasis.common.query.rest)  


- QueryController - Main REST controller with GET endpoints
- QueryRequestParser - Parse URL parameters to execution parameters
- QueryResponseBuilder - Build response with metadata
- QueryExceptionHandler - Global exception handling
- Support for multiple formats (JSON, CSV, Excel)  


7.  Security Integration (com.balasam.oasis.common.query.security)  


- SecurityContext interface - User context abstraction
- SecurityContextProvider - Spring Security integration
- AttributeSecurityFilter - Apply security rules to attributes
- CriteriaSecurityFilter - Apply security-based criteria

8.  Caching Layer (com.balasam.oasis.common.query.cache)  


- QueryCacheManager - Cache management interface
- CaffeineCacheProvider - Default cache implementation
- CacheKeyGenerator - Generate cache keys from context
- CacheConfig - Per-query cache configuration  


9.  Configuration (com.balasam.oasis.common.query.config)

- QueryRegistrationAutoConfiguration - Spring Boot auto-config
- QueryRegistrationConfigurer - Programmatic configuration
- QueryProperties - Configuration properties
- GlobalProcessors - Shared processors and converters
- @EnableQueryRegistration annotation  


10. Exception Hierarchy (com.balasam.oasis.common.query.exception)  


- QueryException - Base exception
- QueryDefinitionException, QueryValidationException
- QueryExecutionException, QuerySecurityException
- QueryTimeoutException  


11. Testing Suite  


- Unit tests for all components
- Integration tests with TestContainers (PostgreSQL, MySQL)
- REST API tests with MockMvc
- Security tests for role-based access  


1.  Additional Features  


- Support for CTEs, UNION, Window Functions
- Batch operations support
- Streaming for large results
- Metrics and monitoring integration
- OpenAPI/Swagger documentation generation  


Implementation Order:

1.  Update pom.xml with all dependencies
2.  Create core domain models and interfaces
3.  Implement builders with validation
4.  Build execution engine with JdbcTemplate
5.  Add REST API layer
6.  Integrate security and caching
7.  Create comprehensive test suite
8.  Add configuration and auto-configuration
9.  Create example queries and documentation  


This implementation will be:

- Production-ready with proper error handling, logging, and monitoring
- Type-safe with immutable objects and builder pattern
- Secure with SQL injection prevention and role-based access
- Performant with caching, pagination, and connection pooling
- Testable with comprehensive test coverage
- Maintainable following SOLID principles and clean architecture
