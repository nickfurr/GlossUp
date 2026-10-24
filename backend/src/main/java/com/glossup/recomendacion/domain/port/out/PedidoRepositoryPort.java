package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Pedido;

import java.util.Optional;

/**
 * Puerto de salida: persistencia y recuperación del agregado {@link Pedido}.
 */
public interface PedidoRepositoryPort {

    Pedido guardar(Pedido pedido);

    Optional<Pedido> buscarPorId(Long id);
}
