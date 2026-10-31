package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.out.ModeracionCatalogoPort;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrarUsuariosServiceTest {

    private static final Long ADMIN = 1L;
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final ModeracionCatalogoPort catalogo = mock(ModeracionCatalogoPort.class);
    private final AdministrarUsuariosService service = new AdministrarUsuariosService(usuarios, catalogo);

    private Usuario conRol(long id, Rol rol) {
        Usuario u = new Usuario(id, "u" + id + "@glossup.co", "Usuario", "hash", true, Set.of(rol), OffsetDateTime.now());
        when(usuarios.buscarPorId(id)).thenReturn(Optional.of(u));
        return u;
    }

    @BeforeEach
    void setUp() {
        when(usuarios.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void noPuedeSuspenderseNiCambiarseElRolASiMismo() {
        conRol(1L, Rol.ADMIN);
        assertThrows(ReglaNegocioException.class, () -> service.suspender(ADMIN, 1L, "x"));
        assertThrows(ReglaNegocioException.class, () -> service.cambiarRol(ADMIN, 1L, "CLIENTE"));
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void suspenderVendedor_bloqueaLaCuentaYSacaSusProductos() {
        Usuario v = conRol(2L, Rol.VENDEDOR);
        service.suspender(ADMIN, 2L, "Fraude");
        assertFalse(v.isActivo());
        assertEquals("Fraude", v.getMotivoSuspension());
        verify(catalogo).suspenderVendedor(2L);
    }

    @Test
    void suspenderCliente_noTocaElCatalogo() {
        conRol(3L, Rol.CLIENTE);
        service.suspender(ADMIN, 3L, "Abuso");
        verifyNoInteractions(catalogo);
    }

    @Test
    void suspenderSinMotivo_falla() {
        Usuario c = conRol(3L, Rol.CLIENTE);
        assertThrows(ReglaNegocioException.class, () -> service.suspender(ADMIN, 3L, "  "));
        assertTrue(c.isActivo());
    }

    @Test
    void reactivarVendedor_devuelveSusProductos() {
        Usuario v = conRol(2L, Rol.VENDEDOR);
        v.suspender("Fraude");
        service.reactivar(2L);
        assertTrue(v.isActivo());
        assertNull(v.getMotivoSuspension());
        verify(catalogo).reactivarVendedor(2L);
    }

    @Test
    void cambiarRol_cierraLasSesionesPorqueElTokenGuardaLosRoles() {
        Usuario c = conRol(3L, Rol.CLIENTE);
        service.cambiarRol(ADMIN, 3L, "VENDEDOR");
        assertEquals(Set.of(Rol.VENDEDOR), c.getRoles());
        assertNotNull(c.getSesionesDesde());
    }

    @Test
    void cambiarRol_invalido_falla() {
        conRol(3L, Rol.CLIENTE);
        assertThrows(ReglaNegocioException.class, () -> service.cambiarRol(ADMIN, 3L, "DIOS"));
    }

    @Test
    void vendedorConProductosPublicados_noPierdeElRol() {
        Usuario v = conRol(2L, Rol.VENDEDOR);
        when(catalogo.tienePublicados(2L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.cambiarRol(ADMIN, 2L, "CLIENTE"));
        assertEquals(Set.of(Rol.VENDEDOR), v.getRoles());
    }

    @Test
    void actualizarDatos_correoDeOtraCuenta_falla() {
        Usuario u = conRol(3L, Rol.CLIENTE);
        Usuario otro = conRol(4L, Rol.CLIENTE);
        when(usuarios.buscarPorEmail("otro@glossup.co")).thenReturn(Optional.of(otro));
        assertThrows(ReglaNegocioException.class, () -> service.actualizarDatos(3L, "Ana", "OTRO@glossup.co "));
        assertEquals("u3@glossup.co", u.getEmail());
    }

    @Test
    void actualizarDatos_conservandoSuCorreo_funciona() {
        Usuario u = conRol(3L, Rol.CLIENTE);
        when(usuarios.buscarPorEmail("u3@glossup.co")).thenReturn(Optional.of(u));
        assertEquals("Ana Gómez", service.actualizarDatos(3L, " Ana Gómez ", "u3@glossup.co").getNombre());
    }
}
