package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.port.in.PagarPedidoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook de Wompi (HU10). Es público porque lo llama Wompi, no un usuario: la autenticidad la da
 * el checksum del evento, que se verifica con el secreto de eventos antes de tocar cualquier pedido.
 */
@RestController
@Tag(name = "Pagos", description = "Pago de pedidos con pasarela externa (HU10)")
public class EventosWompiController {

    private final PagarPedidoUseCase pagos;

    public EventosWompiController(PagarPedidoUseCase pagos) {
        this.pagos = pagos;
    }

    @PostMapping("/api/pagos/wompi/eventos")
    @Operation(summary = "Eventos de Wompi (webhook)",
               description = "Wompi reintenta si no recibe 200. Firma inválida → 401 y el evento se ignora.")
    public ResponseEntity<Void> recibir(@RequestBody String cuerpo) {
        return pagos.procesarEvento(cuerpo) ? ResponseEntity.ok().build() : ResponseEntity.status(401).build();
    }
}
