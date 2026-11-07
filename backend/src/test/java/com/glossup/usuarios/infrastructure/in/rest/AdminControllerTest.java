package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.catalogo.domain.port.in.ModerarProductoUseCase;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AdministrarUsuariosUseCase;
import com.glossup.usuarios.domain.port.in.RecuperarClaveUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El controlador del administrador solo traduce HTTP a casos de uso (auditoría OBS-T02) y es solo para ADMIN. */
@WebMvcTest(controllers = AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, AdminControllerTest.Config.class})
class AdminControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdministrarUsuariosUseCase administrar;
    @MockitoBean private RecuperarClaveUseCase recuperar;
    @MockitoBean private ModerarProductoUseCase moderar;

    private static Usuario vendedorPendiente() {
        Usuario v = new Usuario(4L, "tienda@glossup.co", "Tienda", "hash", true, Set.of(Rol.VENDEDOR), OffsetDateTime.now());
        v.quedarPendienteDeAprobacion();
        return v;
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void unClienteNoEntra() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isForbidden());
        verifyNoInteractions(administrar);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void listaLasCuentasConSuEstadoDeAprobacion() throws Exception {
        when(administrar.listar()).thenReturn(List.of(vendedorPendiente()));
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("tienda@glossup.co"))
                .andExpect(jsonPath("$[0].pendienteAprobacion").value(true))
                .andExpect(jsonPath("$[0].roles[0]").value("VENDEDOR"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void apruebaUnVendedorYCierraSesionesConQuienActua() throws Exception {
        Usuario v = vendedorPendiente();
        v.aprobar();
        when(administrar.aprobarVendedor(1L, 4L)).thenReturn(v);
        when(administrar.cerrarSesiones(4L)).thenReturn(v);

        mockMvc.perform(post("/api/admin/usuarios/4/aprobar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAprobacion").value(false));
        mockMvc.perform(post("/api/admin/usuarios/4/cerrar-sesiones")).andExpect(status().isOk());
        verify(administrar).cerrarSesiones(4L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void creaVendedorConLosDatosValidados() throws Exception {
        when(administrar.crearVendedor("Tienda", "tienda@glossup.co", "Clave-segura-1")).thenReturn(
                new Usuario(9L, "tienda@glossup.co", "Tienda", "hash", true, Set.of(Rol.VENDEDOR), OffsetDateTime.now()));
        mockMvc.perform(post("/api/admin/vendedores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Tienda \",\"email\":\"tienda@glossup.co\",\"password\":\"Clave-segura-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
        mockMvc.perform(post("/api/admin/vendedores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Tienda\",\"email\":\"no-es-correo\",\"password\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void suspenderExigeMotivo() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios/4/suspender").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(administrar);
    }
}
