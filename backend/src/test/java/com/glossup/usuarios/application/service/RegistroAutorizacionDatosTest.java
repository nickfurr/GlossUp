package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioCommand;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ley 1581: el registro exige y guarda la autorización del titular (auditoría NC-T11, OBS-T11). */
class RegistroAutorizacionDatosTest {

    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final RegistrarUsuarioService service = new RegistrarUsuarioService(usuarios, new BCryptPasswordEncoder(4));

    private RegistrarUsuarioCommand comando(boolean aceptaPolitica, boolean mayorDeEdad) {
        return new RegistrarUsuarioCommand("ana@correo.co", "Ana", "ClaveSegura#1", "CLIENTE", aceptaPolitica, mayorDeEdad);
    }

    @Test
    void conAutorizacion_guardaFechaYVersionDeLaPolitica() {
        when(usuarios.buscarPorEmail("ana@correo.co")).thenReturn(Optional.empty());
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario registrado = service.registrar(comando(true, true));

        assertEquals(RegistrarUsuarioService.POLITICA_VIGENTE, registrado.getPoliticaVersion());
        assertNotNull(registrado.getPoliticaAceptadaEn());
    }

    @Test
    void vendedorQueSeRegistraSolo_quedaPendienteYElClienteNo() {
        when(usuarios.buscarPorEmail(any())).thenReturn(Optional.empty());
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario vendedor = service.registrar(new RegistrarUsuarioCommand("tienda@correo.co", "Tienda", "Clave-segura-1",
                "VENDEDOR", true, true));
        assertTrue(vendedor.isPendienteAprobacion());
        assertFalse(service.registrar(comando(true, true)).isPendienteAprobacion());
    }

    @Test
    void sinAceptarLaPolitica_noCreaLaCuenta() {
        assertThrows(ReglaNegocioException.class, () -> service.registrar(comando(false, true)));
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void sinDeclararMayoriaDeEdad_noCreaLaCuenta() {
        assertThrows(ReglaNegocioException.class, () -> service.registrar(comando(true, false)));
        verify(usuarios, never()).guardar(any());
    }
}
