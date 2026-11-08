package com.example.elibrary.common.cache;

import com.example.elibrary.common.paging.PageQuery;

/**
 * Deterministic cache-key helpers. Query normalization must match the query service so that
 * equivalent searches share an entry and invalidation remains predictable.
 */
public final class CacheKeys {

    public static String bookSearch(String query, PageQuery page) {
        String normalizedQuery = query == null ? "" : query.trim();
        return normalizedQuery + '|' + page.page() + '|' + page.size();
    }

    private CacheKeys() {
    }
}
