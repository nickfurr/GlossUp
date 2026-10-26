package com.glossup.perfil.infrastructure.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.perfil.domain.model.Afeccion;
import com.glossup.perfil.domain.model.Alergeno;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.Ph;
import com.glossup.perfil.domain.model.TipoCabello;
import com.glossup.perfil.domain.model.TipoPiel;
import com.glossup.perfil.domain.port.in.GestionarPerfilUseCase;
import com.glossup.perfil.infrastructure.in.rest.dto.AfeccionRequest;
import com.glossup.perfil.infrastructure.in.rest.dto.AlergiaRequest;
import com.glossup.shared.config.SecurityConfig;
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
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PerfilController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, PerfilControllerTest.TestSecurityConfig.class})
class PerfilControllerTest {

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
    private GestionarPerfilUseCase gestionarPerfilUseCase;

    private PerfilDermatologico crearPerfilEjemplo(Set<Afeccion> afecciones, Set<Alergeno> alergias) {
        return new PerfilDermatologico(
                10L, 7L, new Ph(5.2), TipoPiel.MIXTA, TipoCabello.ONDULADO_2A,
                afecciones, alergias, OffsetDateTime.parse("2026-10-16T17:00:00Z")
        );
    }

    @Test
    void sinToken_retorna401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/perfil/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "7", roles = "VENDEDOR")
    void conRolVendedor_retorna403Forbidden() throws Exception {
        mockMvc.perform(get("/api/perfil/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void obtenerMiPerfil_conRolCliente_retorna200Ok() throws Exception {
        PerfilDermatologico perfil = crearPerfilEjemplo(Set.of(Afeccion.ACNE), Set.of(Alergeno.FRAGANCIA));
        when(gestionarPerfilUseCase.obtenerPerfil(7L)).thenReturn(perfil);

        mockMvc.perform(get("/api/perfil/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.usuarioId").value(7))
                .andExpect(jsonPath("$.ph").value(5.2))
                .andExpect(jsonPath("$.tipoPiel").value("MIXTA"))
                .andExpect(jsonPath("$.tipoCabello").value("ONDULADO_2A"))
                .andExpect(jsonPath("$.afecciones[0]").value("ACNE"))
                .andExpect(jsonPath("$.alergias[0]").value("FRAGANCIA"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void agregarAlergia_conRequestValido_retorna200OkConListaActualizada() throws Exception {
        AlergiaRequest request = new AlergiaRequest("FRAGANCIA");
        PerfilDermatologico perfilActualizado = crearPerfilEjemplo(Set.of(), Set.of(Alergeno.FRAGANCIA));

        when(gestionarPerfilUseCase.agregarAlergia(7L, "FRAGANCIA")).thenReturn(perfilActualizado);

        mockMvc.perform(post("/api/perfil/me/alergias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alergias[0]").value("FRAGANCIA"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void quitarAlergiaPorRuta_conRolCliente_retorna200Ok() throws Exception {
        PerfilDermatologico perfilActualizado = crearPerfilEjemplo(Set.of(), Set.of());
        when(gestionarPerfilUseCase.quitarAlergia(7L, "FRAGANCIA")).thenReturn(perfilActualizado);

        mockMvc.perform(delete("/api/perfil/me/alergias/FRAGANCIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alergias.length()").value(0));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void quitarAlergiaPorBody_conRolCliente_retorna200Ok() throws Exception {
        AlergiaRequest request = new AlergiaRequest("FRAGANCIA");
        PerfilDermatologico perfilActualizado = crearPerfilEjemplo(Set.of(), Set.of());
        when(gestionarPerfilUseCase.quitarAlergia(7L, "FRAGANCIA")).thenReturn(perfilActualizado);

        mockMvc.perform(delete("/api/perfil/me/alergias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alergias.length()").value(0));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void agregarYQuitarAfeccion_conRequestValido_retorna200Ok() throws Exception {
        AfeccionRequest request = new AfeccionRequest("ROSACEA");
        PerfilDermatologico conAfeccion = crearPerfilEjemplo(Set.of(Afeccion.ROSACEA), Set.of());
        PerfilDermatologico sinAfeccion = crearPerfilEjemplo(Set.of(), Set.of());

        when(gestionarPerfilUseCase.agregarAfeccion(7L, "ROSACEA")).thenReturn(conAfeccion);
        when(gestionarPerfilUseCase.quitarAfeccion(7L, "ROSACEA")).thenReturn(sinAfeccion);

        mockMvc.perform(post("/api/perfil/me/afecciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afecciones[0]").value("ROSACEA"));

        mockMvc.perform(delete("/api/perfil/me/afecciones/ROSACEA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afecciones.length()").value(0));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void agregarAlergiaEnBlanco_retorna400BadRequest() throws Exception {
        AlergiaRequest requestInvalido = new AlergiaRequest("   ");

        mockMvc.perform(post("/api/perfil/me/alergias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Error de validacion"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void cuandoCasoDeUsoLanzaReglaNegocio_retorna422UnprocessableEntity() throws Exception {
        AfeccionRequest request = new AfeccionRequest("me pica");
        when(gestionarPerfilUseCase.agregarAfeccion(7L, "me pica"))
                .thenThrow(new ReglaNegocioException("Valor no válido para afección: me pica"));

        mockMvc.perform(post("/api/perfil/me/afecciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Valor no válido para afección: me pica"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void opciones_devuelveCatalogosCerradosConEtiquetas() throws Exception {
        mockMvc.perform(get("/api/perfil/me/opciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiposPiel.length()").value(TipoPiel.values().length))
                .andExpect(jsonPath("$.tiposCabello.length()").value(TipoCabello.values().length))
                .andExpect(jsonPath("$.alergias.length()").value(Alergeno.values().length))
                .andExpect(jsonPath("$.afecciones.length()").value(Afeccion.values().length))
                .andExpect(jsonPath("$.alergias[0].codigo").value("FRAGANCIA"))
                .andExpect(jsonPath("$.alergias[0].etiqueta").value("Fragancia / perfume"));
    }
}
