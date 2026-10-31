package com.glossup.recomendacion.domain.port.out;

import java.util.Set;

/** Puerto de salida: qué vendedores confirmaron que ya entregaron su parte de un pedido a la transportadora. */
public interface DespachoRepositoryPort {

    /** Idempotente. Serializa las confirmaciones del pedido para que dos vendedores simultáneos se vean entre sí. */
    void registrar(Long pedidoId, Long vendedorId);

    Set<Long> vendedoresQueDespacharon(Long pedidoId);

    Set<Long> pedidosDespachadosPor(Long vendedorId);
}
