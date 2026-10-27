package com.glossup.compatibilidad.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZonaTest {

    @Test
    void clasificaLasCategoriasDelCatalogo() {
        for (String c : new String[]{"Champús", "Acondicionadores", "Tratamientos capilares", "Peinado", "Anticaída",
                "Coloración", "Química capilar"}) {
            assertEquals(Zona.CABELLO, Zona.deCategoria(c), c);
        }
        for (String c : new String[]{"Corporal", "Desodorantes", "Uñas", "Fragancias", "Aromaterapia"}) {
            assertEquals(Zona.CUERPO, Zona.deCategoria(c), c);
        }
        for (String c : new String[]{"Sérums", "Tónicos", "Limpiadores", "Exfoliantes", "Tratamientos", "Labios",
                "Maquillaje", "Hidratantes", "Protección solar", "Mascarillas", "Aceites"}) {
            assertEquals(Zona.ROSTRO, Zona.deCategoria(c), c);
        }
        assertEquals(Zona.ROSTRO, Zona.deCategoria(null), "sin categoría se asume rostro: más avisos, nunca menos");
    }
}
