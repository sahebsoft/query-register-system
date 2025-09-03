## Copilot instructions for Query Registration System

This file gives concise, actionable guidance for an AI coding agent to be immediately productive in this repository.

1) Quick orientation
- Tech: Java + Spring Boot (Maven wrapper). Project root contains `mvnw` and `pom.xml`.
- JVM: project targets modern Java (see `pom.xml`; examples use Java 21). Use the wrapper: `./mvnw`.

2) Key commands (run from repo root)
- Build: `./mvnw clean compile`
- Tests: `./mvnw test` (single test: `-Dtest=QueryExecutorIntegrationTest`)
- Run app: `./mvnw spring-boot:run` (profiles via `-Dspring-boot.run.profiles=dev`)
- Coverage: `./mvnw jacoco:report`

3) Big-picture architecture (what to read first)
- Entry: `src/main/java/com/balasam/oasis/common/query/QueryRegistrationApplication.java`.
- Auto-config / wiring: `config/QueryRegistrationAutoConfiguration.java`, `config/EnableQueryRegistration.java`, `config/QueryRegistrationConfigurer.java`.
- Definitions: immutable domain models in `core/definition/` (e.g. `QueryDefinition.java`, `AttributeDef.java`, `ParamDef.java`).
- Builders: fluent builders in `builder/` (start with `QueryDefinitionBuilder.java`). Builders validate at `build()`.
- Execution: runtime SQL and execution logic in `core/execution/` (uses Spring's `JdbcTemplate`).
- REST: controller and request parsing in `rest/` (`QueryController.java`, `QueryRequestParser.java`, `QueryResponseBuilder.java`).
- Processors: composable hooks in `processor/` (e.g. `PreProcessor`, `RowProcessor`, `PostProcessor`).

4) Project-specific conventions and patterns (don't guess — follow these)
- Immutable definitions + fluent builders: always construct query metadata via the builder API and call `build()` to validate.
- Named SQL parameters only: always use `:paramName` (never positional `?`). The engine assumes named parameters.
- SQL comment placeholders: queries include comment markers where dynamic SQL is injected — common tokens:  `--<criteriaName>`. Preserve these comments when editing SQL.
- Dynamic criteria and virtual attributes: Criteria are declared with `CriteriaDef` and injected conditionally; virtual attributes exist and are calculated via processors.
- Processor composition: processing is done via small functional interfaces; prefer composing `PreProcessor -> RowProcessor -> PostProcessor` rather than monolithic methods.

5) Important integration points & config
- Caching: Caffeine-based caching is configured via `config/GlobalProcessors.java` and `config/QueryProperties.java`.
- Security: optional integration via `config/SecurityConfig.java` and checks in `exception/QuerySecurityException.java`.
- Data access: `core/execution` uses `JdbcTemplate`; SQL logging and test DBs use TestContainers (see `src/main/resources` and tests).

6) Tests and examples to mirror
- Example query registration is in `example/OracleHRQueryConfig.java` — use this as a template for adding new queries or beans with @PostConstruct registration.
- Tests live under `src/test/java/...` (integration tests: `QueryExecutorIntegrationTest.java`, controller tests: `rest/QueryControllerTest.java`, builder unit tests: `builder/QueryDefinitionBuilderTest.java`).

7) Common pitfalls to avoid
- Do not remove or rename SQL placeholder comments — runtime SQL assembly depends on them.
- Avoid raw string concatenation for SQL; use named parameters and builders.
- Builders validate at `build()`; failing fast is expected — fix data model or builder inputs rather than swallowing exceptions.

8) How to register new queries
- Create immutable definition using `QueryDefinitionBuilder` (example in `example/OracleHRQueryConfig.java`).
- Register the resulting `QueryDefinition` as a Spring bean or add it to `QueryRegistry` during application start.
- Add unit + integration tests that exercise SQL placeholders and processors.

9) Files & symbols to reference when making changes
- `builder/QueryDefinitionBuilder.java` — builder idioms and parent/child chaining
- `rest/QueryController.java` & `rest/QueryRequestParser.java` — request parsing, filters, pagination
- `core/execution/MetadataBuilder.java` — how SQL placeholders map to runtime clauses
- `config/QueryRegistrationAutoConfiguration.java` — auto-configuration and default beans

10) When in doubt
- Read the builder and definition classes first to understand the data shape before changing parsing or SQL assembly.
- Run the small suite of tests in `src/test/java/...` that correspond to your change (run the exact test with `-Dtest=...`).

If anything here is unclear or you'd like more examples (e.g., a short example of a new QueryDefinition + test), tell me which part to expand.
