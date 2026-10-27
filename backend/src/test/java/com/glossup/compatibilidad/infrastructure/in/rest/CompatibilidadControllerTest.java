package com.glossup.compatibilidad.infrastructure.in.rest;

import com.glossup.compatibilidad.domain.model.Alerta;
import com.glossup.compatibilidad.domain.model.Cruce;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;
import com.glossup.compatibilidad.domain.model.Severidad;
import com.glossup.compatibilidad.domain.port.in.EvaluarCompatibilidadUseCase;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompatibilidadController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, CompatibilidadControllerTest.TestSecurityConfig.class})
class CompatibilidadControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluarCompatibilidadUseCase evaluarCompatibilidad;

    @Test
    void sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/compatibilidad/productos").param("ids", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "7", roles = "VENDEDOR")
    void vendedor_retorna403() throws Exception {
        mockMvc.perform(get("/api/compatibilidad/productos").param("ids", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void lote_devuelveUnResultadoPorProductoConSusAlertas() throws Exception {
        Map<Long, ResultadoCompatibilidad> resultado = new LinkedHashMap<>();
        resultado.put(1L, ResultadoCompatibilidad.vacio());
        resultado.put(2L, new ResultadoCompatibilidad(List.of(
                new Alerta(Severidad.ALTA, "Contiene retinol", "Retinoides en embarazo"))));
        when(evaluarCompatibilidad.evaluarProductosPerfil(7L, List.of(1L, 2L))).thenReturn(resultado);

        mockMvc.perform(get("/api/compatibilidad/productos").param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productoId").value(1))
                .andExpect(jsonPath("$[0].compatible").value(true))
                .andExpect(jsonPath("$[0].recomendado").value(false))
                .andExpect(jsonPath("$[1].compatible").value(false))
                .andExpect(jsonPath("$[1].alertas[0].severidad").value("ALTA"))
                .andExpect(jsonPath("$[1].alertas[0].causa").value("Retinoides en embarazo"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void cruces_devuelveElParConSuCausa() throws Exception {
        when(evaluarCompatibilidad.evaluarCruces(List.of(1L, 2L))).thenReturn(List.of(
                new Cruce(1L, 2L, new Alerta(Severidad.MEDIA, "No juntos", "Suman irritación"))));

        mockMvc.perform(get("/api/compatibilidad/cruces").param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoA").value(1))
                .andExpect(jsonPath("$[0].productoB").value(2))
                .andExpect(jsonPath("$[0].severidad").value("MEDIA"))
                .andExpect(jsonPath("$[0].causa").value("Suman irritación"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void loteDemasiadoGrande_seRechaza() throws Exception {
        String ids = LongStream.rangeClosed(1, 201).mapToObj(String::valueOf).collect(Collectors.joining(","));
        mockMvc.perform(get("/api/compatibilidad/productos").param("ids", ids))
                .andExpect(status().isUnprocessableEntity());
    }
}
