# Query System Engine Workflow

## System Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        REST[REST API Client]
        WEB[Web Application]
    end
    
    subgraph "API Layer"
        QC[QueryController]
        SC[SelectController]
        RP[RequestParser]
        RB[ResponseBuilder]
    end
    
    subgraph "Service Layer"
        QS[QueryService]
    end
    
    subgraph "Execution Layer"
        QE[QueryExecutor]
        QEX[QueryExecution]
    end
    
    subgraph "Builder Layer"
        QB[QueryBuilder]
        SB[SqlBuilder]
        MC[MetadataCache]
    end
    
    subgraph "Registry Layer"
        QR[QueryRegistry]
        QD[QueryDefinitions]
    end
    
    subgraph "Data Layer"
        JT[JdbcTemplate]
        DB[(Oracle Database)]
    end
    
    REST --> QC
    WEB --> SC
    QC --> RP
    SC --> RP
    QC --> QS
    SC --> QS
    QS --> QE
    QE --> QEX
    QEX --> QR
    QR --> QD
    QEX --> SB
    SB --> MC
    QEX --> JT
    JT --> DB
    QC --> RB
    SC --> RB
```

## Query Definition & Registration Flow

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant QB as QueryBuilder
    participant VAL as Validator
    participant MCB as MetadataCacheBuilder
    participant QR as QueryRegistry
    participant DB as Database
    
    Dev->>QB: Create query definition
    QB->>QB: Define attributes
    QB->>QB: Define parameters
    QB->>QB: Define criteria
    QB->>VAL: Validate definition
    VAL-->>QB: Validation result
    QB->>MCB: Build metadata cache
    MCB->>DB: Fetch column metadata
    DB-->>MCB: Column information
    MCB-->>QB: MetadataCache
    QB->>QR: Register query
    QR->>QR: Store in registry
    QR-->>Dev: Registration complete
```

## Query Execution Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as REST API
    participant Parser as RequestParser
    participant QE as QueryExecutor
    participant QR as QueryRegistry
    participant CTX as QueryContext
    participant SQL as SqlBuilder
    participant JDBC as JdbcTemplate
    participant DB as Database
    participant Mapper as RowMapper
    
    Client->>API: GET /api/query/employees?deptId=100
    API->>Parser: Parse request
    Parser->>Parser: Extract params, filters, sort
    Parser-->>API: QueryRequest
    
    API->>QE: execute("employees")
    QE->>QR: get("employees")
    QR-->>QE: QueryDefinition
    
    QE->>CTX: Create context
    CTX->>CTX: Add parameters
    CTX->>CTX: Add filters
    CTX->>CTX: Add sorting
    
    QE->>QE: Run PreProcessors
    
    QE->>SQL: build(context)
    SQL->>SQL: Apply criteria
    SQL->>SQL: Apply filters
    SQL->>SQL: Apply sorting
    SQL->>SQL: Apply pagination
    SQL-->>QE: Final SQL + params
    
    QE->>JDBC: Execute query
    JDBC->>DB: SQL query
    DB-->>JDBC: ResultSet
    
    JDBC->>Mapper: Map rows
    Mapper->>Mapper: Process attributes
    Mapper->>Mapper: Calculate virtuals
    Mapper->>Mapper: Apply formatters
    Mapper-->>JDBC: QueryRows
    
    QE->>QE: Run RowProcessors
    QE->>QE: Run PostProcessors
    
    QE-->>API: QueryResult
    API-->>Client: JSON response
```

## SQL Building Process

```mermaid
flowchart TD
    Start([Start with base SQL]) --> Criteria{Has Criteria?}
    Criteria -->|Yes| ApplyCriteria[Apply Dynamic Criteria]
    Criteria -->|No| Filters
    ApplyCriteria --> Filters{Has Filters?}
    
    Filters -->|Yes| BuildFilter[Build Filter Clauses]
    Filters -->|No| Sorting
    BuildFilter --> WrapFilter[Wrap SQL with WHERE]
    WrapFilter --> Sorting{Has Sorting?}
    
    Sorting -->|Yes| BuildSort[Build ORDER BY]
    Sorting -->|No| Pagination
    BuildSort --> Pagination{Has Pagination?}
    
    Pagination -->|Yes| CheckDialect{Check Dialect}
    Pagination -->|No| Clean
    
    CheckDialect -->|Oracle 11g| RowNum[Apply ROWNUM]
    CheckDialect -->|Oracle 12c+| Offset[Apply OFFSET/FETCH]
    CheckDialect -->|Other| Standard[Apply Standard]
    
    RowNum --> Clean
    Offset --> Clean
    Standard --> Clean
    
    Clean[Clean Placeholders] --> Final([Final SQL])
```

## Row Mapping Process

```mermaid
flowchart LR
    subgraph "Row Mapping Pipeline"
        RS[ResultSet] --> Extract[Extract Raw Data]
        Extract --> Process[Process Attributes]
        Process --> Virtual{Virtual Attrs?}
        Virtual -->|Yes| Calculate[Calculate Values]
        Virtual -->|No| Format
        Calculate --> Format{Has Formatters?}
        Format -->|Yes| ApplyFormat[Apply Formatters]
        Format -->|No| Dynamic
        ApplyFormat --> Dynamic{Dynamic Mode?}
        Dynamic -->|Yes| AddDynamic[Add Dynamic Attrs]
        Dynamic -->|No| Final
        AddDynamic --> Final[QueryRow]
    end
```

## Processor Chain Execution

```mermaid
flowchart TD
    subgraph "Processor Chain"
        Start([Query Start]) --> Pre{PreProcessors?}
        Pre -->|Yes| RunPre[Run PreProcessors]
        Pre -->|No| Execute
        RunPre --> Execute[Execute SQL]
        
        Execute --> Row{RowProcessors?}
        Row -->|Yes| ProcessRows[Process Each Row]
        Row -->|No| Post
        ProcessRows --> Post{PostProcessors?}
        
        Post -->|Yes| RunPost[Run PostProcessors]
        Post -->|No| Meta
        RunPost --> Meta{Include Metadata?}
        
        Meta -->|Yes| AddMeta[Add Metadata]
        Meta -->|No| Result
        AddMeta --> Result([QueryResult])
    end
```

## Cache Strategy Flow

```mermaid
flowchart TD
    Request([Query Request]) --> CheckCache{Cache Enabled?}
    CheckCache -->|No| Execute[Execute Query]
    CheckCache -->|Yes| GenerateKey[Generate Cache Key]
    GenerateKey --> LookupCache{Key Exists?}
    LookupCache -->|Yes| CheckTTL{TTL Valid?}
    LookupCache -->|No| Execute
    CheckTTL -->|Yes| ReturnCached[Return Cached Result]
    CheckTTL -->|No| Execute
    Execute --> StoreCache{Cache Result?}
    StoreCache -->|Yes| Store[Store in Cache]
    StoreCache -->|No| Return
    Store --> Return([Return Result])
    ReturnCached --> Return
```

## Error Handling Flow

```mermaid
flowchart TD
    Operation([Any Operation]) --> Try{Try Operation}
    Try -->|Success| Continue[Continue Flow]
    Try -->|Error| Type{Error Type?}
    
    Type -->|Validation| ValError[QueryValidationException]
    Type -->|Not Found| NotFound[QueryNotFoundException]
    Type -->|SQL Error| SQLError[QuerySQLException]
    Type -->|Timeout| Timeout[QueryTimeoutException]
    Type -->|Other| Generic[QueryException]
    
    ValError --> Log[Log Error]
    NotFound --> Log
    SQLError --> Log
    Timeout --> Log
    Generic --> Log
    
    Log --> Response{API Response?}
    Response -->|Yes| HTTPStatus[Set HTTP Status]
    Response -->|No| Throw[Throw Exception]
    
    HTTPStatus --> JSONError[Return JSON Error]
```

## Component Interactions

```mermaid
graph LR
    subgraph "Core Components"
        QD[QueryDefinition]
        QC[QueryContext]
        QR[QueryResult]
        QRow[QueryRow]
    end
    
    subgraph "Processing"
        Pre[PreProcessor]
        Row[RowProcessor]
        Post[PostProcessor]
    end
    
    subgraph "Building"
        SQL[SqlBuilder]
        Meta[MetadataBuilder]
    end
    
    QD --> QC
    QC --> SQL
    SQL --> QR
    QC --> Pre
    Pre --> QC
    QRow --> Row
    Row --> QRow
    QR --> Post
    Post --> QR
    QC --> Meta
    QR --> Meta
    Meta --> QR
```

## Performance Optimization Points

```mermaid
flowchart TD
    subgraph "Optimization Strategies"
        Query([Query Request]) --> Cache{Cached?}
        Cache -->|Hit| Fast[Return Immediately]
        Cache -->|Miss| Metadata{Metadata Cached?}
        
        Metadata -->|Yes| SkipMeta[Skip Metadata Fetch]
        Metadata -->|No| FetchMeta[Fetch Metadata]
        
        SkipMeta --> PrepStmt{Prepared Statement?}
        FetchMeta --> PrepStmt
        
        PrepStmt -->|Yes| Reuse[Reuse Statement]
        PrepStmt -->|No| Prepare[Prepare Statement]
        
        Reuse --> FetchSize{Set Fetch Size}
        Prepare --> FetchSize
        
        FetchSize --> Batch[Batch Fetching]
        Batch --> Pool{Connection Pool}
        Pool --> Execute[Execute Query]
    end
```

## Key Features

1. **Dynamic SQL Generation**: Criteria placeholders allow conditional SQL fragments
2. **Type Safety**: Generic types ensure compile-time type checking
3. **Metadata Caching**: Column metadata cached to avoid repeated introspection
4. **Processor Pipeline**: Pre/Row/Post processors for data transformation
5. **Flexible Filtering**: Runtime filters without SQL modification
6. **Oracle Optimization**: Dialect-specific pagination strategies
7. **Virtual Attributes**: Calculated fields without database columns
8. **Field Selection**: Reduce data transfer with specific field selection