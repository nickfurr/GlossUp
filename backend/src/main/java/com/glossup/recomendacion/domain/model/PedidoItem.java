package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;

import java.util.Objects;

/**
 * Línea inmutable de un pedido confirmado (foto del producto al momento de la compra): el nombre y
 * el precio son los de ese momento, para el historial y el comprobante (HU11).
 */
public record PedidoItem(Long productoId, String nombre, int cantidad, Dinero precioUnitario) {

    public PedidoItem {
        Objects.requireNonNull(productoId, "productoId requerido");
        Objects.requireNonNull(precioUnitario, "precio requerido");
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor que cero");
        }
    }

    public PedidoItem(Long productoId, int cantidad, Dinero precioUnitario) {
        this(productoId, null, cantidad, precioUnitario);
    }

    public Dinero subtotal() {
        return precioUnitario.por(cantidad);
    }
}
