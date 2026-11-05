package com.glossup.catalogo.domain.port.out;

import com.glossup.catalogo.domain.model.Producto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y recuperación del agregado {@link Producto}.
 */
public interface ProductoRepositoryPort {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(Long id);

    /**
     * Suma {@code delta} (negativo para descontar) al stock de forma atómica frente a otras transacciones.
     *
     * @return false si el producto no existe o el stock quedaría negativo; en ese caso no cambia nada
     */
    boolean ajustarStock(Long id, int delta);

    /** Varios productos en un solo viaje a la BD; los ids que no existen se omiten. */
    List<Producto> buscarPorIds(Collection<Long> ids);

    List<Producto> buscarPorVendedor(Long vendedorId);

    /** Busca productos activos cuyo nombre, marca o categoria contengan la palabra clave (HU12). */
    List<Producto> buscarPorPalabraClave(String palabraClave);

    List<Producto> listarTodos();

    /** Una página del catálogo publicado, por id; {@code categoria} null = todas. */
    PaginaCatalogo catalogo(String categoria, int pagina, int tamano);

    /** Id y categoría de cada producto del catálogo publicado, por id. */
    List<EntradaCatalogo> indiceCatalogo();

    record PaginaCatalogo(List<Producto> productos, long total) {}

    record EntradaCatalogo(Long id, String categoria) {}
}
