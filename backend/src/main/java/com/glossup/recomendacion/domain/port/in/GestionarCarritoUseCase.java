package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.Carrito;

/**
 * Puerto de entrada: gestión del carrito de compras del cliente (HU9 - CRUD de carrito).
 *
 * <p>Permite ver y editar el carrito activo del usuario antes del checkout. Agregar/cambiar/quitar
 * no reservan inventario; la reserva ocurre al consolidar el pedido ({@link ConsolidarCarritoUseCase}).</p>
 */
public interface GestionarCarritoUseCase {

    Carrito verCarrito(Long usuarioId);

    Carrito agregarItem(Long usuarioId, Long productoId, int cantidad);

    Carrito cambiarCantidad(Long usuarioId, Long productoId, int cantidad);

    Carrito quitarItem(Long usuarioId, Long productoId);

    Carrito vaciar(Long usuarioId);
}
