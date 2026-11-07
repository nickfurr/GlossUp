package com.glossup.shared.security;

import com.glossup.shared.exception.ApiError;
import com.glossup.shared.exception.CredencialesInvalidasException;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.ratelimit.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginTarpitServiceTest {

    private LoginTarpitService tarpitService;

    @BeforeEach
    void setUp() {
        // baseDelayMs = 500, maxDelayMs = 4000, expireMinutes = 5
        tarpitService = new LoginTarpitService(500L, 4000L, 5L);
    }

    @Test
    void registrarFallo_incrementaContadorPorIp() {
        String ip = "192.168.1.50";
        assertEquals(0, tarpitService.obtenerIntentos(ip));

        assertEquals(1, tarpitService.registrarFallo(ip));
        assertEquals(2, tarpitService.registrarFallo(ip));
        assertEquals(3, tarpitService.registrarFallo(ip));

        assertEquals(3, tarpitService.obtenerIntentos(ip));
    }

    @Test
    void calcularRetardoMs_progresionLinealYTope() {
        String ip = "10.0.0.1";

        assertEquals(0L, tarpitService.calcularRetardoMs(ip));

        tarpitService.registrarFallo(ip); // 1 fallo
        assertEquals(500L, tarpitService.calcularRetardoMs(ip));

        tarpitService.registrarFallo(ip); // 2 fallos
        assertEquals(1000L, tarpitService.calcularRetardoMs(ip));

        for (int i = 3; i <= 8; i++) {
            tarpitService.registrarFallo(ip);
        }
        // 8 fallos * 500ms = 4000ms
        assertEquals(4000L, tarpitService.calcularRetardoMs(ip));

        // 9 fallos o más sigue topado a 4000ms
        tarpitService.registrarFallo(ip);
        assertEquals(4000L, tarpitService.calcularRetardoMs(ip));
    }

    @Test
    void aplicarRetardo_ejecutaPausaEfectiva() {
        // Usamos servicio con retardo corto para prueba rápida (50 ms)
        LoginTarpitService servicioRapido = new LoginTarpitService(50L, 200L, 5L);
        String ip = "172.16.0.5";
        servicioRapido.registrarFallo(ip);

        long inicio = System.currentTimeMillis();
        servicioRapido.aplicarRetardo(ip);
        long duracion = System.currentTimeMillis() - inicio;

        assertTrue(duracion >= 40, "Debe pausar al menos ~50ms, duró: " + duracion + "ms");
    }

    @Test
    void limpiarFallos_reiniciaContador() {
        String ip = "192.168.1.99";
        tarpitService.registrarFallo(ip);
        tarpitService.registrarFallo(ip);
        assertEquals(2, tarpitService.obtenerIntentos(ip));

        tarpitService.limpiarFallos(ip);
        assertEquals(0, tarpitService.obtenerIntentos(ip));
        assertEquals(0L, tarpitService.calcularRetardoMs(ip));
    }

    @Test
    void authenticationFailureListener_registraFalloEnTarpit() {
        AuthenticationFailureListener listener = new AuthenticationFailureListener(tarpitService);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@test.co", "badpass");
        auth.setDetails("203.0.113.10");

        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Bad credentials"));

        listener.onAuthenticationFailure(event);

        assertEquals(1, tarpitService.obtenerIntentos("203.0.113.10"));
        assertEquals(500L, tarpitService.calcularRetardoMs("203.0.113.10"));
    }

    @Test
    void globalExceptionHandler_conCredencialesInvalidas_emiteEventoYAplicaTarpit() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        LoginTarpitService tarpitService = new LoginTarpitService(0L, 0L, 5L);
        ClientIpResolver ipResolver = new ClientIpResolver(0);

        GlobalExceptionHandler handler = new GlobalExceptionHandler(ipResolver, publisher, tarpitService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.25");
        request.setRequestURI("/api/auth/login");

        ResponseEntity<ApiError> response =
                handler.handleInvalidCredentials(new CredencialesInvalidasException(), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciales inválidas", response.getBody().message());

        ArgumentCaptor<AuthenticationFailureBadCredentialsEvent> eventCaptor =
                ArgumentCaptor.forClass(AuthenticationFailureBadCredentialsEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertEquals("198.51.100.25", eventCaptor.getValue().getAuthentication().getDetails());
    }
}
