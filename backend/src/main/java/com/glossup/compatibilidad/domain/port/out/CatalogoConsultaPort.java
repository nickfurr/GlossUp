package com.glossup.compatibilidad.domain.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: ficha INCI del producto que el motor necesita, sin acoplarse al agregado
 * {@code Producto} del módulo de catálogo.
 */
public interface CatalogoConsultaPort {

    /** Ficha del producto para evaluar compatibilidad; vacío si no existe. */
    Optional<ProductoConsulta> buscar(Long productoId);

    /** @param ingredientesInci nombres INCI de la ficha, normalizados (minúsculas). */
    record ProductoConsulta(String nombre, List<String> ingredientesInci, Double ph) {
        public ProductoConsulta(String nombre, List<String> ingredientesInci) {
            this(nombre, ingredientesInci, null);
        }
    }
}
