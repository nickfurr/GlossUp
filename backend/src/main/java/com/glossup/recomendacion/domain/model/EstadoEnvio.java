package com.glossup.recomendacion.domain.model;

/**
 * Etapas del envío, en orden. Siguen el modelo de envíos de Mercado Libre: handling, ready_to_ship,
 * shipped, delivered y not_delivered (estos dos últimos son finales).
 */
public enum EstadoEnvio {
    PREPARANDO,
    LISTO_PARA_ENVIO,
    EN_CAMINO,
    ENTREGADO,
    NO_ENTREGADO;

    public boolean esFinal() {
        return this == ENTREGADO || this == NO_ENTREGADO;
    }

    /** Las etapas que informa la transportadora; las anteriores las pone GlossUP. */
    public boolean laInformaLaTransportadora() {
        return this == EN_CAMINO || esFinal();
    }
}
