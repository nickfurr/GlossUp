package com.glossup.recomendacion.infrastructure.in.rest.dto;

import com.glossup.recomendacion.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO de salida con el pedido consolidado y su inventario reservado (HU9). */
public record PedidoResponse(
        Long id,
        Long usuarioId,
        String estado,
        List<Item> items,
        BigDecimal total,
        OffsetDateTime creadoEn
) {
    public record Item(Long productoId, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    public static PedidoResponse fromDomain(Pedido pedido) {
        List<Item> items = pedido.getItems().stream()
                .map(i -> new Item(
                        i.productoId(),
                        i.cantidad(),
                        i.precioUnitario().monto(),
                        i.subtotal().monto()))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuarioId(),
                pedido.getEstado().name(),
                items,
                pedido.getTotal().monto(),
                pedido.getCreadoEn());
    }
}
