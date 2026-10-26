package com.glossup.compatibilidad.domain.port.out;

import java.util.List;

/**
 * Puerto de salida: ficha INCI del producto que el motor necesita, sin acoplarse al agregado
 * {@code Producto} del módulo de catálogo.
 */
public interface CatalogoConsultaPort {

    /**
     * Ficha del producto para evaluar compatibilidad.
     *
     * @throws com.glossup.shared.exception.NotFoundException si el producto no existe
     */
    ProductoConsulta obtener(Long productoId);

    /** @param ingredientesInci nombres INCI de la ficha, normalizados (minúsculas). */
    record ProductoConsulta(String nombre, List<String> ingredientesInci) {}
}
