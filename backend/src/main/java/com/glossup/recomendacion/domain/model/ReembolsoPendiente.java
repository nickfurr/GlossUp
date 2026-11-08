package com.glossup.recomendacion.domain.model;

/** Evento: llegó un pago aprobado para un pedido ya cancelado y hay que devolver el dinero (auditoría OBS-T13). */
public record ReembolsoPendiente(Long pedidoId) {}
