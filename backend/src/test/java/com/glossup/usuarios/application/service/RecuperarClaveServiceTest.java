package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.out.CorreoRecuperacionPort;
import com.glossup.usuarios.domain.port.out.RecuperacionClaveRepositoryPort;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecuperarClaveServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final RecuperacionClaveRepositoryPort enlaces = mock(RecuperacionClaveRepositoryPort.class);
    private final CorreoRecuperacionPort correo = mock(CorreoRecuperacionPort.class);
    private final RecuperarClaveService service =
            new RecuperarClaveService(usuarios, enlaces, correo, encoder, "https://glossup.co/");
    private Usuario ana;

    @BeforeEach
    void setUp() {
        ana = new Usuario(7L, "ana@glossup.co", "Ana", encoder.encode("clave-vieja"), true, Set.of(Rol.CLIENTE), OffsetDateTime.now());
        when(usuarios.buscarPorEmail("ana@glossup.co")).thenReturn(Optional.of(ana));
        when(usuarios.buscarPorId(7L)).thenReturn(Optional.of(ana));
    }

    @Test
    void solicitar_correoDesconocido_noHaceNadaNiFalla() {
        when(usuarios.buscarPorEmail("nadie@glossup.co")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.solicitar("nadie@glossup.co"));
        verifyNoInteractions(correo, enlaces);
    }

    @Test
    void solicitar_envíaEnlaceConElTokenYGuardaSoloSuHash() {
        service.solicitar(" ANA@glossup.co ");

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo).enviarEnlace(eq("ana@glossup.co"), eq("Ana"), enlace.capture());
        assertTrue(enlace.getValue().startsWith("https://glossup.co/restablecer?token="));
        String token = enlace.getValue().substring(enlace.getValue().indexOf('=') + 1);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(enlaces).guardar(hash.capture(), eq(7L), any());
        assertEquals(64, hash.getValue().length());
        assertNotEquals(token, hash.getValue());
    }

    @Test
    void solicitar_repetidoEnPocoTiempo_noVuelveAEnviar() {
        when(enlaces.hayRecienteDe(eq(7L), any())).thenReturn(true);
        service.solicitar("ana@glossup.co");
        verifyNoInteractions(correo);
    }

    @Test
    void solicitar_cuentaSuspendida_noEnvia() {
        ana.suspender("x");
        service.solicitar("ana@glossup.co");
        verifyNoInteractions(correo);
    }

    @Test
    void generarEnlace_cuentaSuspendida_falla() {
        ana.suspender("x");
        assertThrows(ReglaNegocioException.class, () -> service.generarEnlace(7L));
    }

    @Test
    void restablecer_cambiaLaClaveYCierraLasSesiones() {
        when(enlaces.consumir(anyString())).thenReturn(Optional.of(7L));
        service.restablecer("token", "clave-nueva-1");
        assertTrue(encoder.matches("clave-nueva-1", ana.getPasswordHash()));
        assertNotNull(ana.getSesionesDesde());
        verify(usuarios).guardar(ana);
    }

    @Test
    void restablecer_enlaceInvalidoOUsado_falla() {
        when(enlaces.consumir(anyString())).thenReturn(Optional.empty());
        assertThrows(ReglaNegocioException.class, () -> service.restablecer("token", "clave-nueva-1"));
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void restablecer_cuentaSuspendida_falla() {
        ana.suspender("x");
        when(enlaces.consumir(anyString())).thenReturn(Optional.of(7L));
        assertThrows(ReglaNegocioException.class, () -> service.restablecer("token", "clave-nueva-1"));
        assertTrue(encoder.matches("clave-vieja", ana.getPasswordHash()));
    }
}
