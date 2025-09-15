package com.balsam.oasis.common.registry.domain.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pagination {
    private Integer start;
    private Integer end;
    private Integer total;
    private boolean hasNext;
    private boolean hasPrevious;

    public Integer getPageSize() {
        return end - start;
    }

    public Integer getOffset() {
        return start;
    }

    public Integer getLimit() {
        return getPageSize();
    }
}