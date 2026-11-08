package com.example.elibrary.catalog.application;

import com.example.elibrary.common.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cache-aside invalidation happens after the write transaction commits, not before. Evicting
 * earlier would leave a window where a concurrent reader repopulates the cache with the old
 * availability and the just-committed change remains invisible until the TTL expires.
 */
@Component
public class BookCacheInvalidator {

    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.BOOK_BY_ID, key = "#event.bookId"),
        @CacheEvict(cacheNames = CacheNames.BOOK_SEARCH, allEntries = true)
    })
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAvailabilityChanged(BookAvailabilityChangedEvent event) {
        // The cache annotations perform the eviction; this method intentionally has no body.
    }
}
