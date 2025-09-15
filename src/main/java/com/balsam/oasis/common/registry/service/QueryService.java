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
import com.balsam.oasis.common.registry.domain.execution.QueryExecution;
import com.balsam.oasis.common.registry.web.dto.request.QueryRequest;
import com.balsam.oasis.common.registry.domain.definition.FilterOp;

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
     * Execute a query with the provided request parameters.
     *
     * @param queryName The name of the registered query
     * @param request   The parsed query request containing parameters, filters,
     *                  etc.
     * @return QueryData containing the execution results
     * @throws QueryException if query not found or execution fails
     */
    public QueryData executeQuery(String queryName, QueryRequest request) {
        log.info("Executing query: {} with params: {}", queryName, request.getParams());

        var query = queryExecutor.execute(queryName);

        // queryExecutor.doExecute(null)

        if (request.getParams() != null) {
            query.withParams(request.getParams());
        }

        if (request.getFilters() != null) {
            query.withFilters(request.getFilters());
        }

        if (request.getSorts() != null) {
            query.withSort(request.getSorts());
        }

        if (request.getPagination() != null) {
            query.withPagination(request.getPagination().getStart(), request.getPagination().getEnd());
        }

        return query.execute();
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
     * Execute a select query with flexible options (search, IDs, or general
     * execution).
     * This is a unified method that handles all select use cases.
     *
     * @param queryName        The name of the registered select query
     * @param searchTerm       The search term to apply (optional)
     * @param ids              The IDs to fetch (optional, takes precedence over
     *                         search)
     * @param additionalParams Additional query parameters (optional)
     * @param pagination       Pagination settings (optional)
     * @return QueryData containing the execution results
     */
    public QueryData executeAsSelect(String queryName, String searchTerm, List<String> ids,
            Integer start,
            Integer end,
            Map<String, Object> additionalParams) {
        log.info("Executing select: {} with ids: {}, search: {}", queryName, ids, searchTerm);

        QueryDefinitionBuilder queryDefinition = getQueryDefinition(queryName);

        if (!queryDefinition.hasValueAttribute() || !queryDefinition.hasLabelAttribute()) {
            throw new QueryException(queryName, QueryException.ErrorCode.DEFINITION_ERROR,
                    "Query must define value and label attributes for select mode. Use asSelect() or valueAttribute()/labelAttribute() when building the query.");
        }

        QueryExecution execution = queryExecutor.execute(queryDefinition);
        // Add additional params if provided
        if (additionalParams != null) {
            execution.withParams(additionalParams);
        }

        // Apply ID filtering (takes precedence over search)
        if (ids != null && !ids.isEmpty()) {
            List<Object> idObjects = ids.stream().map(s -> (Object) s).toList();
            execution.withFilter("value", FilterOp.IN, idObjects);
        }
        // Apply search logic if no IDs provided
        else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            boolean hasSearchParam = queryDefinition.getParameters().containsKey("search");
            boolean hasSearchCriteria = queryDefinition.getCriteria().containsKey("search") ||
                    queryDefinition.getCriteria().containsKey("searchFilter");

            if (hasSearchParam || hasSearchCriteria) {
                // Use parameter approach - query has explicit search support
                execution.withParam("search", "%" + searchTerm.trim() + "%");
            } else {
                execution.withFilter("label", FilterOp.LIKE, "%" + searchTerm.trim() + "%");
            }
        }

        // Apply pagination
        if (start != null && end != null) {
            execution.withPagination(start, end);
        }

        QueryData result = execution.execute();

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