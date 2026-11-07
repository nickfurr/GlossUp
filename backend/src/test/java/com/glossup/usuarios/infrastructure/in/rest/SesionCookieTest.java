package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.CookieSesion;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AutenticacionResultado;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioUseCase;
import com.glossup.usuarios.domain.port.in.GestionarCuentaUseCase;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La sesión vive en una cookie HttpOnly y no en localStorage (auditoría OBS-T09 / R-01). */
@WebMvcTest(controllers = {AuthController.class, CuentaController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, SesionCookieTest.Config.class})
class SesionCookieTest {

    private static final JwtService JWT = new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);

    @TestConfiguration
    static class Config {
        @Bean
        JwtService jwtService() {
            return JWT;
        }

        @Bean
        CookieSesion cookieSesion() {
            return new CookieSesion(true, 3600000L);
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AutenticarUsuarioUseCase autenticar;
    @MockitoBean private RegistrarUsuarioUseCase registrar;
    @MockitoBean private GestionarCuentaUseCase cuenta;

    private Cookie sesionDe(String usuarioId) {
        return new Cookie(CookieSesion.NOMBRE, JWT.generarToken(usuarioId, List.of("CLIENTE")));
    }

    @Test
    void login_abreLaSesionEnUnaCookieHttpOnlyYNoDevuelveElToken() throws Exception {
        Usuario ana = new Usuario(7L, "ana@glossup.co", "Ana", "hash", true, Set.of(Rol.CLIENTE), OffsetDateTime.now());
        when(autenticar.autenticar(any())).thenReturn(new AutenticacionResultado("token-firmado", ana));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@glossup.co\",\"password\":\"Clave-segura-1\"}"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItem(allOf(startsWith("glossup_sesion=token-firmado"),
                        containsString("HttpOnly"), containsString("Secure"), containsString("SameSite=Strict"),
                        containsString("Path=/api")))))
                .andExpect(jsonPath("$.usuarioId").value(7))
                .andExpect(content().string(not(containsString("token-firmado"))));
    }

    @Test
    void laCookieAutenticaSinEncabezadoAuthorization() throws Exception {
        mockMvc.perform(put("/api/cuenta/nombre").cookie(sesionDe("7")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana María\"}"))
                .andExpect(status().isNoContent());
        verify(cuenta).cambiarNombre(7L, "Ana María");
    }

    @Test
    void conLaCookiePeroSinTokenCsrf_seRechaza() throws Exception {
        mockMvc.perform(put("/api/cuenta/nombre").cookie(sesionDe("7"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana María\"}"))
                .andExpect(status().isForbidden());
    }


    @Test
    void logout_borraLaCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(sesionDe("7")).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie", hasItem(allOf(startsWith("glossup_sesion=;"), containsString("Max-Age=0")))));
    }

    @Test
    void cambiarClave_cierraLasDemasSesionesYRenuevaEsta() throws Exception {
        mockMvc.perform(put("/api/cuenta/clave").cookie(sesionDe("7")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"clave-vieja-1\",\"nueva\":\"Clave-nueva-1\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie", hasItem(allOf(startsWith("glossup_sesion=ey"), containsString("HttpOnly")))));
        verify(cuenta).cambiarClave(7L, "clave-vieja-1", "Clave-nueva-1");
    }

    @Test
    void cerrarSesionesEnTodosLosDispositivos_revocaYBorraLaCookie() throws Exception {
        mockMvc.perform(post("/api/cuenta/cerrar-sesiones").cookie(sesionDe("7")).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie", hasItem(allOf(startsWith("glossup_sesion=;"), containsString("Max-Age=0")))));
        verify(cuenta).cerrarSesiones(7L);
    }

    @Test
    void webhookSinCookieNoPideTokenCsrf() throws Exception {
        // Sin cookie de sesión la regla CSRF no aplica: llega al controlador (aquí no existe la ruta, 404 y no 403).
        mockMvc.perform(post("/api/auth/no-existe").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}
