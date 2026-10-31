package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.MetodoEnvio;

import java.util.List;

/**
 * Comando de entrada para consolidar un carrito y reservar inventario (HU9).
 *
 * <p>El carrito es enviado por el cliente (no se persiste como tal); cada línea referencia un
 * producto y la cantidad deseada. Las líneas repetidas del mismo producto se consolidan sumando
 * sus cantidades en la capa de aplicación. La dirección y la forma de envío (HU14) definen el costo
 * de envío que se suma al total.</p>
 */
public record ConsolidarCarritoCommand(Long usuarioId, List<Linea> lineas, Long direccionId, MetodoEnvio metodoEnvio) {

    public record Linea(Long productoId, int cantidad) {}
}
