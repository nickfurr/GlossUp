package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Pedido;

/**
 * Puerto de salida: API de envíos (HU14). Debe ser idempotente por pedido: un reintento del mismo
 * pedido devuelve la misma guía. Lanza una excepción si la transportadora no acepta la solicitud.
 */
public interface TransportadoraPort {

    Aceptacion solicitarEnvio(Pedido pedido);

    record Aceptacion(String transportadora, String guia) {}
}
