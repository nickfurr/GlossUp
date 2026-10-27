package com.glossup.compatibilidad.domain.model;

import java.text.Normalizer;
import java.util.List;

/**
 * Zona donde se aplica un producto. Dos productos solo chocan (HU8) si se usan en la misma zona:
 * un sérum de retinol en la cara no choca con un acondicionador que trae ácido láctico de ajuste.
 */
public enum Zona {
    ROSTRO, CUERPO, CABELLO;

    private static final List<String> CABELLO_CLAVES = List.of("champ", "acondicion", "capilar", "cabello", "peinado",
            "anticaida", "coloracion", "tinte");
    private static final List<String> CUERPO_CLAVES = List.of("corporal", "cuerpo", "desodorante", "unas", "pies",
            "fragancia", "aromaterapia");

    // shortcut: la categoría es texto libre del vendedor; se clasifica por palabras clave y lo desconocido
    // cae en ROSTRO (más avisos, nunca menos). Pasar a categoría cerrada si los vendedores escriben otras.
    public static Zona deCategoria(String categoria) {
        if (categoria == null) return ROSTRO;
        String c = Normalizer.normalize(categoria, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        if (CABELLO_CLAVES.stream().anyMatch(c::contains)) return CABELLO;
        if (CUERPO_CLAVES.stream().anyMatch(c::contains)) return CUERPO;
        return ROSTRO;
    }
}
