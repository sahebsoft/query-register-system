package com.balsam.oasis.common.registry.domain.definition;

import java.time.Duration;
import java.util.function.Function;

import lombok.Builder;
import lombok.Value;

/**
 * Simple cache configuration for query results
 */
@Value
@Builder(toBuilder = true)
public class CacheConfig {
    @Builder.Default
    boolean enabled = false;

    @Builder.Default
    Duration ttl = Duration.ofMinutes(5);

    Function<Object, String> keyGenerator;

    public boolean hasKeyGenerator() {
        return keyGenerator != null;
    }

    public boolean isEnabled() {
        return enabled;
    }
}