package com.glossup.shared.config;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.security.CookieSesion;
import com.glossup.shared.security.EventosSeguridad;
import com.glossup.shared.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Configuracion de seguridad transversal (reemplaza al API Gateway en el monolito modular).
 *
 * <p>Centraliza: politica stateless, rutas publicas (auth, documentacion, health), proteccion del
 * resto de endpoints y el filtro JWT. @EnableMethodSecurity habilita @PreAuthorize por rol
 * (RNF-14).</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.glossup.shared.ratelimit.AuthRateLimitFilter authRateLimitFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          org.springframework.beans.factory.ObjectProvider<com.glossup.shared.ratelimit.AuthRateLimitFilter> authRateLimitFilterProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authRateLimitFilter = authRateLimitFilterProvider.getIfAvailable();
    }

    /**
     * Qué puede cargar el navegador (auditoría OBS-E02 / R-09). Los estilos en línea son los que Angular inyecta por
     * componente; las imágenes de producto son URLs https que escribe cada vendedor. Wompi se abre navegando, no embebido.
     */
    static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: blob: https:",
            "font-src 'self'",
            "connect-src 'self'",
            "worker-src 'self'",
            "manifest-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    /**
     * El service worker de Angular (PWA) descarga y guarda en caché las fotos de producto con fetch, y un worker obedece
     * la CSP de su propio script: sin https: en connect-src respondía 504 y el catálogo quedaba sin imágenes.
     */
    static final String CSP_SERVICE_WORKER = "default-src 'self'; connect-src 'self' https:";
    private static final RequestMatcher SERVICE_WORKER = peticion -> "/ngsw-worker.js".equals(peticion.getRequestURI());

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(h -> h
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(SERVICE_WORKER),
                                new StaticHeadersWriter("Content-Security-Policy", CSP)))
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(SERVICE_WORKER,
                                new StaticHeadersWriter("Content-Security-Policy", CSP_SERVICE_WORKER)))
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .crossOriginOpenerPolicy(c -> c.policy(CrossOriginOpenerPolicy.SAME_ORIGIN))
                        .crossOriginResourcePolicy(c -> c.policy(CrossOriginResourcePolicy.SAME_ORIGIN))
                        // credentialless y no require-corp: las fotos de producto vienen de otros dominios sin cabecera CORP.
                        .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Embedder-Policy", "credentialless"))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=()")))
                // El navegador manda la cookie de sesión solo: SameSite=Strict ya impide que otro sitio la aproveche, y el
                // token CSRF (cookie XSRF-TOKEN que Angular devuelve en X-XSRF-TOKEN) es la segunda barrera. Solo se exige
                // cuando una petición que cambia algo viaja con esa cookie; los webhooks y los clientes Bearer no la llevan.
                .csrf(csrf -> csrf.spa()
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(SecurityConfig::usaCookieDeSesion))
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                // Webhook de Wompi: lo autentica el checksum del evento, no un JWT.
                                "/api/pagos/wompi/eventos",
                                // Webhook de la transportadora: lo autentica su secreto compartido (HU14).
                                "/api/envios/eventos",
                                "/ping",
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        // El resto de la API sigue protegida; lo no-API (SPA de React servido en
                        // el mismo contenedor: index.html, assets, rutas del router) es público.
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh.authenticationEntryPoint((peticion, respuesta, error) -> {
                    EventosSeguridad.alertar("NO_AUTENTICADO", "metodo", peticion.getMethod(), "ruta", peticion.getRequestURI());
                    respuesta.sendError(HttpStatus.UNAUTHORIZED.value());
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(SecurityConfig::escribirCookieCsrf, CsrfFilter.class);
        if (authRateLimitFilter != null) {
            http.addFilterBefore(authRateLimitFilter, JwtAuthenticationFilter.class);
        }
        return http.build();
    }

    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    /** Spring difiere el token CSRF; leerlo hace que la cookie XSRF-TOKEN llegue al navegador antes del primer POST. */
    private static void escribirCookieCsrf(ServletRequest peticion, ServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        if (peticion.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
        cadena.doFilter(peticion, respuesta);
    }

    static boolean usaCookieDeSesion(HttpServletRequest peticion) {
        return !METODOS_SEGUROS.contains(peticion.getMethod()) && CookieSesion.leer(peticion) != null;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new ClaveBcrypt();
    }

    /**
     * BCrypt solo usa los primeros 72 bytes (CVE-2025-22228, auditoría R-03): una clave más larga se
     * rechaza con 422 al guardarla, en vez del 500 de Spring Security. Al iniciar sesión simplemente no coincide.
     */
    static final class ClaveBcrypt implements PasswordEncoder {
        static final int MAX_BYTES = 72;
        private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

        @Override
        public String encode(CharSequence clave) {
            if (clave != null && clave.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
                throw new ReglaNegocioException("La contraseña es demasiado larga: máximo " + MAX_BYTES
                        + " caracteres (menos si lleva tildes o ñ)");
            }
            return bcrypt.encode(clave);
        }

        @Override
        public boolean matches(CharSequence clave, String hash) {
            return clave != null && clave.toString().getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES
                    && bcrypt.matches(clave, hash);
        }
    }
}
