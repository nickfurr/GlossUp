package com.glossup.shared.config;

import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import com.glossup.shared.web.PingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cabeceras que protegen al navegador en toda respuesta, también en la SPA y en los errores (auditoría OBS-E02 / R-09). */
@WebMvcTest(controllers = PingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, CabecerasSeguridadTest.Jwt.class})
class CabecerasSeguridadTest {

    @TestConfiguration
    static class Jwt {
        @Bean
        JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void respuestaPublicaLlevaLasCabecerasDeSeguridad() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"))
                .andExpect(header().string("Cross-Origin-Embedder-Policy", "credentialless"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    void elServiceWorkerPuedeTraerFotosDeOtrosDominiosPeroLaPaginaNo() throws Exception {
        // Sin esto el worker de la PWA respondía 504 a cada foto de producto (vienen de otros dominios).
        mockMvc.perform(get("/ngsw-worker.js"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'; connect-src 'self' https:"));
        mockMvc.perform(get("/ping"))
                .andExpect(header().string("Content-Security-Policy", containsString("connect-src 'self';")));
    }

    @Test
    void elRechazoSinSesionTambienLasLleva() throws Exception {
        mockMvc.perform(get("/api/perfil/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void laPrimeraCargaEntregaElTokenCsrfQueAngularDevuelve() throws Exception {
        // Legible por JavaScript a propósito: Angular la copia en X-XSRF-TOKEN (la de sesión sí es HttpOnly).
        mockMvc.perform(get("/ping"))
                .andExpect(header().stringValues("Set-Cookie", hasItem(allOf(startsWith("XSRF-TOKEN="), not(containsString("HttpOnly"))))));
    }

    @Test
    void sobreHttpsMandaHsts() throws Exception {
        mockMvc.perform(get("/ping").secure(true))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=")));
    }
}
