package com.glossup.usuarios.domain.model;

import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    private static Usuario con(Rol rol) {
        return new Usuario(5L, "Ana@Correo.CO ", "  Ana ", "hash", true, Set.of(rol), OffsetDateTime.now());
    }

    @Test
    void normalizaCorreoYNombreYExigeDatosValidos() {
        Usuario u = con(Rol.CLIENTE);
        assertEquals("ana@correo.co", u.getEmail());
        assertEquals("Ana", u.getNombre());
        assertThrows(ReglaNegocioException.class, () -> u.cambiarEmail("sin-arroba"));
        assertThrows(ReglaNegocioException.class, () -> u.cambiarNombre(" "));
        assertThrows(ReglaNegocioException.class, () -> u.cambiarPasswordHash(""));
        assertThrows(NullPointerException.class, () -> Usuario.registrar("a@b.co", "hash", "Ana", null));
    }

    @Test
    void sinRolesQuedaComoClienteYNuncaSeQuedaSinRol() {
        Usuario u = new Usuario(1L, "a@b.co", "Ana", "hash", true, Set.of(), null);
        assertTrue(u.tieneRol(Rol.CLIENTE));
        assertNotNull(u.getCreadoEn());
        assertThrows(ReglaNegocioException.class, () -> u.quitarRol(Rol.CLIENTE));
        u.asignarRol(Rol.VENDEDOR);
        u.quitarRol(Rol.CLIENTE);
        assertEquals(Set.of(Rol.VENDEDOR), u.getRoles());
    }

    @Test
    void suspenderExigeMotivoYReactivarLoBorra() {
        Usuario u = con(Rol.CLIENTE);
        assertThrows(ReglaNegocioException.class, () -> u.suspender("  "));
        u.suspender(" Fraude ");
        assertFalse(u.isActivo());
        assertEquals("Fraude", u.getMotivoSuspension());
        u.reactivar();
        assertTrue(u.isActivo());
        assertNull(u.getMotivoSuspension());
    }

    @Test
    void soloUnVendedorQuedaPendienteYCambiarleElRolLoApruebaImplicitamente() {
        assertThrows(ReglaNegocioException.class, () -> con(Rol.CLIENTE).quedarPendienteDeAprobacion());
        Usuario v = con(Rol.VENDEDOR);
        assertThrows(ReglaNegocioException.class, v::aprobar, "no hay nada pendiente");
        v.quedarPendienteDeAprobacion();
        assertTrue(v.isPendienteAprobacion());
        v.cambiarRol(Rol.CLIENTE);
        assertFalse(v.isPendienteAprobacion());
    }

    @Test
    void suprimirAnonimizaYCierraLasSesiones() {
        Usuario u = con(Rol.CLIENTE);
        OffsetDateTime ahora = OffsetDateTime.now();
        u.suprimir("otro-hash", ahora);
        assertEquals("eliminado-5@glossup.invalid", u.getEmail());
        assertEquals("Cuenta eliminada", u.getNombre());
        assertFalse(u.isActivo());
        assertEquals(ahora, u.getSesionesDesde());
        assertThrows(NullPointerException.class,
                () -> Usuario.registrar("b@c.co", "h", "B", Rol.CLIENTE).suprimir("h", ahora), "sin id no hay a quién suprimir");
    }

    @Test
    void guardaLaPruebaDeLaAutorizacion() {
        Usuario u = con(Rol.CLIENTE);
        OffsetDateTime cuando = OffsetDateTime.now();
        u.aceptarPolitica("1.0", cuando);
        assertEquals("1.0", u.getPoliticaVersion());
        assertEquals(cuando, u.getPoliticaAceptadaEn());
        assertThrows(NullPointerException.class, () -> u.aceptarPolitica(null, cuando));
    }
}
