package com.glossup.compatibilidad.domain.port.out;

import com.glossup.compatibilidad.domain.model.Zona;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Puerto de salida: ficha INCI del producto que el motor necesita, sin acoplarse al agregado
 * {@code Producto} del módulo de catálogo.
 */
public interface CatalogoConsultaPort {

    /** Ficha del producto para evaluar compatibilidad; vacío si no existe. */
    Optional<ProductoConsulta> buscar(Long productoId);

    /** Varios productos de una vez, por id; los que no existen no aparecen en el mapa. */
    Map<Long, ProductoConsulta> buscarVarios(Collection<Long> productoIds);

    /** Productos que el cliente ve en el catálogo (activos y con stock), en el orden del catálogo (HU13). */
    Map<Long, ProductoConsulta> listarCatalogo();

    /** @param ingredientesInci nombres INCI de la ficha, normalizados (minúsculas). */
    record ProductoConsulta(String nombre, List<String> ingredientesInci, Double ph, Zona zona) {
        public ProductoConsulta(String nombre, List<String> ingredientesInci) {
            this(nombre, ingredientesInci, null, Zona.ROSTRO);
        }

        public ProductoConsulta(String nombre, List<String> ingredientesInci, Double ph) {
            this(nombre, ingredientesInci, ph, Zona.ROSTRO);
        }
    }
}
