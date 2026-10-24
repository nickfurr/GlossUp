package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.Pedido;

/**
 * Puerto de entrada: consolidación del carrito y reserva de inventario (HU9 - SCRUM-36).
 *
 * <p>Toma las líneas de un carrito, reserva (descuenta) el stock de cada producto y crea un
 * {@link Pedido} en estado CREADO. La operación es atómica (RNF-25): si algún producto no tiene
 * stock suficiente, no se reserva nada.</p>
 */
public interface ConsolidarCarritoUseCase {

    Pedido consolidar(ConsolidarCarritoCommand command);
}
