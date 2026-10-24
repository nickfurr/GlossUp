package com.glossup.recomendacion.domain.port.in;

import java.util.List;

/**
 * Comando de entrada para consolidar un carrito y reservar inventario (HU9).
 *
 * <p>El carrito es enviado por el cliente (no se persiste como tal); cada línea referencia un
 * producto y la cantidad deseada. Las líneas repetidas del mismo producto se consolidan sumando
 * sus cantidades en la capa de aplicación.</p>
 */
public record ConsolidarCarritoCommand(Long usuarioId, List<Linea> lineas) {

    public record Linea(Long productoId, int cantidad) {}
}
