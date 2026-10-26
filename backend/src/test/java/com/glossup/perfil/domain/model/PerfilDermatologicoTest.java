package com.glossup.perfil.domain.model;

import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfilDermatologicoTest {

    @Test
    void gestiona_alergias_dinamicamente_sin_duplicados() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, new Ph(5.2), TipoPiel.MIXTA, TipoCabello.LISO_1A);
        perfil.agregarAlergia(Alergeno.FRAGANCIA);
        perfil.agregarAlergia(Alergeno.FRAGANCIA); // mismo valor -> no duplica
        assertEquals(1, perfil.getAlergias().size());
        assertTrue(perfil.esAlergicoA(Alergeno.FRAGANCIA));

        perfil.quitarAlergia(Alergeno.FRAGANCIA);
        assertFalse(perfil.esAlergicoA(Alergeno.FRAGANCIA));
        assertTrue(perfil.getAlergias().isEmpty());
    }

    @Test
    void gestiona_afecciones_dinamicamente() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, new Ph(5.0), TipoPiel.SENSIBLE, TipoCabello.ONDULADO_2B);
        perfil.agregarAfeccion(Afeccion.ROSACEA);
        perfil.agregarAfeccion(Afeccion.ROSACEA);
        perfil.agregarAfeccion(Afeccion.ACNE);

        assertEquals(2, perfil.getAfecciones().size());
        assertTrue(perfil.tieneAfeccion(Afeccion.ROSACEA));

        perfil.quitarAfeccion(Afeccion.ROSACEA);
        assertFalse(perfil.tieneAfeccion(Afeccion.ROSACEA));
        assertEquals(1, perfil.getAfecciones().size());
    }

    @Test
    void alergia_nula_no_se_permite() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, null, null, null);
        assertThrows(ReglaNegocioException.class, () -> perfil.agregarAlergia(null));
    }

    @Test
    void afeccion_nula_no_se_permite() {
        PerfilDermatologico perfil = PerfilDermatologico.crear(1L, null, null, null);
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
