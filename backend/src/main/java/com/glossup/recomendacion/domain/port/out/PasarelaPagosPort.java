package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.TransaccionPago;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Puerto de salida: pasarela de pagos externa (HU10). El cliente paga en la página de la pasarela,
 * así GlossUP nunca recibe datos de tarjeta (RNF-13).
 */
public interface PasarelaPagosPort {

    /**
     * URL de la página de pago de la pasarela para cobrar {@code montoCentavos} COP. El enlace deja
     * de aceptar pagos en {@code expira} y, al terminar, devuelve al cliente a {@code urlRetorno}.
     */
    String urlPago(String referencia, long montoCentavos, String urlRetorno, OffsetDateTime expira);

    /** Estado actual de una transacción, consultado directamente a la pasarela; vacío si no existe. */
    Optional<TransaccionPago> consultar(String transaccionId);

    /**
     * Transacción reportada por un evento (webhook) de la pasarela; vacío si la firma no es válida
     * o el evento no es de una transacción.
     */
    Optional<TransaccionPago> leerEvento(String cuerpo);
}
