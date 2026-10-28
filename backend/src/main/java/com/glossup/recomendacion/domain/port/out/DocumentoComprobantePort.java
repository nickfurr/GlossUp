package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Pedido;

import java.time.OffsetDateTime;

/** Puerto de salida: genera el documento (PDF) del comprobante de un pedido pagado (HU11). */
public interface DocumentoComprobantePort {

    byte[] generar(Pedido pedido, ClientePort.Cliente cliente, String numero, OffsetDateTime emitidoEn);
}
