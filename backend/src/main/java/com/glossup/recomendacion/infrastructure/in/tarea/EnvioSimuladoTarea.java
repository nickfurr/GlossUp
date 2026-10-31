package com.glossup.recomendacion.infrastructure.in.tarea;

import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Hace de transportadora de demostración (sin GLOSSUP_ENVIOS_URL): avanza cada envío una etapa cada
 * pocos minutos por el mismo camino que usaría el webhook de una transportadora real.
 */
@Component
@ConditionalOnExpression("'${glossup.envios.url:}'.isBlank()")
public class EnvioSimuladoTarea {

    private final EnvioUseCase envios;
    private final int minutosPorEtapa;

    public EnvioSimuladoTarea(EnvioUseCase envios,
                              @Value("${glossup.envios.simulador.minutos-por-etapa:3}") int minutosPorEtapa) {
        this.envios = envios;
        this.minutosPorEtapa = minutosPorEtapa;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void avanzar() {
        OffsetDateTime limite = OffsetDateTime.now().minusMinutes(minutosPorEtapa);
        for (Pedido pedido : envios.enCurso()) {
            Envio envio = pedido.getEnvio();
            if (envio.getEstado() == EstadoEnvio.LISTO_PARA_ENVIO && envio.getNotificadoEn().isBefore(limite)) {
                envios.actualizar(envio.getGuia(), EstadoEnvio.EN_CAMINO);
            } else if (envio.getEstado() == EstadoEnvio.EN_CAMINO && envio.getEnviadoEn().isBefore(limite)) {
                envios.actualizar(envio.getGuia(), EstadoEnvio.ENTREGADO);
            }
        }
    }
}
