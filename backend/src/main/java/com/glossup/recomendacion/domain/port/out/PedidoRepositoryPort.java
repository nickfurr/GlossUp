package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Pedido;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y recuperación del agregado {@link Pedido}.
 */
public interface PedidoRepositoryPort {

    Pedido guardar(Pedido pedido);

    Optional<Pedido> buscarPorId(Long id);

    /** Pedidos de un cliente, del más reciente al más antiguo. */
    List<Pedido> buscarPorUsuario(Long usuarioId);

    /** Pedidos todavía CREADO (sin pagar) creados antes de {@code limite}: candidatos a vencer. */
    List<Pedido> buscarSinPagarCreadosAntesDe(OffsetDateTime limite);
}
