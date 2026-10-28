package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Comprobante;

import java.util.Optional;

/** Puerto de salida: almacenamiento de los comprobantes emitidos (HU11). */
public interface ComprobanteRepositoryPort {

    Optional<Comprobante> buscar(Long pedidoId);

    /** Guarda el comprobante si el pedido aún no tiene uno. Devuelve false si ya existía. */
    boolean guardarSiNoExiste(Comprobante comprobante);
}
