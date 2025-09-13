package com.balsam.oasis.common.registry.domain.metadata;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.Pagination;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.definition.AttributeDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class QueryMetadata {
    PaginationInfo pagination;
    List<AttributeInfo> attributes;
    Long executionTimeMs;

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {
        int start;
        int end;
        int total;
        Boolean hasNext;
        Boolean hasPrevious;
        int pageSize;
        int pageCount;
        int currentPage;
    }

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AttributeInfo {
        String name;
        String type;
        Boolean restricted;
        String label;
        String labelKey;
        String width;
        String flex;
        String headerText;
        String headerStyle;
        String alignment;
        Boolean visible;
    }

    public static class MetadataBuilder {
        private final QueryContext context;
        private final QueryResult result;

        public MetadataBuilder(QueryContext context, QueryResult result) {
            this.context = context;
            this.result = result;
        }

        public QueryMetadata build() {
            return QueryMetadata.builder()
                    .pagination(buildPaginationInfo())
                    .attributes(buildAttributesInfo())
                    .executionTimeMs(context.getExecutionTime())
                    .build();
        }

        private PaginationInfo buildPaginationInfo() {
            if (!context.hasPagination()) {
                return null;
            }

            Pagination pagination = context.getPagination();
            Integer totalCount = context.getTotalCount();
            int total = (totalCount != null) ? totalCount : result.getRows().size();
            int pageSize = pagination.getPageSize();
            int pageCount = (total + pageSize - 1) / pageSize;
            int currentPage = pagination.getStart() / pageSize + 1;

            boolean hasNext = pagination.getEnd() < total;
            boolean hasPrevious = pagination.getStart() > 0;

            return PaginationInfo.builder()
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

        private List<AttributeInfo> buildAttributesInfo() {
            List<AttributeInfo> metadata = new ArrayList<>();
            QueryDefinitionBuilder definition = context.getDefinition();

            for (Map.Entry<String, AttributeDef<?>> entry : definition.getAttributes().entrySet()) {
                String attrName = entry.getKey();
                AttributeDef<?> attr = entry.getValue();

                if (!context.isFieldSelected(attrName)) {
                    continue;
                }

                if (!attr.selected() && (context.getSelectedFields() == null ||
                        !context.getSelectedFields().contains(attrName))) {
                    continue;
                }

                Boolean restricted = null;
                if (attr.isSecured() && context.getSecurityContext() != null) {
                    Boolean allowed = attr.securityRule().apply(context.getSecurityContext());
                    if (!Boolean.TRUE.equals(allowed)) {
                        restricted = true;
                    }
                }

                AttributeInfo attrInfo = AttributeInfo
                        .builder()
                        .name(attrName)
                        .type(attr.type() != null ? attr.type().getSimpleName() : "Object")
                        .restricted(restricted)
                        .label(attr.label())
                        .labelKey(attr.labelKey())
                        .width(attr.width())
                        .flex(attr.flex())
                        .headerText(attr.label())
                        .alignment(attr.alignment())
                        .headerStyle(attr.headerStyle())
                        .visible(attr.visible())
                        .build();

                metadata.add(attrInfo);
            }

            return metadata;
        }
    }
}