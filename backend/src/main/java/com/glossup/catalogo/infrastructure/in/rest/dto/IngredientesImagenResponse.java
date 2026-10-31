package com.glossup.catalogo.infrastructure.in.rest.dto;

import java.util.List;

/**
 * Ingredientes leídos de la etiqueta. {@code enCatalogo} son los que se pueden agregar a la ficha;
 * los demás hay que revisarlos, porque la ficha solo admite ingredientes del catálogo.
 */
public record IngredientesImagenResponse(List<String> ingredientes, List<Reconocido> enCatalogo) {

    public record Reconocido(Long id, String inci, String descripcion) {}
}

