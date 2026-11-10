package com.glossup.perfil.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoCabelloTest {

    @Test
    void verifica_todas_las_familias_y_etiquetas() {
        assertEquals("CABELLO_LISO", TipoCabello.LISO_1A.familia());
        assertEquals("CABELLO_LISO", TipoCabello.LISO_1B.familia());
        assertEquals("CABELLO_LISO", TipoCabello.LISO_1C.familia());

        assertEquals("CABELLO_ONDULADO", TipoCabello.ONDULADO_2A.familia());
        assertEquals("CABELLO_ONDULADO", TipoCabello.ONDULADO_2B.familia());
        assertEquals("CABELLO_ONDULADO", TipoCabello.ONDULADO_2C.familia());

        assertEquals("CABELLO_RIZADO", TipoCabello.RIZADO_3A.familia());
        assertEquals("CABELLO_RIZADO", TipoCabello.RIZADO_3B.familia());
        assertEquals("CABELLO_RIZADO", TipoCabello.RIZADO_3C.familia());
        assertEquals("CABELLO_RIZADO", TipoCabello.MUY_RIZADO_4A.familia());
        assertEquals("CABELLO_RIZADO", TipoCabello.MUY_RIZADO_4B.familia());
        assertEquals("CABELLO_RIZADO", TipoCabello.MUY_RIZADO_4C.familia());

        for (TipoCabello tc : TipoCabello.values()) {
            assertNotNull(tc.getEtiqueta());
            assertFalse(tc.getEtiqueta().isBlank());
        }
    }
}
