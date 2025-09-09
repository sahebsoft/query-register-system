# JDBC Performance Optimization Guide 2025
## Modern Techniques for High-Performance Database Access

### Table of Contents
1. [Executive Summary](#executive-summary)
2. [Connection Pool Optimization](#connection-pool-optimization)
3. [Statement & Query Optimization](#statement--query-optimization)
4. [ResultSet Processing](#resultset-processing)
5. [Metadata Caching Strategies](#metadata-caching-strategies)
6. [Batch Operations](#batch-operations)
7. [Transaction Management](#transaction-management)
8. [Modern JDBC Features](#modern-jdbc-features)
9. [Database-Specific Optimizations](#database-specific-optimizations)
10. [Monitoring & Profiling](#monitoring--profiling)
11. [Real-World Case Studies](#real-world-case-studies)

---

## Executive Summary

JDBC performance optimization in 2025 focuses on:
- **Virtual Threads** (Project Loom) for massive concurrency
- **Reactive/Async patterns** with R2DBC
- **Native compilation** with GraalVM
- **Advanced connection pooling** with HikariCP
- **Smart caching** and metadata optimization
- **Database-specific tuning** for cloud databases

### Performance Impact Matrix

| Optimization | Effort | Impact | Priority |
|-------------|--------|--------|----------|
| Connection Pooling | Low | 50-200% | Critical |
| PreparedStatement Reuse | Low | 30-50% | High |
| Batch Operations | Medium | 100-500% | High |
| Metadata Caching | Medium | 20-40% | Medium |
| Fetch Size Tuning | Low | 10-30% | Medium |
| Virtual Threads | High | 200-1000% | Future |

---

## Connection Pool Optimization

### HikariCP Configuration (2025 Best Practices)

```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Connection settings
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
        config.setUsername("user");
        config.setPassword("password");
        
        // Pool sizing (Virtual Threads aware)
        if (isVirtualThreadsEnabled()) {
            config.setMaximumPoolSize(1000);  // Much higher with virtual threads
            config.setMinimumIdle(100);
        } else {
            config.setMaximumPoolSize(20);    // Traditional thread model
            config.setMinimumIdle(5);
        }
        
        // Performance settings
        config.setConnectionTimeout(30000);   // 30 seconds
        config.setIdleTimeout(600000);        // 10 minutes
        config.setMaxLifetime(1800000);       // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        // Optimization flags
        config.setAutoCommit(false);          // Manage transactions explicitly
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        // Connection test
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        return new HikariDataSource(config);
    }
}
```

### Connection Pool Metrics

```java
@Component
public class PoolMonitor {
    
    @Autowired
    private HikariDataSource dataSource;
    
    @Scheduled(fixedDelay = 60000)
    public void logPoolStats() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        
        log.info("Pool Stats - Active: {}, Idle: {}, Waiting: {}, Total: {}",
            pool.getActiveConnections(),
            pool.getIdleConnections(),
            pool.getThreadsAwaitingConnection(),
            pool.getTotalConnections());
        
        // Alert on pool exhaustion
        if (pool.getThreadsAwaitingConnection() > 0) {
            log.warn("Connection pool has {} threads waiting!", 
                pool.getThreadsAwaitingConnection());
        }
    }
}
```

---

## Statement & Query Optimization

### PreparedStatement Caching & Reuse

```java
public class OptimizedQueryExecutor {
    
    private final Map<String, PreparedStatement> statementCache = new ConcurrentHashMap<>();
    private final Connection connection;
    
    public List<User> findUsersByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM users WHERE status = ?";
        
        // Reuse PreparedStatement from cache
        PreparedStatement ps = statementCache.computeIfAbsent(sql, k -> {
            try {
                PreparedStatement stmt = connection.prepareStatement(k,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
                
                // Optimize fetch size for expected result size
                stmt.setFetchSize(100);
                
                return stmt;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        
        // Clear previous parameters
        ps.clearParameters();
        ps.setString(1, status);
        
        try (ResultSet rs = ps.executeQuery()) {
            return mapResultSet(rs);
        }
        // Note: Don't close PreparedStatement - it's cached!
    }
}
```

### Query Plan Optimization

```java
public class QueryOptimizer {
    
    // Use hints for query optimization
    private static final String OPTIMIZED_QUERY = """
        SELECT /*+ INDEX(u idx_user_status) PARALLEL(4) */
               u.id, u.name, u.email, u.status
        FROM users u
        WHERE u.status = ?
        AND u.created_date > ?
        ORDER BY u.id
        """;
    
    // Analyze query execution plan
    public void analyzeQueryPlan(String sql) throws SQLException {
        String explainSql = "EXPLAIN (ANALYZE, BUFFERS) " + sql;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {
            
            while (rs.next()) {
                log.info("Query Plan: {}", rs.getString(1));
            }
        }
    }
}
```

---

## ResultSet Processing

### Optimal ResultSet Configuration

```java
public class ResultSetOptimizer {
    
    public void processLargeResultSet(Connection conn, String query) throws SQLException {
        // Configure for large result sets
        conn.setAutoCommit(false);  // Required for fetch size to work
        
        try (PreparedStatement ps = conn.prepareStatement(query,
                ResultSet.TYPE_FORWARD_ONLY,      // Forward only for performance
                ResultSet.CONCUR_READ_ONLY,       // Read-only
                ResultSet.CLOSE_CURSORS_AT_COMMIT)) {
            
            // Critical: Set fetch size for streaming
            ps.setFetchSize(1000);  // Fetch 1000 rows at a time
            ps.setFetchDirection(ResultSet.FETCH_FORWARD);
            
            // For MySQL, use Integer.MIN_VALUE for streaming
            // ps.setFetchSize(Integer.MIN_VALUE);
            
            try (ResultSet rs = ps.executeQuery()) {
                processResultSet(rs);
            }
        } finally {
            conn.commit();
            conn.setAutoCommit(true);
        }
    }
    
    // Efficient column access by index
    private void processResultSet(ResultSet rs) throws SQLException {
        // Cache column indexes
        int idIdx = rs.findColumn("id");
        int nameIdx = rs.findColumn("name");
        int emailIdx = rs.findColumn("email");
        
        while (rs.next()) {
            // Access by index is faster than by name
            Long id = rs.getLong(idIdx);
            String name = rs.getString(nameIdx);
            String email = rs.getString(emailIdx);
            
            // Process row...
        }
    }
}
```

### Streaming Large Results

```java
public class StreamingResultProcessor {
    
    @Transactional(readOnly = true)
    public void streamResults(JdbcTemplate jdbc, String sql) {
        jdbc.setFetchSize(1000);
        
        jdbc.query(sql, rs -> {
            // Process each row as it arrives
            while (rs.next()) {
                processRow(rs);
                
                // Yield to other threads periodically
                if (rs.getRow() % 10000 == 0) {
                    Thread.yield();
                }
            }
        });
    }
    
    // Using Spring's RowCallbackHandler for memory efficiency
    public void processWithCallback(JdbcTemplate jdbc, String sql) {
        jdbc.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                // Process one row at a time - minimal memory usage
                String data = rs.getString("data");
                processData(data);
            }
        });
    }
}
```

---

## Metadata Caching Strategies

### Advanced Metadata Cache Implementation

```java
@Component
public class MetadataCacheManager {
    
    private final Map<String, TableMetadata> metadataCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @PostConstruct
    public void init() {
        // Refresh metadata cache periodically
        scheduler.scheduleAtFixedRate(this::refreshCache, 0, 1, TimeUnit.HOURS);
    }
    
    public TableMetadata getTableMetadata(Connection conn, String tableName) {
        return metadataCache.computeIfAbsent(tableName, name -> {
            try {
                return loadTableMetadata(conn, name);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private TableMetadata loadTableMetadata(Connection conn, String tableName) 
            throws SQLException {
        DatabaseMetaData dbMeta = conn.getMetaData();
        
        TableMetadata metadata = new TableMetadata();
        metadata.setTableName(tableName);
        
        // Load column information
        try (ResultSet columns = dbMeta.getColumns(null, null, tableName, null)) {
            Map<String, ColumnInfo> columnMap = new HashMap<>();
            
            while (columns.next()) {
                ColumnInfo col = new ColumnInfo();
                col.setName(columns.getString("COLUMN_NAME"));
                col.setType(columns.getInt("DATA_TYPE"));
                col.setTypeName(columns.getString("TYPE_NAME"));
                col.setSize(columns.getInt("COLUMN_SIZE"));
                col.setNullable(columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                col.setPosition(columns.getInt("ORDINAL_POSITION"));
                
                columnMap.put(col.getName().toLowerCase(), col);
            }
            
            metadata.setColumns(columnMap);
        }
        
        // Load index information
        try (ResultSet indexes = dbMeta.getIndexInfo(null, null, tableName, false, false)) {
            Map<String, IndexInfo> indexMap = new HashMap<>();
            
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null) {
                    IndexInfo index = indexMap.computeIfAbsent(indexName, IndexInfo::new);
                    index.addColumn(indexes.getString("COLUMN_NAME"));
                    index.setUnique(!indexes.getBoolean("NON_UNIQUE"));
                }
            }
            
            metadata.setIndexes(indexMap);
        }
        
        return metadata;
    }
    
    @Data
    public static class TableMetadata {
        private String tableName;
        private Map<String, ColumnInfo> columns;
        private Map<String, IndexInfo> indexes;
        private long cachedAt = System.currentTimeMillis();
        
        public boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > TimeUnit.HOURS.toMillis(1);
        }
    }
    
    @Data
    public static class ColumnInfo {
        private String name;
        private int type;
        private String typeName;
        private int size;
        private boolean nullable;
        private int position;
    }
    
    @Data
    public static class IndexInfo {
        private String name;
        private List<String> columns = new ArrayList<>();
        private boolean unique;
        
        public IndexInfo(String name) {
            this.name = name;
        }
        
        public void addColumn(String column) {
            columns.add(column);
        }
    }
}
```

---

## Batch Operations

### Optimal Batch Processing

```java
public class BatchProcessor {
    
    private static final int BATCH_SIZE = 1000;
    
    public void insertBatch(List<User> users) throws SQLException {
        String sql = "INSERT INTO users (name, email, status) VALUES (?, ?, ?)";
        
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int count = 0;
            
            for (User user : users) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getStatus());
                ps.addBatch();
                
                if (++count % BATCH_SIZE == 0) {
                    int[] results = ps.executeBatch();
                    conn.commit();
                    ps.clearBatch();
                    
                    log.info("Processed {} records", count);
                }
            }
            
            // Process remaining records
            if (count % BATCH_SIZE != 0) {
                ps.executeBatch();
                conn.commit();
            }
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }
    
    // Parallel batch processing with Virtual Threads (Java 21+)
    public void parallelBatchInsert(List<User> users) {
        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = users.size() / cores;
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < cores; i++) {
                int start = i * chunkSize;
                int end = (i == cores - 1) ? users.size() : (i + 1) * chunkSize;
                List<User> chunk = users.subList(start, end);
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        insertBatch(chunk);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
                
                futures.add(future);
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }
}
```

### Batch Update with Error Handling

```java
public class RobustBatchProcessor {
    
    public BatchResult updateBatch(List<UpdateRequest> requests) throws SQLException {
        String sql = "UPDATE users SET status = ?, updated_at = ? WHERE id = ?";
        BatchResult result = new BatchResult();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (UpdateRequest request : requests) {
                try {
                    ps.setString(1, request.getStatus());
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    ps.setLong(3, request.getId());
                    ps.addBatch();
                } catch (SQLException e) {
                    result.addError(request, e);
                }
            }
            
            try {
                int[] updateCounts = ps.executeBatch();
                conn.commit();
                
                // Process results
                for (int i = 0; i < updateCounts.length; i++) {
                    if (updateCounts[i] == Statement.SUCCESS_NO_INFO || 
                        updateCounts[i] > 0) {
                        result.addSuccess(requests.get(i));
                    } else {
                        result.addFailure(requests.get(i));
                    }
                }
            } catch (BatchUpdateException e) {
                conn.rollback();
                processBatchException(e, requests, result);
            }
        }
        
        return result;
    }
    
    private void processBatchException(BatchUpdateException e, 
                                      List<UpdateRequest> requests, 
                                      BatchResult result) {
        int[] updateCounts = e.getUpdateCounts();
        
        for (int i = 0; i < updateCounts.length; i++) {
            if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                result.addError(requests.get(i), e);
            } else if (updateCounts[i] > 0) {
                result.addSuccess(requests.get(i));
            }
        }
    }
}
```

---

## Transaction Management

### Optimized Transaction Handling

```java
@Component
public class TransactionOptimizer {
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    // Manual transaction management for fine control
    public void performComplexTransaction(List<Operation> operations) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        
        // Configure transaction properties
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        template.setTimeout(30); // 30 seconds timeout
        
        template.execute(status -> {
            try {
                for (Operation op : operations) {
                    op.execute();
                    
                    // Create savepoint after critical operations
                    if (op.isCritical()) {
                        Object savepoint = status.createSavepoint();
                        
                        try {
                            op.validate();
                        } catch (ValidationException e) {
                            status.rollbackToSavepoint(savepoint);
                            throw e;
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException(e);
            }
        });
    }
    
    // Read-only transaction optimization
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public List<Data> readLargeDataset() {
        // Read-only transactions can use different connection pool
        // and have better performance characteristics
        return jdbcTemplate.query(
            "SELECT * FROM large_table",
            (rs, rowNum) -> mapRow(rs)
        );
    }
}
```

---

## Modern JDBC Features

### Virtual Threads (Java 21+)

```java
@Configuration
public class VirtualThreadConfig {
    
    @Bean
    public DataSource virtualThreadDataSource() {
        HikariConfig config = new HikariConfig();
        // ... standard configuration ...
        
        // Virtual threads can handle many more connections
        config.setMaximumPoolSize(1000);
        config.setMinimumIdle(100);
        
        // Use virtual thread executor
        config.setScheduledExecutor(Executors.newScheduledThreadPool(1, Thread.ofVirtual()
            .name("hikari-scheduler-", 0)
            .factory()));
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

### Reactive Database Access (R2DBC)

```java
@Repository
public class ReactiveUserRepository {
    
    @Autowired
    private DatabaseClient databaseClient;
    
    public Flux<User> findAllUsers() {
        return databaseClient
            .sql("SELECT * FROM users")
            .map((row, metadata) -> User.builder()
                .id(row.get("id", Long.class))
                .name(row.get("name", String.class))
                .email(row.get("email", String.class))
                .build())
            .all();
    }
    
    public Mono<User> findById(Long id) {
        return databaseClient
            .sql("SELECT * FROM users WHERE id = :id")
            .bind("id", id)
            .map(this::mapRow)
            .one();
    }
    
    // Reactive batch insert
    public Flux<Integer> batchInsert(Flux<User> users) {
        return users
            .buffer(1000) // Batch size
            .flatMap(batch -> {
                String sql = "INSERT INTO users (name, email) VALUES ($1, $2)";
                
                return databaseClient.inConnection(conn -> {
                    Statement statement = conn.createStatement(sql);
                    
                    for (User user : batch) {
                        statement.bind(0, user.getName())
                                .bind(1, user.getEmail())
                                .add(); // Add to batch
                    }
                    
                    return Flux.from(statement.execute())
                        .flatMap(result -> Mono.from(result.getRowsUpdated()));
                });
            });
    }
}
```

---

## Database-Specific Optimizations

### PostgreSQL Optimizations

```java
public class PostgreSQLOptimizer {
    
    // Use COPY for bulk insert (10x faster than INSERT)
    public void bulkCopy(List<User> users) throws SQLException, IOException {
        CopyManager copyManager = new CopyManager((BaseConnection) connection);
        
        String copyQuery = "COPY users (name, email, status) FROM STDIN WITH CSV";
        
        StringWriter writer = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(writer);
        
        for (User user : users) {
            csvWriter.writeNext(new String[]{
                user.getName(),
                user.getEmail(),
                user.getStatus()
            });
        }
        
        copyManager.copyIn(copyQuery, new StringReader(writer.toString()));
    }
    
    // Use arrays for IN clause optimization
    public List<User> findUsersById(List<Long> ids) throws SQLException {
        Array array = connection.createArrayOf("bigint", ids.toArray());
        
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM users WHERE id = ANY(?)")) {
            ps.setArray(1, array);
            
            try (ResultSet rs = ps.executeQuery()) {
                return mapResults(rs);
            }
        } finally {
            array.free();
        }
    }
}
```

### MySQL Optimizations

```java
public class MySQLOptimizer {
    
    // MySQL-specific batch insert with ON DUPLICATE KEY UPDATE
    public void upsertBatch(List<User> users) throws SQLException {
        String sql = """
            INSERT INTO users (id, name, email, status, updated_at)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                email = VALUES(email),
                status = VALUES(status),
                updated_at = VALUES(updated_at)
            """;
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (User user : users) {
                ps.setLong(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getStatus());
                ps.setTimestamp(5, Timestamp.from(Instant.now()));
                ps.addBatch();
            }
            
            ps.executeBatch();
        }
    }
    
    // Use LOAD DATA INFILE for ultra-fast bulk loading
    public void loadDataInfile(String csvPath) throws SQLException {
        String sql = """
            LOAD DATA LOCAL INFILE ?
            INTO TABLE users
            FIELDS TERMINATED BY ','
            LINES TERMINATED BY '\n'
            IGNORE 1 ROWS
            (name, email, status)
            """;
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, csvPath);
            ps.execute();
        }
    }
}
```

### Oracle Optimizations

```java
public class OracleOptimizer {
    
    // Use Oracle's array processing for batch operations
    public void oracleBatchInsert(List<User> users) throws SQLException {
        OracleConnection oraConn = connection.unwrap(OracleConnection.class);
        
        String sql = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
        
        try (OraclePreparedStatement ps = 
                (OraclePreparedStatement) oraConn.prepareStatement(sql)) {
            
            // Set execution batch size
            ps.setExecuteBatch(1000);
            
            for (User user : users) {
                ps.setLong(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getEmail());
                ps.executeUpdate(); // Automatically batched
            }
            
            ps.sendBatch(); // Send remaining batch
        }
    }
    
    // Use Oracle's RETURNING clause for generated keys
    public List<Long> insertWithReturning(List<User> users) throws SQLException {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?) RETURNING id INTO ?";
        
        List<Long> generatedIds = new ArrayList<>();
        
        try (CallableStatement cs = connection.prepareCall(sql)) {
            for (User user : users) {
                cs.setString(1, user.getName());
                cs.setString(2, user.getEmail());
                cs.registerOutParameter(3, Types.BIGINT);
                cs.execute();
                
                generatedIds.add(cs.getLong(3));
            }
        }
        
        return generatedIds;
    }
}
```

---

## Monitoring & Profiling

### JDBC Performance Monitoring

```java
@Component
public class JdbcPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    public JdbcPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public <T> T monitorQuery(String queryName, Supplier<T> queryExecution) {
        return Timer.Sample.start(meterRegistry)
            .stop(meterRegistry.timer("jdbc.query.duration", "query", queryName))
            .recordCallable(() -> {
                try {
                    T result = queryExecution.get();
                    meterRegistry.counter("jdbc.query.success", "query", queryName).increment();
                    return result;
                } catch (Exception e) {
                    meterRegistry.counter("jdbc.query.error", "query", queryName).increment();
                    throw e;
                }
            });
    }
    
    // Aspect for automatic monitoring
    @Aspect
    @Component
    public static class JdbcMonitoringAspect {
        
        @Autowired
        private MeterRegistry meterRegistry;
        
        @Around("@annotation(Monitored)")
        public Object monitorMethod(ProceedingJoinPoint joinPoint) throws Throwable {
            String methodName = joinPoint.getSignature().getName();
            Timer.Sample sample = Timer.Sample.start(meterRegistry);
            
            try {
                Object result = joinPoint.proceed();
                sample.stop(meterRegistry.timer("jdbc.method.duration", "method", methodName));
                return result;
            } catch (Exception e) {
                meterRegistry.counter("jdbc.method.error", "method", methodName).increment();
                throw e;
            }
        }
    }
}
```

### Query Slow Log

```java
@Component
public class QuerySlowLog {
    
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    
    @EventListener
    public void handleSlowQuery(SlowQueryEvent event) {
        if (event.getDuration() > SLOW_QUERY_THRESHOLD_MS) {
            log.warn("Slow Query Detected:\n" +
                    "SQL: {}\n" +
                    "Duration: {} ms\n" +
                    "Parameters: {}\n" +
                    "Stack Trace: {}",
                    event.getSql(),
                    event.getDuration(),
                    event.getParameters(),
                    getRelevantStackTrace());
            
            // Send alert
            alertingService.sendAlert(AlertLevel.WARNING, 
                "Slow query detected: " + event.getDuration() + "ms");
        }
    }
    
    private String getRelevantStackTrace() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
            .filter(element -> element.getClassName().startsWith("com.mycompany"))
            .limit(10)
            .map(StackTraceElement::toString)
            .collect(Collectors.joining("\n"));
    }
}
```

---

## Real-World Case Studies

### Case Study 1: E-commerce Platform Optimization

**Problem**: Order processing taking 5+ seconds for bulk orders

**Solution**:
```java
// Before: Sequential processing
public void processOrders(List<Order> orders) {
    for (Order order : orders) {
        insertOrder(order);
        updateInventory(order);
        sendNotification(order);
    }
}

// After: Optimized batch processing
public void processOrdersOptimized(List<Order> orders) {
    // Batch insert orders
    batchInsertOrders(orders);
    
    // Bulk update inventory
    Map<Long, Integer> inventoryUpdates = orders.stream()
        .flatMap(o -> o.getItems().stream())
        .collect(Collectors.groupingBy(
            Item::getProductId,
            Collectors.summingInt(Item::getQuantity)
        ));
    bulkUpdateInventory(inventoryUpdates);
    
    // Async notifications
    CompletableFuture.runAsync(() -> 
        orders.parallelStream().forEach(this::sendNotification)
    );
}

// Result: 5 seconds → 0.3 seconds (94% improvement)
```

### Case Study 2: Analytics Dashboard

**Problem**: Dashboard queries timing out with large datasets

**Solution**:
```java
// Before: Real-time aggregation
public DashboardData getDashboard() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*), SUM(amount), AVG(amount) FROM transactions WHERE date > ?",
        new Object[]{thirtyDaysAgo},
        (rs, rowNum) -> new DashboardData(/*...*/)
    );
}

// After: Materialized views + caching
@Cacheable(value = "dashboard", key = "#userId")
public DashboardData getDashboardOptimized(Long userId) {
    // Use materialized view refreshed every hour
    return jdbcTemplate.queryForObject(
        "SELECT * FROM dashboard_summary WHERE user_id = ?",
        new Object[]{userId},
        (rs, rowNum) -> new DashboardData(/*...*/)
    );
}

// Result: 15 seconds → 50ms (99.7% improvement)
```

### Case Study 3: Migration Tool

**Problem**: Data migration taking 48 hours for 100M records

**Solution**:
```java
// After: Parallel processing with Virtual Threads
public void migrateDataOptimized() {
    long totalRecords = countSourceRecords();
    int batchSize = 10000;
    int parallelism = 50; // With virtual threads
    
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (long offset = 0; offset < totalRecords; offset += batchSize) {
            long currentOffset = offset;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                List<Record> batch = fetchBatch(currentOffset, batchSize);
                transformAndLoadBatch(batch);
            }, executor);
            
            futures.add(future);
            
            // Control parallelism
            if (futures.size() >= parallelism) {
                CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();
                futures.removeIf(CompletableFuture::isDone);
            }
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}

// Result: 48 hours → 2 hours (96% improvement)
```

---

## Performance Checklist

### Pre-Development
- [ ] Choose appropriate connection pool (HikariCP recommended)
- [ ] Design schema with indexes for common queries
- [ ] Plan batch operations for bulk data
- [ ] Consider caching strategy

### Development
- [ ] Use PreparedStatement and cache them
- [ ] Set appropriate fetch size
- [ ] Use batch operations for bulk inserts/updates
- [ ] Access ResultSet columns by index
- [ ] Implement connection pooling
- [ ] Use appropriate transaction isolation levels
- [ ] Enable statement caching in driver

### Testing
- [ ] Load test with production-like data volumes
- [ ] Monitor connection pool metrics
- [ ] Profile slow queries
- [ ] Test failover scenarios
- [ ] Verify batch operation performance

### Production
- [ ] Monitor query execution times
- [ ] Set up alerts for slow queries
- [ ] Regular index maintenance
- [ ] Database statistics updates
- [ ] Connection pool tuning based on load

---

## Conclusion

JDBC performance optimization in 2025 requires a holistic approach combining:
- Modern Java features (Virtual Threads, Records)
- Database-specific optimizations
- Intelligent caching strategies
- Reactive programming where appropriate
- Comprehensive monitoring

The key is to measure, optimize, and continuously monitor. Start with connection pooling and PreparedStatement caching for quick wins, then move to more advanced optimizations based on your specific use cases.

## Resources

- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)
- [R2DBC Specification](https://r2dbc.io/)
- [Virtual Threads Guide](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [PostgreSQL JDBC Documentation](https://jdbc.postgresql.org/documentation/)
- [MySQL Connector/J Guide](https://dev.mysql.com/doc/connector-j/en/)
- [Oracle JDBC Best Practices](https://www.oracle.com/database/technologies/develop/jdbc.html)