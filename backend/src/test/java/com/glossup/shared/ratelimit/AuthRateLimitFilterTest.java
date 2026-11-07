package com.glossup.shared.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter;
    private LoadingCache<String, Bucket> cache;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Capacidad 2 para login en test
        RateLimitProperties.LimitProperties loginLimit = new RateLimitProperties.LimitProperties(2, 2, Duration.ofMinutes(1));
        RateLimitProperties.LimitProperties defaultLimit = new RateLimitProperties.LimitProperties(5, 5, Duration.ofMinutes(1));

        RateLimitProperties properties = new RateLimitProperties(
                true,
                1,
                new RateLimitProperties.CacheProperties(Duration.ofMinutes(10), 1000),
                defaultLimit,
                Map.of("/api/auth/login", loginLimit)
        );

        cache = Caffeine.newBuilder()
                .build(key -> Bucket.builder()
                        .addLimit(lim -> lim.capacity(loginLimit.capacity()).refillGreedy(loginLimit.refillTokens(), loginLimit.refillPeriod()))
                        .build());

        filter = new AuthRateLimitFilter(cache, new ClientIpResolver(1), properties, objectMapper);
    }

    @Test
    void peticionesDentroDelCupo_pasan() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");

        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilter(request, response1, chain);
        assertEquals(200, response1.getStatus());

        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request, response2, chain);
        assertEquals(200, response2.getStatus());

        verify(chain).doFilter(request, response1);
        verify(chain).doFilter(request, response2);
    }

    @Test
    void superarCupo_devuelve429YNoLlamaCadena() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.2");

        // 2 permitidas
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // 3ra bloqueada
        MockHttpServletResponse response3 = new MockHttpServletResponse();
        filter.doFilter(request, response3, chain);

        assertEquals(429, response3.getStatus());
        assertNotNull(response3.getHeader("Retry-After"));
        assertEquals("0", response3.getHeader("X-RateLimit-Remaining"));

        // La cadena solo debió invocarse 2 veces, no en la 3ra
        verify(chain, times(2)).doFilter(eq(request), any());
    }

    @Test
    void ipsDistintas_tienenCubetasIndependientes() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/auth/login");
        request1.setRemoteAddr("1.1.1.1");

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/auth/login");
        request2.setRemoteAddr("2.2.2.2");

        // Agotar IP 1
        filter.doFilter(request1, new MockHttpServletResponse(), chain);
        filter.doFilter(request1, new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked1 = new MockHttpServletResponse();
        filter.doFilter(request1, blocked1, chain);
        assertEquals(429, blocked1.getStatus());

        // IP 2 sigue teniendo fichas
        MockHttpServletResponse ok2 = new MockHttpServletResponse();
        filter.doFilter(request2, ok2, chain);
        assertEquals(200, ok2.getStatus());
    }

    @Test
    void preflightCorsOptions_noConsume() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        request.setRemoteAddr("3.3.3.3");

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
        }

        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void rutasFueraDeAuth_noSeFiltran() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/productos");
        request.setRemoteAddr("4.4.4.4");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }
}
