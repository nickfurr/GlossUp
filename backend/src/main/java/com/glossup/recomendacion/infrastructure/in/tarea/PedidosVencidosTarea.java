package com.glossup.recomendacion.infrastructure.in.tarea;

import com.glossup.recomendacion.domain.port.in.PagarPedidoUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada minuto cancela los pedidos que vencieron sin pagar, para devolver su inventario (HU10). */
@Component
public class PedidosVencidosTarea {

    private final PagarPedidoUseCase pagos;

    public PedidosVencidosTarea(PagarPedidoUseCase pagos) {
        this.pagos = pagos;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void cancelarVencidos() {
        pagos.cancelarVencidos();
    }
}
