package com.balsam.oasis.common.registry.domain.definition;

import java.time.Duration;
import java.util.function.Function;

import lombok.Builder;
import lombok.Value;

/**
 * Cache configuration for query results
 */
@Value
@Builder(toBuilder = true)
public class CacheConfig {
    @Builder.Default
    boolean enabled = false;

    @Builder.Default
    Duration ttl = Duration.ofMinutes(5);

    @Builder.Default
    Duration refreshAfter = Duration.ofMinutes(3);

    @Builder.Default
    int maxEntries = 1000;

    Function<Object, String> keyGenerator;

    @Builder.Default
    boolean cacheNullValues = false;

    @Builder.Default
    boolean cacheEmptyResults = false;

    String cacheName;

    public boolean hasKeyGenerator() {
        return keyGenerator != null;
    }
}