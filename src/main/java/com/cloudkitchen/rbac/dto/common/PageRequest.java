package com.cloudkitchen.rbac.dto.common;

/**
 * Standard pagination request parameters
 */
public class PageRequest {
    private int page = 0;
    private int size = 20;
    private String sortBy;
    private String sortDirection = "asc"; // asc or desc

    public PageRequest() {
    }

    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public PageRequest(int page, int size, String sortBy, String sortDirection) {
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(1, size), 100); // Limit between 1 and 100
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        if (sortDirection != null && (sortDirection.equalsIgnoreCase("asc") || sortDirection.equalsIgnoreCase("desc"))) {
            this.sortDirection = sortDirection.toLowerCase();
        }
    }

    public int getOffset() {
        return page * size;
    }
}

