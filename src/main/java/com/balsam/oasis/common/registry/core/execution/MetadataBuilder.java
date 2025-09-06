package com.balsam.oasis.common.registry.core.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.balsam.oasis.common.registry.core.definition.AttributeDef;
import com.balsam.oasis.common.registry.core.definition.ParamDef;
import com.balsam.oasis.common.registry.query.QueryContext;
import com.balsam.oasis.common.registry.query.QueryDefinition;
import com.balsam.oasis.common.registry.query.QueryResult;

/**
 * Builds metadata for query results
 */
public class MetadataBuilder {

    private final QueryContext context;
    private final QueryResult result;

    public MetadataBuilder(QueryContext context, QueryResult result) {
        this.context = context;
        this.result = result;
    }

    public QueryResult.QueryMetadata build() {
        return QueryResult.QueryMetadata.builder()
                .pagination(buildPaginationMetadata())
                .attributes(buildAttributesMetadata())
                .appliedCriteria(context.getAppliedCriteria())
                .appliedFilters(buildAppliedFilters())
                .appliedSort(buildAppliedSort())
                .parameters(buildParametersMetadata())
                .performance(buildPerformanceMetadata())
                .build();
    }

    private QueryResult.QueryMetadata.PaginationMetadata buildPaginationMetadata() {
        if (!context.hasPagination()) {
            return null;
        }

        QueryContext.Pagination pagination = context.getPagination();
        // Use the total count from context if available, otherwise use result size
        Integer totalCount = context.getTotalCount();
        int total = (totalCount != null) ? totalCount : result.getRows().size();
        int pageSize = pagination.getPageSize();
        int pageCount = (total + pageSize - 1) / pageSize;
        int currentPage = pagination.getStart() / pageSize + 1;

        // Calculate hasNext and hasPrevious based on actual total
        boolean hasNext = pagination.getEnd() < total;
        boolean hasPrevious = pagination.getStart() > 0;

        return QueryResult.QueryMetadata.PaginationMetadata.builder()
                .start(pagination.getStart())
                .end(pagination.getEnd())
                .total(total)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .pageSize(pageSize)
                .pageCount(pageCount)
                .currentPage(currentPage)
                .build();
    }

    private List<QueryResult.QueryMetadata.AttributeMetadata> buildAttributesMetadata() {
        List<QueryResult.QueryMetadata.AttributeMetadata> metadata = new ArrayList<>();
        QueryDefinition definition = context.getDefinition();

        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Check security restrictions
            boolean restricted = false;
            String restrictionReason = null;
            if (attr.isSecured() && context.getSecurityContext() != null) {
                Boolean allowed = attr.getSecurityRule().apply(context.getSecurityContext());
                if (!Boolean.TRUE.equals(allowed)) {
                    restricted = true;
                    restrictionReason = "INSUFFICIENT_PERMISSIONS";
                }
            }

            QueryResult.QueryMetadata.AttributeMetadata attrMetadata = QueryResult.QueryMetadata.AttributeMetadata
                    .builder()
                    .name(attrName)
                    .type(attr.getType() != null ? attr.getType().getSimpleName() : "Object")
                    .filterable(attr.isFilterable())
                    .sortable(attr.isSortable())
                    .virtual(attr.isTransient())
                    .restricted(restricted)
                    .restrictionReason(restrictionReason)
                    // Include UI metadata fields
                    .label(attr.getLabel())
                    .labelKey(attr.getLabelKey())
                    .width(attr.getWidth())
                    .flex(attr.getFlex())
                    // Table context metadata
                    .headerText(attr.getHeaderText())
                    .alignment(attr.getAlignment())
                    .displayOrder(attr.getDisplayOrder())
                    .visible(attr.isVisible())
                    // Form context metadata
                    .placeholder(attr.getPlaceholder())
                    .helpText(attr.getHelpText())
                    .inputType(attr.getInputType())
                    .required(attr.isRequired())
                    .maxLength(attr.getMaxLength())
                    .minLength(attr.getMinLength())
                    .pattern(attr.getPattern())
                    .validationMsg(attr.getValidationMsg())
                    .build();

            metadata.add(attrMetadata);
        }

        return metadata;
    }

    private Map<String, QueryResult.QueryMetadata.FilterMetadata> buildAppliedFilters() {
        Map<String, QueryResult.QueryMetadata.FilterMetadata> filters = new HashMap<>();

        for (Map.Entry<String, QueryContext.Filter> entry : context.getFilters().entrySet()) {
            QueryContext.Filter filter = entry.getValue();

            QueryResult.QueryMetadata.FilterMetadata filterMetadata = QueryResult.QueryMetadata.FilterMetadata.builder()
                    .attribute(filter.getAttribute())
                    .operator(filter.getOperator().name())
                    .value(filter.getValue())
                    .value2(filter.getValue2())
                    .values(filter.getValues())
                    .build();

            filters.put(entry.getKey(), filterMetadata);
        }

        return filters;
    }

    private List<QueryResult.QueryMetadata.SortMetadata> buildAppliedSort() {
        return context.getSorts().stream()
                .map(sort -> QueryResult.QueryMetadata.SortMetadata.builder()
                        .field(sort.getAttribute())
                        .direction(sort.getDirection().name())
                        .priority(context.getSorts().indexOf(sort))
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, QueryResult.QueryMetadata.ParameterMetadata> buildParametersMetadata() {
        Map<String, QueryResult.QueryMetadata.ParameterMetadata> metadata = new HashMap<>();
        QueryDefinition definition = context.getDefinition();

        for (Map.Entry<String, ParamDef<?>> entry : definition.getParams().entrySet()) {
            String paramName = entry.getKey();
            ParamDef<?> paramDef = entry.getValue();

            Object value = context.getParam(paramName);
            if (value == null && paramDef.hasDefaultValue()) {
                value = paramDef.getDefaultValue();
            }

            QueryResult.QueryMetadata.ParameterMetadata paramMetadata = QueryResult.QueryMetadata.ParameterMetadata
                    .builder()
                    .name(paramName)
                    .value(value)
                    .type(paramDef.getType() != null ? paramDef.getType().getSimpleName() : "Object")
                    .required(paramDef.isRequired())
                    .build();

            metadata.put(paramName, paramMetadata);
        }

        return metadata;
    }

    private QueryResult.QueryMetadata.PerformanceMetadata buildPerformanceMetadata() {
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("queryName", context.getDefinition().getName());

        if (context.getCacheKey() != null) {
            additionalMetrics.put("cacheKey", context.getCacheKey());
        }

        return QueryResult.QueryMetadata.PerformanceMetadata.builder()
                .executionTimeMs(context.getExecutionTime())
                .rowsFetched(result.size())
                .totalRowsScanned(result.size()) // Would need actual count
                .cacheHit(false) // Would need actual cache check
                .queryPlan("INDEX_SCAN") // Would need actual query plan
                .additionalMetrics(additionalMetrics)
                .build();
    }
}
