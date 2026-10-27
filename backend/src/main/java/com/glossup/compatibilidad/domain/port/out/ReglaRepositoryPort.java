package com.glossup.compatibilidad.domain.port.out;

import com.glossup.compatibilidad.domain.model.Beneficio;
import com.glossup.compatibilidad.domain.model.ReglaPerfil;
import com.glossup.compatibilidad.domain.model.ReglaRango;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puerto de salida: base de conocimiento del motor (reglas de irritantes y alérgenos).
 * Las tablas son pequeñas y se cruzan en memoria.
 */
public interface ReglaRepositoryPort {

    /** Todas las reglas ingrediente↔condición. */
    List<ReglaPerfil> reglasPerfil();

    /** Código de alérgeno (minúsculas) → nombres INCI que lo contienen (minúsculas). */
    Map<String, Set<String>> ingredientesPorAlergeno();

    /** Ingredientes recomendados por condición (tabla beneficio). */
    List<Beneficio> beneficios();

    /** Reglas por rango de comedogenicidad y de pH (HU7). */
    List<ReglaRango> reglasRango();

    /** Nombre INCI (minúsculas) → grado comedogénico 0–5; solo los ingredientes calificados. */
    Map<String, Integer> comedogenicidad();
}
