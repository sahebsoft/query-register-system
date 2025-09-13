package com.balsam.oasis.common.registry.domain.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.Pagination;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.definition.ParamDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

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

    public QueryMetadata build() {
        return QueryMetadata.builder()
                .pagination(buildPaginationMetadata())
                .attributes(buildAttributesMetadata())
                .appliedCriteria(context.getAppliedCriteria())
                .appliedFilters(buildAppliedFilters())
                .appliedSort(buildAppliedSort())
                .parameters(buildParametersMetadata())
                .performance(buildPerformanceMetadata())
                .build();
    }

    private PaginationMetadata buildPaginationMetadata() {
        if (!context.hasPagination()) {
            return null;
        }

        Pagination pagination = context.getPagination();
        // Use the total count from context if available, otherwise use result size
        Integer totalCount = context.getTotalCount();
        int total = (totalCount != null) ? totalCount : result.getRows().size();
        int pageSize = pagination.getPageSize();
        int pageCount = (total + pageSize - 1) / pageSize;
        int currentPage = pagination.getStart() / pageSize + 1;

        // Calculate hasNext and hasPrevious based on actual total
        boolean hasNext = pagination.getEnd() < total;
        boolean hasPrevious = pagination.getStart() > 0;

        return PaginationMetadata.builder()
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

    private List<AttributeMetadata> buildAttributesMetadata() {
        List<AttributeMetadata> metadata = new ArrayList<>();
        QueryDefinitionBuilder definition = context.getDefinition();

        for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
            String attrName = entry.getKey();
            AttributeDef<?> attr = entry.getValue();

            // Skip if attribute not selected
            if (!context.isFieldSelected(attrName)) {
                continue;
            }

            // Skip attributes with selected=false unless explicitly requested
            if (!attr.selected() && (context.getSelectedFields() == null ||
                    !context.getSelectedFields().contains(attrName))) {
                continue;
            }

            // Check security restrictions
            Boolean restricted = null;
            if (attr.isSecured() && context.getSecurityContext() != null) {
                Boolean allowed = attr.securityRule().apply(context.getSecurityContext());
                if (!Boolean.TRUE.equals(allowed)) {
                    restricted = true;
                }
            }

            AttributeMetadata attrMetadata = AttributeMetadata
                    .builder()
                    .name(attrName)
                    .type(attr.type() != null ? attr.type().getSimpleName() : "Object")
                    .restricted(restricted)
                    // Include UI metadata fields
                    .label(attr.label())
                    .labelKey(attr.labelKey())
                    .width(attr.width())
                    .flex(attr.flex())
                    // Table context metadata
                    .headerText(attr.label())
                    .alignment(attr.alignment())
                    .headerStyle(attr.headerStyle())
                    .visible(attr.visible())
                    .build();

            metadata.add(attrMetadata);
        }

        return metadata;
    }

    private Map<String, FilterMetadata> buildAppliedFilters() {
        Map<String, FilterMetadata> filters = new HashMap<>();

        for (Map.Entry<String, QueryContext.Filter> entry : context.getFilters().entrySet()) {
            QueryContext.Filter filter = entry.getValue();

            FilterMetadata filterMetadata = FilterMetadata.builder()
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

    private List<SortMetadata> buildAppliedSort() {
        return context.getSorts().stream()
                .map(sort -> SortMetadata.builder()
                        .field(sort.getAttribute())
                        .direction(sort.getDirection().name())
                        .priority(context.getSorts().indexOf(sort))
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, ParameterMetadata> buildParametersMetadata() {
        Map<String, ParameterMetadata> metadata = new HashMap<>();
        QueryDefinitionBuilder definition = context.getDefinition();

        for (Map.Entry<String, ParamDef<?>> entry : definition.getParameters().entrySet()) {
            String paramName = entry.getKey();
            ParamDef<?> paramDef = entry.getValue();

            Object value = context.getParam(paramName);
            if (value == null && paramDef.hasDefaultValue()) {
                value = paramDef.defaultValue();
            }

            ParameterMetadata paramMetadata = ParameterMetadata
                    .builder()
                    .name(paramName)
                    .value(value)
                    .type(paramDef.type() != null ? paramDef.type().getSimpleName() : "Object")
                    .required(paramDef.required())
                    .build();

            metadata.put(paramName, paramMetadata);
        }

        return metadata;
    }

    private PerformanceMetadata buildPerformanceMetadata() {
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("queryName", context.getDefinition().getName());

        if (context.getCacheKey() != null) {
            additionalMetrics.put("cacheKey", context.getCacheKey());
        }

        return PerformanceMetadata.builder()
                .executionTimeMs(context.getExecutionTime())
                .rowsFetched(result.size())
                .totalRowsScanned(result.size()) // Would need actual count
                .cacheHit(false) // Would need actual cache check
                .queryPlan("INDEX_SCAN") // Would need actual query plan
                .additionalMetrics(additionalMetrics)
                .build();
    }
}
