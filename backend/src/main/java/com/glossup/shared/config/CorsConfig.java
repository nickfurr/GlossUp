package com.glossup.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS centralizado para permitir el consumo desde el cliente PWA (ADR-03).
 * Los origenes permitidos se configuran por entorno (variable GLOSSUP_CORS_ORIGINS).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${glossup.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Retry-After", "X-RateLimit-Remaining")
                .allowCredentials(true);
    }
}
