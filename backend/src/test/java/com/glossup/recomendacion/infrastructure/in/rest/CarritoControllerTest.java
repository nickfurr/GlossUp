package com.glossup.recomendacion.infrastructure.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.recomendacion.domain.model.Carrito;
import com.glossup.recomendacion.domain.port.in.GestionarCarritoUseCase;
import com.glossup.recomendacion.infrastructure.in.rest.dto.CantidadRequest;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CarritoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, CarritoControllerTest.TestSecurityConfig.class})
class CarritoControllerTest {

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
    private GestionarCarritoUseCase gestionarCarritoUseCase;

    private Carrito carritoConUnItem() {
        Carrito carrito = Carrito.vacio(7L);
        carrito.agregar(100L, Dinero.de(20000), 2);
        return carrito;
    }

    @Test
    void sinToken_retorna401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/carrito")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "7", roles = "VENDEDOR")
    void conRolVendedor_retorna403Forbidden() throws Exception {
        mockMvc.perform(get("/api/carrito")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void verCarrito_conRolCliente_retorna200() throws Exception {
        when(gestionarCarritoUseCase.verCarrito(7L)).thenReturn(carritoConUnItem());

        mockMvc.perform(get("/api/carrito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(7))
                .andExpect(jsonPath("$.items[0].productoId").value(100))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.total").value(40000));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void agregarItem_retorna201() throws Exception {
        when(gestionarCarritoUseCase.agregarItem(eq(7L), eq(100L), anyInt()))
                .thenReturn(carritoConUnItem());

        LineaCarritoRequest body = new LineaCarritoRequest(100L, 2);
        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].productoId").value(100));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void cambiarCantidad_productoNoEnCarrito_retorna422() throws Exception {
        when(gestionarCarritoUseCase.cambiarCantidad(anyLong(), anyLong(), anyInt()))
                .thenThrow(new ReglaNegocioException("El producto no está en el carrito"));

        CantidadRequest body = new CantidadRequest(3);
        mockMvc.perform(put("/api/carrito/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void agregarItem_cantidadInvalida_retorna400() throws Exception {
        LineaCarritoRequest body = new LineaCarritoRequest(100L, 0);
        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void vaciar_retorna200ConCarritoVacio() throws Exception {
        when(gestionarCarritoUseCase.vaciar(7L)).thenReturn(Carrito.vacio(7L));

        mockMvc.perform(delete("/api/carrito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }
}
