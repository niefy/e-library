package com.example.elibrary.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Annotation-driven cache infrastructure shared by all Java modules.
 *
 * <p>Caffeine is intentionally in-process: it adds no network hop or extra stateful service to
 * the local topology. Cross-instance freshness is bounded by the configured TTL; correctness is
 * still enforced by the database and never depends on a cache hit.
 */
@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CaffeineCacheManager cacheManager(CacheProperties properties) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(properties.ttl())
                .maximumSize(properties.maximumSize());
        if (properties.recordStats()) {
            builder.recordStats();
        }

        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(CacheNames.BOOK_BY_ID, CacheNames.BOOK_SEARCH));
        manager.setCaffeine(builder);
        return manager;
    }
}
