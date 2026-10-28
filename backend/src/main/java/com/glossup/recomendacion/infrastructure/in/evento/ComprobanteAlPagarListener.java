package com.glossup.recomendacion.infrastructure.in.evento;

import com.glossup.recomendacion.domain.model.PedidoPagado;
import com.glossup.recomendacion.domain.port.in.ComprobanteUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emite el comprobante cuando un pago queda registrado (HU11). Corre después del commit y en otro
 * hilo: generar el PDF o esperar al servidor de correo no frena ni revierte la confirmación del pago.
 */
@Component
public class ComprobanteAlPagarListener {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteAlPagarListener.class);

    private final ComprobanteUseCase comprobantes;

    public ComprobanteAlPagarListener(ComprobanteUseCase comprobantes) {
        this.comprobantes = comprobantes;
    }

    @Async
    @TransactionalEventListener
    public void alPagar(PedidoPagado evento) {
        try {
            comprobantes.emitir(evento.pedidoId());
        } catch (RuntimeException e) {
            log.error("No se pudo emitir el comprobante del pedido #{}; se emitirá cuando el cliente lo descargue",
                    evento.pedidoId(), e);
        }
    }
}
