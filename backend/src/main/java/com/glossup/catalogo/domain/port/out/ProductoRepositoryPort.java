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

    /** Varios productos en un solo viaje a la BD; los ids que no existen se omiten. */
    List<Producto> buscarPorIds(Collection<Long> ids);

    List<Producto> buscarPorVendedor(Long vendedorId);

    /** Busca productos activos cuyo nombre, marca o categoria contengan la palabra clave (HU12). */
    List<Producto> buscarPorPalabraClave(String palabraClave);

    List<Producto> listarTodos();
}
