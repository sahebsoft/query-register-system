# Copilot / AI Agent Instructions for query-register-system

Quick orientation
- Purpose: small framework to define and execute parameterized SQL "queries" as immutable QueryDefinition objects and execute them via a runtime QueryExecutor.
- Key runtime flow: build QueryDefinition -> validate (builder/validator) -> register with runtime -> execute via QueryExecutorImpl.

Important files to read first
- `src/main/java/com/balsam/oasis/common/query/core/definition/QueryDefinition.java` — immutable DTO for queries (attributes, params, criteria, processors, cache config).
- `src/main/java/com/balsam/oasis/common/query/builder/QueryDefinitionBuilder.java` — fluent builder. Note: builder performs validation but should NOT perform global registration.
- `src/main/java/com/balsam/oasis/common/query/validation/QueryDefinitionValidator.java` — contains validation and global registry helpers (validate, register, validateAndRegister for backward compatibility).
- `src/main/java/com/balsam/oasis/common/query/core/execution/QueryExecutorImpl.java` — execution engine, local registry and metadata cache handling. Executors are responsible for registering definitions for runtime lookups.
- `src/main/java/com/balsam/oasis/common/query/core/execution/SqlBuilder.java` — dynamic SQL placeholder handling (look for `--placeholderName` comment placeholders).
- `src/main/java/com/balsam/oasis/common/query/processor/*` — functional interfaces: `PreProcessor`, `RowProcessor`, `PostProcessor`.
- `src/main/java/com/balsam/oasis/common/query/config/QueryConfiguration.java` — Spring bean wiring; executor constructed here (note: QueryRegistrar is created in configuration when present).
- `src/main/java/com/balsam/oasis/common/query/rest/QueryController.java` and `QueryRequestParser.java` — REST entry points and request parsing.

Core architectural notes agents must follow
- Single responsibility: QueryDefinition is a plain immutable data-holder; QueryDefinitionBuilder composes and validates but should not register globally; registration is a runtime concern owned by `QueryExecutorImpl` (or an injected `QueryRegistrar`).
- Processor types are strongly typed (use the provided `PreProcessor/RowProcessor/PostProcessor` interfaces). Avoid working with raw `Function<Object,Object>` unless maintaining backward compatibility — builder has adapter overloads for those.
- SQL placeholders: the codebase uses comment placeholders like `--criteriaName` and `--orderBy`. When modifying SQL generation or `SqlBuilder`, honor this placeholder convention.
- Named parameters only: use `:paramName` in SQL. Do not introduce positional parameters.

Build / test / run (developer commands)
- Build and tests (use wrapper):
  - `./mvnw clean compile`
  - `./mvnw test` (run full test suite)
  - `./mvnw test -Dtest=SomeTest` (single test)
- Run application: `./mvnw spring-boot:run` (default: port 8080). Use `-Dspring-boot.run.profiles=dev` to select profile.
- When iterating locally, prefer `-DskipTests` during packaging: `./mvnw package -DskipTests`.

Project-specific conventions
- Validation: prefer failing fast during `build()` via `QueryDefinitionBuilder` and `QueryDefinitionValidator`. Use `Preconditions` for input checks.
- Registration: if code previously called `QueryDefinitionValidator.validateAndRegister`, maintain backward compatibility but prefer `QueryDefinitionValidator.validate(...)` in builders and perform actual registration via `QueryExecutorImpl.registerQuery(...)` or `QueryRegistrar.register(...)` at runtime.
- Processor signatures: use `PreProcessor.process(QueryContext)`, `RowProcessor.process(Row, QueryContext)`, `PostProcessor.process(QueryResult, QueryContext)`.
- Metadata cache: metadata is mutable and attached to `QueryDefinition` after build; `QueryExecutorImpl` pre-warms and sets `MetadataCache` on definitions.

Integration and external dependencies
- Primary DB: Oracle (connection configured in `application.properties`). The SQL builder contains dialect-aware pagination logic.
- Caching: implemented via `CacheConfig` on `QueryDefinition` and (planned) Caffeine provider.
- Spring Boot: standard DI. Beans are configured in `QueryConfiguration`.

What to do when editing code
- If touching `QueryDefinitionBuilder`:
  - Do not add global side-effects (registration). Call `QueryDefinitionValidator.validate(queryDef)` only.
  - Provide adapter overloads if accepting legacy `Function<Object,Object>` processors, but prefer typed interfaces.
- If touching `QueryExecutorImpl`:
  - Keep local `queryRegistry` for quick lookups and accept an injected `QueryRegistrar` if available for global registration.
  - Ensure `preProcessors`, `rowProcessors`, and `postProcessors` are invoked via typed `process(...)` methods.
- If touching `QueryDefinitionValidator`:
  - Maintain `validate(...)` and `register(...)` separated. Keep `validateAndRegister(...)` as a compatibility wrapper.

Tests and examples
- Tests under `src/test/java/com/balsam/oasis/common/query/validation` verify duplicate detection and registration behavior; update tests if you change validator contract.
- Use provided test helpers for building minimal `QueryDefinition` objects (look at tests in `src/test/java` for patterns).

If you need to refactor
- Prefer small, backwards-compatible changes. Add adapter methods rather than remove legacy APIs in the same commit.
- Update `QueryConfiguration` to wire `QueryRegistrar` into `QueryExecutorImpl` when introducing the registrar abstraction.

If anything is unclear or you need more context, ask for:
- Which callers rely on `validateAndRegister(...)` so we can update them safely.
- Whether you want a single authoritative registry (move-to `QueryRegistrar`) or keep the `QueryDefinitionValidator` global registry for tests.

Please review these instructions and tell me any missing areas to expand (examples, file-level notes, or common refactor patterns to automate).
