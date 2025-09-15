package com.balsam.oasis.common.registry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.engine.query.QueryExecutorImpl;
import com.balsam.oasis.common.registry.engine.query.QueryRegistryImpl;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import com.balsam.oasis.common.registry.domain.common.QueryData;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.common.collect.ImmutableList;

/**
 * Shared service for executing queries and selects.
 * Provides common business logic for both QueryController and SelectController.
 */
@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private final QueryExecutorImpl queryExecutor;
    private final QueryRegistryImpl queryRegistry;

    public QueryService(QueryExecutorImpl queryExecutor, QueryRegistryImpl queryRegistry) {
        this.queryExecutor = queryExecutor;
        this.queryRegistry = queryRegistry;
    }

    /**
     * Execute a query with the provided QueryContext.
     *
     * @param queryContext The QueryContext containing all execution parameters
     * @return QueryData containing the execution results
     * @throws QueryException if query not found or execution fails
     */
    public QueryData executeQuery(QueryContext queryContext) {
        log.info("Executing query: {} with params: {}",
                queryContext.getDefinition().getName(), queryContext.getParams());

        QueryData result = queryExecutor.doExecute(queryContext);

        // Handle select mode transformation if needed
        if (isSelectMode(queryContext)) {
            result = transformForSelect(result, queryContext);
        }

        return result;
    }

    /**
     * Get a registered query definition.
     *
     * @param queryName The name of the query
     * @return QueryDefinitionBuilder or null if not found
     */
    public QueryDefinitionBuilder getQueryDefinition(String queryName) {
        QueryDefinitionBuilder queryDefinition = queryRegistry.get(queryName);
        if (queryDefinition == null) {
            throw new QueryException(queryName, QueryException.ErrorCode.QUERY_NOT_FOUND,
                    "Query not found: " + queryName);
        }

        return queryDefinition;
    }

    public QueryRow executeSingle(String queryName, Map<String, Object> params) {
        return queryExecutor.execute(queryName).withParams(params).executeSingle();
    }

    /**
     * Check if the QueryContext indicates select mode.
     */
    private boolean isSelectMode(QueryContext queryContext) {
        return queryContext.getParams() != null &&
                Boolean.TRUE.equals(queryContext.getParams().get("_selectMode"));
    }

    /**
     * Transform query results for select mode by ensuring value and label fields are present.
     */
    private QueryData transformForSelect(QueryData result, QueryContext queryContext) {
        QueryDefinitionBuilder queryDefinition = queryContext.getDefinition();

        List<QueryRow> transformedRows = new ArrayList<>();
        for (QueryRow row : result.getRows()) {
            Map<String, Object> rowData = new HashMap<>(row.toMap());
            rowData.put("value", rowData.get(queryDefinition.getValueAttribute()));
            rowData.put("label", rowData.get(queryDefinition.getLabelAttribute()));
            transformedRows.add(QueryRow.create(rowData, result.getContext()));
        }

        return QueryData.builder()
                .rows(ImmutableList.copyOf(transformedRows))
                .context(result.getContext())
                .metadata(result.getMetadata())
                .build();
    }

}