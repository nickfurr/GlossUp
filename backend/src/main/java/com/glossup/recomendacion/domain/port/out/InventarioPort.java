package com.glossup.recomendacion.domain.port.out;

import com.glossup.shared.domain.Dinero;

/**
 * Puerto de salida: reserva de inventario contra el catálogo (HU9).
 *
 * <p>Permite al módulo de recomendación/ventas descontar stock sin conocer los detalles del
 * agregado {@code Producto}, que viven en el módulo de catálogo.</p>
 */
public interface InventarioPort {

    /**
     * Reserva (descuenta) {@code cantidad} unidades del producto y devuelve su nombre y precio
     * unitario actuales, para fijarlos en la línea del pedido.
     *
     * @throws com.glossup.shared.exception.NotFoundException si el producto no existe
     * @throws com.glossup.shared.domain.ReglaNegocioException si no está disponible o sin stock suficiente
     */
    Reserva reservar(Long productoId, int cantidad);

    /**
     * Devuelve el precio unitario actual de un producto disponible, sin tocar el stock (para
     * armar el carrito). La reserva de inventario solo ocurre en el checkout (HU9).
     *
     * @throws com.glossup.shared.exception.NotFoundException si el producto no existe
     * @throws com.glossup.shared.domain.ReglaNegocioException si no está disponible
     */
    Dinero consultarPrecio(Long productoId);

    /** Devuelve al stock unidades que reservó un pedido que se canceló o venció sin pagar (HU10). */
    void liberar(Long productoId, int cantidad);

    record Reserva(String nombre, Dinero precioUnitario) {}
}
