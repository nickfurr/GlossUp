package com.glossup.shared.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@ConfigurationProperties(prefix = "glossup.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int trustedProxies,
        CacheProperties cache,
        LimitProperties defaultLimit,
        Map<String, LimitProperties> endpoints
) {
    public RateLimitProperties {
        if (cache == null) {
            cache = new CacheProperties(Duration.ofMinutes(10), 50000);
        }
        if (defaultLimit == null) {
            defaultLimit = new LimitProperties(20, 20, Duration.ofMinutes(1));
        }
        if (endpoints == null) {
            endpoints = Collections.emptyMap();
        }
    }

    public record CacheProperties(
            Duration expireAfterAccess,
            long maximumSize
    ) {
        public CacheProperties {
            if (expireAfterAccess == null) {
                expireAfterAccess = Duration.ofMinutes(10);
            }
            if (maximumSize <= 0) {
                maximumSize = 50000;
            }
        }
    }

    public record LimitProperties(
            long capacity,
            long refillTokens,
            Duration refillPeriod
    ) {
        public LimitProperties {
            if (capacity <= 0) {
                capacity = 20;
            }
            if (refillTokens <= 0) {
                refillTokens = capacity;
            }
            if (refillPeriod == null || refillPeriod.isNegative() || refillPeriod.isZero()) {
                refillPeriod = Duration.ofMinutes(1);
            }
        }
    }

    public LimitProperties limiteDe(String endpoint) {
        if (endpoint != null && endpoints.containsKey(endpoint)) {
            return endpoints.get(endpoint);
        }
        return defaultLimit;
    }
}
