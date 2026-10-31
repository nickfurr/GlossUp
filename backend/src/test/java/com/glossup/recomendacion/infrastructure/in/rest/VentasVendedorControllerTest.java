package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import com.glossup.recomendacion.domain.port.in.VentasVendedorUseCase;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.exception.NotFoundException;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VentasVendedorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, VentasVendedorControllerTest.TestSecurityConfig.class})
class VentasVendedorControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockBean private VentasVendedorUseCase ventas;
    @MockBean private EnvioUseCase envios;

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void marcarEnviado_usaElVendedorDelToken() throws Exception {
        mockMvc.perform(post("/api/vendedor/ventas/9/enviado")).andExpect(status().isNoContent());
        verify(envios).marcarEnviado(5L, 9L);
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void marcarEnviado_pedidoAjeno_retorna404() throws Exception {
        doThrow(new NotFoundException("no")).when(envios).marcarEnviado(5L, 9L);
        mockMvc.perform(post("/api/vendedor/ventas/9/enviado")).andExpect(status().isNotFound());
    }

    @Test
    void marcarEnviado_sinToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/vendedor/ventas/9/enviado")).andExpect(status().isUnauthorized());
        verifyNoInteractions(envios);
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void marcarEnviado_comoCliente_retorna403() throws Exception {
        mockMvc.perform(post("/api/vendedor/ventas/9/enviado")).andExpect(status().isForbidden());
        verifyNoInteractions(envios);
    }
}
