package com.glossup.recomendacion.domain.model;

/** Evento: el envío de un pedido cambió de etapa (HU14). Dispara el aviso por correo al cliente. */
public record EnvioActualizado(Long pedidoId) {}
