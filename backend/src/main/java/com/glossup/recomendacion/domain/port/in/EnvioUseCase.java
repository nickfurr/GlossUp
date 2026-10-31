package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;

import java.util.List;

/** Puerto de entrada: envío del pedido pagado y su seguimiento (HU14 - SCRUM-39). */
public interface EnvioUseCase {

    /** Avisa a la transportadora. Nunca lanza por un fallo de ella: queda para reintentar. */
    void notificar(Long pedidoId);

    void reintentarPendientes();

    List<Pedido> enCurso();

    /** Aplica una etapa reportada por la transportadora. Idempotente. */
    void actualizar(String guia, EstadoEnvio etapa);

    /** El vendedor confirma que entregó su parte a la transportadora. Idempotente. */
    void marcarEnviado(Long vendedorId, Long pedidoId);

    void avisarCliente(Long pedidoId);
}
