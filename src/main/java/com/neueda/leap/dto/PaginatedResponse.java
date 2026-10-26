package com.neueda.leap.dto;

import java.util.List;

/**
 * Pagination metadata wrapper for list responses
 */
public class PaginatedResponse<T> {
    private List<T> items;
    private PaginationMeta pagination;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> items, int limit, int offset, int totalCount) {
        this.items = items;
        this.pagination = new PaginationMeta(limit, offset, totalCount);
    }

    // Getters and Setters
    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public PaginationMeta getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMeta pagination) {
        this.pagination = pagination;
    }

    // Inner class for pagination metadata
    public static class PaginationMeta {
        private int limit;
        private int offset;
        private int totalCount;
        private boolean hasMore;

        public PaginationMeta() {
        }

        public PaginationMeta(int limit, int offset, int totalCount) {
            this.limit = limit;
            this.offset = offset;
            this.totalCount = totalCount;
            this.hasMore = (offset + limit) < totalCount;
        }

        // Getters and Setters
        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public boolean isHasMore() {
            return hasMore;
        }

        public void setHasMore(boolean hasMore) {
            this.hasMore = hasMore;
        }
    }
}
