package com.glossup.recomendacion.infrastructure.out.envio;

import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.out.TransportadoraPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** shortcut: transportadora de demostración sin envío físico; definir GLOSSUP_ENVIOS_URL cuando haya contrato con una real. */
@Component
@ConditionalOnExpression("'${glossup.envios.url:}'.isBlank()")
public class TransportadoraSimuladaAdapter implements TransportadoraPort {

    @Override
    public Aceptacion solicitarEnvio(Pedido pedido) {
        return new Aceptacion("GlossUP Envíos (demostración)", "DEMO-%07d".formatted(pedido.getId()));
    }
}
