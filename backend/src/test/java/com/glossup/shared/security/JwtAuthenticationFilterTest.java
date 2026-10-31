package com.glossup.shared.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtService jwt = new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private void pasarConSesion(boolean vigente) throws ServletException, IOException {
        SesionVigentePort sesiones = mock(SesionVigentePort.class);
        when(sesiones.vigente(eq("7"), any())).thenReturn(vigente);
        ObjectProvider<SesionVigentePort> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(sesiones);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("Authorization", "Bearer " + jwt.generarToken("7", List.of("CLIENTE")));
        new JwtAuthenticationFilter(jwt, proveedor).doFilter(peticion, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void tokenDeSesionVigente_autentica() throws Exception {
        pasarConSesion(true);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValidoDeUsuarioSuspendido_noAutentica() throws Exception {
        pasarConSesion(false);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
