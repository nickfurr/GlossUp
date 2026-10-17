package com.glossup.perfil.domain.model;

import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfilDermatologicoTest {

    @Test
    void gestiona_alergias_dinamicamente_sin_duplicados() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, new Ph(5.2), TipoPiel.MIXTA, "liso");
        perfil.agregarAlergia("Parfum");
        perfil.agregarAlergia("  parfum  "); // mismo (normalizado) -> no duplica
        assertEquals(1, perfil.getAlergias().size());
        assertTrue(perfil.esAlergicoA("PARFUM"));

        perfil.quitarAlergia("parfum");
        assertFalse(perfil.esAlergicoA("parfum"));
        assertTrue(perfil.getAlergias().isEmpty());
    }

    @Test
    void gestiona_afecciones_dinamicamente_sin_duplicados() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, new Ph(5.0), TipoPiel.SENSIBLE, "ondulado");
        perfil.agregarAfeccion("Rosácea");
        perfil.agregarAfeccion("  rosácea ");
        perfil.agregarAfeccion("Acné");

        assertEquals(2, perfil.getAfecciones().size());
        assertTrue(perfil.tieneAfeccion("ROSÁCEA"));
        assertTrue(perfil.tieneAfeccion("acné"));

        perfil.quitarAfeccion("ROSÁCEA");
        assertFalse(perfil.tieneAfeccion("rosácea"));
        assertEquals(1, perfil.getAfecciones().size());
    }

    @Test
    void lanza_excepcion_al_agregar_alergia_vacia_o_nula() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, null, null, null);
        assertThrows(ReglaNegocioException.class, () -> perfil.agregarAlergia(""));
        assertThrows(ReglaNegocioException.class, () -> perfil.agregarAlergia("   "));
        assertThrows(ReglaNegocioException.class, () -> perfil.agregarAlergia(null));
    }

    @Test
    void lanza_excepcion_al_agregar_afeccion_vacia_o_nula() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, null, null, null);
        assertThrows(ReglaNegocioException.class, () -> perfil.agregarAfeccion(""));
        assertThrows(ReglaNegocioException.class, () -> perfil.agregarAfeccion("   "));
        assertThrows(ReglaNegocioException.class, () -> perfil.agregarAfeccion(null));
    }

    @Test
    void ph_fuera_de_rango_no_es_valido() {
        assertThrows(ReglaNegocioException.class, () -> new Ph(15));
        assertThrows(ReglaNegocioException.class, () -> new Ph(-0.1));
    }

    @Test
    void ph_fisiologico_se_evalua_correctamente() {
        assertTrue(new Ph(5.2).esFisiologico());
        assertFalse(new Ph(7.0).esFisiologico());
    }
}
