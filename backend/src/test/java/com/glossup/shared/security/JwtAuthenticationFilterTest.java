package com.glossup.shared.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static <T> ObjectProvider<T> proveedorDe(T valor) {
        return new ObjectProvider<>() {
            @Override public T getObject() { return valor; }
            @Override public T getObject(Object... args) { return valor; }
            @Override public T getIfAvailable() { return valor; }
            @Override public T getIfUnique() { return valor; }
        };
    }

    private void pasarConSesion(boolean vigente) throws ServletException, IOException {
        SesionVigentePort sesiones = mock(SesionVigentePort.class);
        when(sesiones.vigente(eq("7"), any())).thenReturn(vigente);
        ObjectProvider<SesionVigentePort> proveedor = proveedorDe(sesiones);
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

    @Test
    void baseDeDatosCaida_responde503SinSeguirLaCadena() throws Exception {
        SesionVigentePort sesiones = mock(SesionVigentePort.class);
        when(sesiones.vigente(eq("7"), any())).thenThrow(new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection"));
        ObjectProvider<SesionVigentePort> proveedor = proveedorDe(sesiones);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("Authorization", "Bearer " + jwt.generarToken("7", List.of("CLIENTE")));
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        new JwtAuthenticationFilter(jwt, proveedor).doFilter(peticion, respuesta, cadena);

        assertEquals(503, respuesta.getStatus());
        assertNull(cadena.getRequest());
    }
}
