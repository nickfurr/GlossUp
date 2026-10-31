package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;

import java.util.List;

/**
 * Lo que un vendedor ve de un pedido pagado: solo sus líneas y sus ingresos, no lo que el mismo
 * cliente le compró a otros vendedores ni el costo de envío.
 */
public record VentaVendedor(Pedido pedido, String cliente, List<PedidoItem> lineas, boolean enviadaPorMi) {

    public Dinero ingresos() {
        return Pedido.subtotalDe(lineas);
    }

    public int unidades() {
        return lineas.stream().mapToInt(PedidoItem::cantidad).sum();
    }
}
