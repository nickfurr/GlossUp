package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.TransaccionPago;

import java.util.List;

/**
 * Puerto de entrada: pago de un pedido con la pasarela externa (HU10). El pedido solo pasa a
 * PAGADO con una transacción aprobada que la pasarela confirma (consulta directa o evento firmado);
 * el regreso del cliente desde la pasarela nunca se toma como prueba de pago.
 */
public interface PagarPedidoUseCase {

    /** URL de la pasarela para pagar un pedido propio que sigue CREADO y vigente. */
    String iniciarPago(Long usuarioId, Long pedidoId);

    /** Consulta la transacción a la pasarela y la aplica al pedido. */
    ResultadoPago confirmarPago(Long usuarioId, Long pedidoId, String transaccionId);

    /** Aplica un evento de la pasarela. Devuelve false si la firma no es válida. */
    boolean procesarEvento(String cuerpo);

    /** Cancela un pedido propio sin pagar y devuelve su inventario. */
    Pedido cancelar(Long usuarioId, Long pedidoId);

    /** Cancela los pedidos que vencieron sin pagar y devuelve su inventario. */
    int cancelarVencidos();

    List<Pedido> misPedidos(Long usuarioId);

    record ResultadoPago(Pedido pedido, TransaccionPago.Estado estadoTransaccion) {}
}
