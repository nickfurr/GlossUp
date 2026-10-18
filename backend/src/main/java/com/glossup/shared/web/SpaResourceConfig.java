package com.glossup.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve el frontend (build de React, empaquetado en {@code classpath:/static} al construir la
 * imagen) y hace el fallback de rutas SPA: una ruta de React Router (p. ej. /cliente) que no
 * corresponde a un archivo real devuelve index.html, para que funcionen los enlaces directos.
 *
 * <p>No afecta a la API: los controladores de /api, /ping y Swagger se resuelven antes que este
 * handler de recursos; las rutas que empiezan por "api/" se dejan pasar (null) para que respondan
 * ellos. En desarrollo (sin build de front) simplemente no hay index.html y no pasa nada.</p>
 */
@Configuration
public class SpaResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource recurso = location.createRelative(path);
                        if (recurso.exists() && recurso.isReadable()) {
                            return recurso; // archivo estático real (js, css, sw.js, manifest…)
                        }
                        if (path.startsWith("api/")) {
                            return null; // deja que respondan los controladores REST
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null; // SPA fallback (si hay front empaquetado)
                    }
                });
    }
}
