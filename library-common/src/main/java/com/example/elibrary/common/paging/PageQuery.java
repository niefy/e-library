package com.example.elibrary.common.paging;

/**
 * Transport-neutral pagination request. The upper bound is enforced here as well as at the API
 * boundary so application code cannot accidentally request an unbounded page.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
