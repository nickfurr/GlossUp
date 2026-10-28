package com.glossup.recomendacion.domain.model;

/** Evento: la pasarela confirmó el pago de un pedido (HU10). Dispara la emisión del comprobante (HU11). */
public record PedidoPagado(Long pedidoId) {}
