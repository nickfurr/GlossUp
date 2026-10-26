package com.glossup.perfil.infrastructure.in.rest.dto;

import com.glossup.perfil.domain.model.Afeccion;
import com.glossup.perfil.domain.model.Alergeno;
import com.glossup.perfil.domain.model.TipoCabello;
import com.glossup.perfil.domain.model.TipoPiel;

import java.util.Arrays;
import java.util.List;

/**
 * Catálogos cerrados del perfil para las listas desplegables del front. El front envía siempre
 * {@code codigo}; {@code etiqueta} y {@code detalle} son solo para mostrar.
 */
public record OpcionesPerfilResponse(
        List<Opcion> tiposPiel,
        List<Opcion> tiposCabello,
        List<Opcion> alergias,
        List<Opcion> afecciones
) {
    /** @param detalle descripción del alérgeno o zona de la afección (PIEL, CABELLO, GENERAL). */
    public record Opcion(String codigo, String etiqueta, String detalle) {}

    public static OpcionesPerfilResponse crear() {
        return new OpcionesPerfilResponse(
                Arrays.stream(TipoPiel.values())
                        .map(t -> new Opcion(t.name(), t.name().charAt(0) + t.name().substring(1).toLowerCase(), null))
                        .toList(),
                Arrays.stream(TipoCabello.values()).map(t -> new Opcion(t.name(), t.getEtiqueta(), null)).toList(),
                Arrays.stream(Alergeno.values()).map(a -> new Opcion(a.name(), a.getEtiqueta(), a.getDescripcion())).toList(),
                Arrays.stream(Afeccion.values()).map(a -> new Opcion(a.name(), a.getEtiqueta(), a.getZona().name())).toList());
    }
}
