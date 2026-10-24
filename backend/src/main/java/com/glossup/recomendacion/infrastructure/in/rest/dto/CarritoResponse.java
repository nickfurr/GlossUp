package com.glossup.recomendacion.infrastructure.in.rest.dto;

import com.glossup.recomendacion.domain.model.Carrito;

import java.math.BigDecimal;
import java.util.List;

/** DTO de salida con el estado actual del carrito del cliente (HU9 - CRUD de carrito). */
public record CarritoResponse(
        Long usuarioId,
        List<Item> items,
        BigDecimal total
) {
    public record Item(Long productoId, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    public static CarritoResponse fromDomain(Carrito carrito) {
        List<Item> items = carrito.getItems().stream()
                .map(i -> new Item(
                        i.getProductoId(),
                        i.getCantidad(),
                        i.getPrecioUnitario().monto(),
                        i.subtotal().monto()))
                .toList();

        return new CarritoResponse(carrito.getUsuarioId(), items, carrito.total().monto());
    }
}
