package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioCommand;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrarUsuarioServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final RegistrarUsuarioService service = new RegistrarUsuarioService(usuarios, encoder);

    @BeforeEach
    void setUp() {
        when(usuarios.buscarPorEmail(any())).thenReturn(Optional.empty());
        when(usuarios.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrar_conClaveValida_guardaUsuarioConHashSeguro() {
        RegistrarUsuarioCommand cmd = new RegistrarUsuarioCommand(
                "laura@glossup.co",
                "Laura Gómez",
                "MiClaveSegura#2026",
                "CLIENTE"
        );

        Usuario resultado = service.registrar(cmd);

        assertNotNull(resultado);
        assertEquals("laura@glossup.co", resultado.getEmail());
        assertEquals("Laura Gómez", resultado.getNombre());
        assertTrue(resultado.tieneRol(Rol.CLIENTE));
        assertTrue(encoder.matches("MiClaveSegura#2026", resultado.getPasswordHash()));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        assertEquals("laura@glossup.co", captor.getValue().getEmail());
    }

    @Test
    void registrar_conClaveSinMayuscula_fallaPorComplejidad() {
        RegistrarUsuarioCommand cmd = new RegistrarUsuarioCommand(
                "juan@glossup.co",
                "Juan Pérez",
                "miclavesegura#2026",
                "CLIENTE"
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(cmd));
        assertEquals("La contraseña debe contener al menos una letra mayúscula", ex.getMessage());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void registrar_conClaveSinCaracterEspecial_fallaPorComplejidad() {
        RegistrarUsuarioCommand cmd = new RegistrarUsuarioCommand(
                "juan@glossup.co",
                "Juan Pérez",
                "MiClaveSegura2026",
                "CLIENTE"
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(cmd));
        assertEquals("La contraseña debe contener al menos un carácter especial", ex.getMessage());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void registrar_conClaveCorta_fallaPorComplejidad() {
        RegistrarUsuarioCommand cmd = new RegistrarUsuarioCommand(
                "juan@glossup.co",
                "Juan Pérez",
                "Ab1!xyz",
                "CLIENTE"
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(cmd));
        assertEquals("La contraseña debe tener al menos 8 caracteres", ex.getMessage());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void registrar_conCorreoRepetido_fallaPorReglaNegocio() {
        Usuario existente = Usuario.registrar("repetido@glossup.co", "hash", "Existente", Rol.CLIENTE);
        when(usuarios.buscarPorEmail("repetido@glossup.co")).thenReturn(Optional.of(existente));

        RegistrarUsuarioCommand cmd = new RegistrarUsuarioCommand(
                "repetido@glossup.co",
                "Nuevo Usuario",
                "ClaveSegura#1",
                "CLIENTE"
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(cmd));
        assertEquals("Ese correo ya está registrado", ex.getMessage());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void registrar_conRolAdmin_fallaPorReglaNegocio() {
        RegistrarUsuarioCommand cmd = new RegistrarUsuarioCommand(
                "admin@glossup.co",
                "Intento Admin",
                "ClaveSegura#1",
                "ADMIN"
        );

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(cmd));
        assertEquals("Solo puedes registrarte como cliente; las cuentas de vendedor las crea un administrador", ex.getMessage());
        verify(usuarios, never()).guardar(any());
    }
}
