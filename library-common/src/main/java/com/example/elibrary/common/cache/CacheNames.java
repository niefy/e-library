package com.example.elibrary.common.cache;

/**
 * Logical cache names shared by application services and cache invalidators. Keeping the names
 * here prevents a typo in one annotation from silently creating a second, never-evicted cache.
 */
public final class CacheNames {

    public static final String BOOK_BY_ID = "bookById";
    public static final String BOOK_SEARCH = "bookSearch";

    private CacheNames() {
    }
}
