package com.glossup.usuarios.application.service;

import com.glossup.shared.exception.CredencialesInvalidasException;
import com.glossup.shared.exception.CuentaSuspendidaException;
import com.glossup.shared.security.JwtService;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioCommand;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticarUsuarioServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final AutenticarUsuarioService service = new AutenticarUsuarioService(usuarios, encoder,
            new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L));
    private Usuario ana;

    @BeforeEach
    void setUp() {
        ana = new Usuario(7L, "ana@glossup.co", "Ana", encoder.encode("clave-buena"), true, Set.of(Rol.CLIENTE), OffsetDateTime.now());
        when(usuarios.buscarPorEmail("ana@glossup.co")).thenReturn(Optional.of(ana));
    }

    @Test
    void cuentaSuspendida_conLaClaveCorrecta_recibeElMotivo() {
        ana.suspender("Abuso de devoluciones");
        CuentaSuspendidaException e = assertThrows(CuentaSuspendidaException.class,
                () -> service.autenticar(new AutenticarUsuarioCommand("ana@glossup.co", "clave-buena")));
        assertEquals("Abuso de devoluciones", e.getMotivo());
    }

    @Test
    void cuentaSuspendida_conClaveIncorrecta_noDelataLaSuspension() {
        ana.suspender("Abuso de devoluciones");
        assertThrows(CredencialesInvalidasException.class,
                () -> service.autenticar(new AutenticarUsuarioCommand("ana@glossup.co", "clave-mala")));
    }
}
