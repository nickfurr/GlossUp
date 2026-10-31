package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventosEnvioController.class, properties = "glossup.envios.secreto-eventos=secreto-de-prueba")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, EventosEnvioControllerTest.TestSecurityConfig.class})
class EventosEnvioControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockBean private EnvioUseCase envios;

    private static final String EN_CAMINO = "{\"guia\":\"G-5\",\"estado\":\"EN_CAMINO\"}";

    @Test
    void conElSecreto_aplicaLaEtapaSinNecesitarToken() throws Exception {
        mockMvc.perform(post("/api/envios/eventos").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Envios-Secreto", "secreto-de-prueba").content(EN_CAMINO))
                .andExpect(status().isOk());
        verify(envios).actualizar("G-5", EstadoEnvio.EN_CAMINO);
    }

    @Test
    void sinSecretoOConOtro_retorna401YNoTocaNada() throws Exception {
        mockMvc.perform(post("/api/envios/eventos").contentType(MediaType.APPLICATION_JSON).content(EN_CAMINO))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/envios/eventos").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Envios-Secreto", "otro").content(EN_CAMINO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(envios);
    }

    @Test
    void etapaInternaODesconocida_retorna422() throws Exception {
        for (String estado : new String[]{"PREPARANDO", "VOLANDO"}) {
            mockMvc.perform(post("/api/envios/eventos").contentType(MediaType.APPLICATION_JSON)
                            .header("X-Envios-Secreto", "secreto-de-prueba")
                            .content("{\"guia\":\"G-5\",\"estado\":\"" + estado + "\"}"))
                    .andExpect(status().isUnprocessableEntity());
        }
        verifyNoInteractions(envios);
    }
}
