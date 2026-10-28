package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.model.Pedido;

/** Puerto de salida: avisa al cliente que su pago se confirmó y le entrega el comprobante (HU11). */
public interface NotificacionPort {

    void enviarComprobante(ClientePort.Cliente cliente, Pedido pedido, Comprobante comprobante);
}
