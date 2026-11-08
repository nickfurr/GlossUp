package com.glossup.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventosSeguridadTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void agregaQuienActuoYDesdeQueIp() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr("181.50.10.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));

        assertEquals("evento=ADMIN_SUSPENDE_CUENTA usuario=9 actor=1 ip=181.50.10.7",
                EventosSeguridad.linea("ADMIN_SUSPENDE_CUENTA", "usuario", 9L));
    }

    @Test
    void unValorNoPuedeInventarOtroCampoNiOtraLinea() {
        assertEquals("evento=NO_AUTENTICADO ruta=/api/x_evento_LOGIN_OK_actor_1",
                EventosSeguridad.linea("NO_AUTENTICADO", "ruta", "/api/x\nevento=LOGIN_OK actor=1"));
        assertEquals("evento=LOGIN_FALLIDO usuario=-", EventosSeguridad.linea("LOGIN_FALLIDO", "usuario", null));
    }
}
