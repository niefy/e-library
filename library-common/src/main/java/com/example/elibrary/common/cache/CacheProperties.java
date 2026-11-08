package com.example.elibrary.common.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Local cache tuning. The TTL bounds staleness when another API instance invalidates only its
 * own in-process cache.
 */
@ConfigurationProperties("elibrary.cache")
public record CacheProperties(
        @DefaultValue("30s") Duration ttl,
        @DefaultValue("10000") long maximumSize,
        @DefaultValue("true") boolean recordStats) {
}
