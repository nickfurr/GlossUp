package com.glossup.recomendacion.domain.model;

import java.time.OffsetDateTime;

/**
 * Comprobante de compra de un pedido pagado (HU11): el PDF se emite una sola vez y se conserva tal
 * cual, como soporte del pago aunque luego cambien los productos o los datos del cliente.
 */
public record Comprobante(Long pedidoId, String numero, byte[] pdf, OffsetDateTime emitidoEn) {

    /** Un comprobante por pedido: el número se deriva del pedido y no se repite. */
    public static String numeroDe(Long pedidoId) {
        return String.format("GU-%07d", pedidoId);
    }
}
