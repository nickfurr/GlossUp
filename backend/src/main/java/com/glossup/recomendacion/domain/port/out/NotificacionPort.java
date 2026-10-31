package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.model.Pedido;

/** Puerto de salida: avisa al cliente del pago confirmado con su comprobante (HU11) y de cada etapa del envío (HU14). */
public interface NotificacionPort {

    void enviarComprobante(ClientePort.Cliente cliente, Pedido pedido, Comprobante comprobante);

    void avisarEnvio(ClientePort.Cliente cliente, Pedido pedido);
}
