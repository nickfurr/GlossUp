package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Pedido;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /** Pedidos pagados antes de {@code limite} cuyo envío la transportadora aún no acepta, con menos de {@code maxIntentos}. */
    List<Pedido> buscarEnviosSinNotificar(OffsetDateTime limite, int maxIntentos);

    /** Pedidos con el paquete en manos de la transportadora, sin etapa final. */
    List<Pedido> buscarEnviosEnCurso();

    Optional<Pedido> buscarPorGuia(String guia);

    /** Todos los pedidos, del más reciente al más antiguo. */
    List<Pedido> listarTodos();

    /** Pedidos pagados (o ya enviados) que contienen alguno de estos productos, del más reciente al más antiguo. */
    List<Pedido> buscarPagadosConProductos(Set<Long> productoIds);
}
