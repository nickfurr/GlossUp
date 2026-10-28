package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.Comprobante;

/**
 * Puerto de entrada: comprobante de compra (HU11). Solo un pedido pagado tiene comprobante; se emite
 * una vez (al confirmarse el pago) y después siempre se entrega el mismo documento.
 */
public interface ComprobanteUseCase {

    /** Emite el comprobante del pedido pagado y lo envía al cliente. Si ya existe, lo devuelve. */
    Comprobante emitir(Long pedidoId);

    /** Comprobante de un pedido propio y pagado; lo emite si todavía no existe. */
    Comprobante descargar(Long usuarioId, Long pedidoId);
}
