package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.model.Pedido;

/** Puerto de salida: avisa al cliente del pago confirmado con su comprobante (HU11), de cada etapa del envío (HU14) y de los reembolsos. */
public interface NotificacionPort {

    void enviarComprobante(ClientePort.Cliente cliente, Pedido pedido, Comprobante comprobante);

    void avisarEnvio(ClientePort.Cliente cliente, Pedido pedido);

    /** El pago llegó cuando el pedido ya estaba cancelado: se le devolverá el dinero (auditoría OBS-T13). */
    void avisarReembolso(ClientePort.Cliente cliente, Pedido pedido);
}
