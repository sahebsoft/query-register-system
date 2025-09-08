package com.balsam.oasis.common.registry.domain.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pagination {
    private int start;
    private int end;
    private int total;
    private boolean hasNext;
    private boolean hasPrevious;

    public int getPageSize() {
        return end - start;
    }

    public int getOffset() {
        return start;
    }

    public int getLimit() {
        return getPageSize();
    }
}