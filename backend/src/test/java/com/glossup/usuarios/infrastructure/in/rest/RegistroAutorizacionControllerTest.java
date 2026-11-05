package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioUseCase;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El API rechaza el registro sin autorización de datos ni mayoría de edad, aunque el frontend no lo valide. */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, RegistroAutorizacionControllerTest.TestSecurityConfig.class})
class RegistroAutorizacionControllerTest {

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
    private AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    @MockBean
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    private static String cuerpo(String extra) {
        return "{\"email\":\"ana@correo.co\",\"nombre\":\"Ana\",\"password\":\"clave-segura-1\",\"rol\":\"CLIENTE\"" + extra + "}";
    }

    @Test
    void sinAutorizacionNiMayoriaDeEdad_responde400SinRegistrar() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo("")))
                .andExpect(status().isBadRequest());
        verify(registrarUsuarioUseCase, never()).registrar(any(RegistrarUsuarioCommand.class));
    }

    @Test
    void conAutorizacionYMayoriaDeEdad_registra() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(",\"aceptaPolitica\":true,\"mayorDeEdad\":true")))
                .andExpect(status().isCreated());
        verify(registrarUsuarioUseCase).registrar(any(RegistrarUsuarioCommand.class));
    }
}
