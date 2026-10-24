package com.glossup.recomendacion.infrastructure.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoUseCase;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.infrastructure.in.rest.dto.CarritoRequest;
import com.glossup.recomendacion.infrastructure.in.rest.dto.LineaCarritoRequest;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PedidoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, PedidoControllerTest.TestSecurityConfig.class})
class PedidoControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsolidarCarritoUseCase consolidarCarritoUseCase;

    @MockBean
    private PedidoRepositoryPort pedidoRepository;

    private Pedido pedidoEjemplo() {
        return new Pedido(55L, 7L, EstadoPedido.CREADO,
                List.of(new PedidoItem(100L, 2, Dinero.de(30000))),
                OffsetDateTime.parse("2026-10-24T10:00:00Z"));
    }

    @Test
    void sinToken_retorna401Unauthorized() throws Exception {
        CarritoRequest body = new CarritoRequest(List.of(new LineaCarritoRequest(100L, 2)));
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "7", roles = "VENDEDOR")
    void conRolVendedor_retorna403Forbidden() throws Exception {
        CarritoRequest body = new CarritoRequest(List.of(new LineaCarritoRequest(100L, 2)));
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void consolidar_conRolCliente_retorna201ConPedido() throws Exception {
        when(consolidarCarritoUseCase.consolidar(any())).thenReturn(pedidoEjemplo());

        CarritoRequest body = new CarritoRequest(List.of(new LineaCarritoRequest(100L, 2)));
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.usuarioId").value(7))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.items[0].productoId").value(100))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.total").value(60000));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void consolidar_carritoVacio_retorna400() throws Exception {
        CarritoRequest body = new CarritoRequest(List.of());
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void consolidar_sinStock_retorna422() throws Exception {
        when(consolidarCarritoUseCase.consolidar(any()))
                .thenThrow(new ReglaNegocioException("Stock insuficiente"));

        CarritoRequest body = new CarritoRequest(List.of(new LineaCarritoRequest(100L, 99)));
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }
}
