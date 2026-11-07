package com.glossup.shared.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.glossup.shared.exception.ApiError;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Filtro perimetral de Rate Limiting sobre /api/auth/**.
 * No se anota con @Component para evitar el autoregistro duplicado en la cadena servlet.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final Set<String> ENDPOINTS_CONOCIDOS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/recuperar",
            "/api/auth/restablecer"
    );

    private final LoadingCache<String, Bucket> buckets;
    private final ClientIpResolver ipResolver;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(LoadingCache<String, Bucket> buckets,
                               ClientIpResolver ipResolver,
                               RateLimitProperties properties,
                               ObjectMapper objectMapper) {
        this.buckets = buckets;
        this.ipResolver = ipResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true; // CORS preflight no consume tokens
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/auth");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String endpointNormalizado = normalizarEndpoint(request.getRequestURI());
        String ip = ipResolver.resolver(request);
        String clave = ip + "|" + endpointNormalizado;

        Bucket bucket = buckets.get(clave);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long nanosEspera = probe.getNanosToWaitForRefill();
        long segundos = Math.max(1, (long) Math.ceil(nanosEspera / 1_000_000_000.0));

        log.warn("Rate limit excedido para IP [{}] en endpoint [{}]. Espera requerida: {}s",
                ip, endpointNormalizado, segundos);

        response.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiError error = new ApiError(
                OffsetDateTime.now(),
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Demasiados intentos. Intenta de nuevo en " + segundos + " s.",
                request.getRequestURI(),
                List.of()
        );

        objectMapper.writeValue(response.getOutputStream(), error);
    }

    public static String normalizarEndpoint(String uri) {
        if (uri == null) {
            return "*";
        }
        // Quita trailing slash si existe para igualar
        String limpia = uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
        if (ENDPOINTS_CONOCIDOS.contains(limpia)) {
            return limpia;
        }
        return "*";
    }
}
