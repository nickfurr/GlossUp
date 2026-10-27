package com.glossup.recomendacion.domain.model;

/**
 * Transacción tal como la reporta la pasarela de pagos (HU10). El monto viaja en centavos, como
 * lo maneja la pasarela, para comparar sin redondeos contra el total del pedido.
 */
public record TransaccionPago(String id, String referencia, Estado estado, long montoCentavos,
                              String moneda, String metodo) {

    public enum Estado {
        APROBADA, PENDIENTE, RECHAZADA, ANULADA, ERROR;

        public boolean esFinal() { return this != PENDIENTE; }
    }
}
