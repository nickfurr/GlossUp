package com.glossup.recomendacion.infrastructure.in.evento;

import com.glossup.recomendacion.domain.model.EnvioActualizado;
import com.glossup.recomendacion.domain.model.PedidoPagado;
import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fire and forget (HU14): corre después del commit y en otro hilo, así la API de envíos o el correo
 * no frenan ni revierten el pago ni la actualización del envío.
 */
@Component
public class EnvioListener {

    private static final Logger log = LoggerFactory.getLogger(EnvioListener.class);

    private final EnvioUseCase envios;

    public EnvioListener(EnvioUseCase envios) {
        this.envios = envios;
    }

    @Async
    @TransactionalEventListener
    public void alPagar(PedidoPagado evento) {
        try {
            envios.notificar(evento.pedidoId());
        } catch (RuntimeException e) {
            log.error("No se pudo procesar el envío del pedido #{}; se reintentará", evento.pedidoId(), e);
        }
    }

    @Async
    @TransactionalEventListener
    public void alActualizarEnvio(EnvioActualizado evento) {
        try {
            envios.avisarCliente(evento.pedidoId());
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar del envío del pedido #{}: {}", evento.pedidoId(), e.getMessage());
        }
    }
}
