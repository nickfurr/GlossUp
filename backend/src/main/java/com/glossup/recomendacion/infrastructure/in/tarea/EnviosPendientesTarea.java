package com.glossup.recomendacion.infrastructure.in.tarea;

import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada minuto reintenta avisar a la transportadora de los pedidos pagados que quedaron sin aceptar (HU14). */
@Component
public class EnviosPendientesTarea {

    private final EnvioUseCase envios;

    public EnviosPendientesTarea(EnvioUseCase envios) {
        this.envios = envios;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void reintentar() {
        envios.reintentarPendientes();
    }
}
