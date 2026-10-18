package com.glossup.catalogo.domain.port.out;

import com.glossup.catalogo.domain.model.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y recuperación del agregado {@link Producto}.
 */
public interface ProductoRepositoryPort {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(Long id);

    List<Producto> buscarPorVendedor(Long vendedorId);

    List<Producto> listarTodos();
}
