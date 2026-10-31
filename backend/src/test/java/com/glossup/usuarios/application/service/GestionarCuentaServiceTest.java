package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GestionarCuentaServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final GestionarCuentaService service = new GestionarCuentaService(usuarios, encoder);
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.registrar("vende@glossup.co", encoder.encode("clave-actual"), "Vende", Rol.VENDEDOR);
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario));
    }

    @Test
    void cambiarClave_conLaActualCorrecta_guardaLaNuevaCifrada() {
        service.cambiarClave(1L, "clave-actual", "clave-nueva-1");
        assertTrue(encoder.matches("clave-nueva-1", usuario.getPasswordHash()));
        verify(usuarios).guardar(usuario);
    }

    @Test
    void cambiarClave_conLaActualIncorrecta_noCambiaNada() {
        String antes = usuario.getPasswordHash();
        assertThrows(ReglaNegocioException.class, () -> service.cambiarClave(1L, "otra", "clave-nueva-1"));
        assertEquals(antes, usuario.getPasswordHash());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void cambiarNombre_recortaYGuarda() {
        service.cambiarNombre(1L, "  Aurora Cosmetics ");
        assertEquals("Aurora Cosmetics", usuario.getNombre());
        verify(usuarios).guardar(usuario);
    }
}
