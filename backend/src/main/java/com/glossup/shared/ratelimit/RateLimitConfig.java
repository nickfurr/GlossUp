package com.glossup.shared.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bucket;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public LoadingCache<String, Bucket> rateLimitBuckets(RateLimitProperties props) {
        return Caffeine.newBuilder()
                .expireAfterAccess(props.cache().expireAfterAccess())
                .maximumSize(props.cache().maximumSize())
                .build(clave -> {
                    String endpoint = endpointDeClave(clave);
                    RateLimitProperties.LimitProperties lim = props.limiteDe(endpoint);
                    return Bucket.builder()
                            .addLimit(limit -> limit.capacity(lim.capacity())
                                    .refillGreedy(lim.refillTokens(), lim.refillPeriod()))
                            .build();
                });
    }

    @Bean
    public AuthRateLimitFilter authRateLimitFilter(LoadingCache<String, Bucket> buckets,
                                                  RateLimitProperties props,
                                                  org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules());
        return new AuthRateLimitFilter(buckets, new ClientIpResolver(props.trustedProxies()), props, mapper);
    }

    private static String endpointDeClave(String clave) {
        if (clave == null) {
            return "*";
        }
        int idx = clave.indexOf('|');
        if (idx >= 0 && idx + 1 < clave.length()) {
            return clave.substring(idx + 1);
        }
        return "*";
    }
}
