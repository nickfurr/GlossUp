package com.glossup.recomendacion.infrastructure.in.rest.dto;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO de salida con el pedido consolidado y su inventario reservado (HU9). */
public record PedidoResponse(
        Long id,
        Long usuarioId,
        String estado,
        List<Item> items,
        BigDecimal subtotal,
        EnvioResponse envio,
        BigDecimal total,
        OffsetDateTime creadoEn,
        OffsetDateTime expiraEn,
        String transaccionId,
        String metodoPago,
        OffsetDateTime pagadoEn
) {
    public record Item(Long productoId, String nombre, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    public record EnvioResponse(String nombreRecibe, String telefono, String departamento, String ciudad, String barrio, String direccion,
                                String complemento, String indicaciones, String metodo, String metodoEtiqueta, BigDecimal costo,
                                LocalDate fechaEstimada, String estado, String transportadora, String guia,
                                OffsetDateTime notificadoEn, OffsetDateTime enviadoEn,
                                OffsetDateTime entregadoEn, OffsetDateTime noEntregadoEn) {}

    public static Item itemDe(PedidoItem i) {
        return new Item(i.productoId(), i.nombre(), i.cantidad(), i.precioUnitario().monto(), i.subtotal().monto());
    }

    public static EnvioResponse envioDe(Pedido pedido) {
        var e = pedido.getEnvio();
        if (e == null) {
            return null;
        }
        Direccion d = e.getDestino();
        return new EnvioResponse(d.nombreRecibe(), d.telefono(), d.departamento(), d.ciudad(), d.barrio(), d.direccion(),
                d.complemento(), d.indicaciones(), e.getMetodo().name(), e.getMetodo().etiqueta(), e.getCosto().monto(),
                e.getFechaEstimada(), e.getEstado().name(), e.getTransportadora(), e.getGuia(),
                e.getNotificadoEn(), e.getEnviadoEn(), e.getEntregadoEn(), e.getNoEntregadoEn());
    }

    public static PedidoResponse fromDomain(Pedido pedido) {
        List<Item> items = pedido.getItems().stream().map(PedidoResponse::itemDe).toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuarioId(),
                pedido.getEstado().name(),
                items,
                pedido.getSubtotal().monto(),
                envioDe(pedido),
                pedido.getTotal().monto(),
                pedido.getCreadoEn(),
                pedido.getEstado().name().equals("CREADO") ? pedido.expiraEn() : null,
                pedido.getTransaccionId(),
                pedido.getMetodoPago(),
                pedido.getPagadoEn());
    }
}
