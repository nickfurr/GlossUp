package com.glossup.recomendacion.infrastructure.in.tarea;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MetodoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import com.glossup.shared.domain.Dinero;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnvioSimuladoTareaTest {

    private final EnvioUseCase envios = mock(EnvioUseCase.class);
    private final EnvioSimuladoTarea tarea = new EnvioSimuladoTarea(envios, 3);

    private static Pedido pedido(EstadoEnvio etapa, OffsetDateTime enviadoEn) {
        OffsetDateTime hace1Hora = OffsetDateTime.now().minusHours(1);
        Envio envio = new Envio(new Direccion(null, null, "Ana", "3001234567", "Quindío", "Armenia", "Centro", "Calle 1", null, null, false),
                MetodoEnvio.ESTANDAR, Dinero.de(6000), LocalDate.of(2026, 11, 4))
                .conSeguimiento(etapa, "Demo", "G-1", 0, hace1Hora, enviadoEn, null, null);
        return new Pedido(1L, 1L, EstadoPedido.PAGADO, List.of(new PedidoItem(1L, 1, Dinero.de(1000))), hace1Hora, envio);
    }

    @Test
    void sinLaConfirmacionDelVendedorNoAvanzaAunquePaseElTiempo() {
        when(envios.enCurso()).thenReturn(List.of(pedido(EstadoEnvio.LISTO_PARA_ENVIO, null)));
        tarea.avanzar();
        verify(envios, never()).actualizar(any(), any());
    }

    @Test
    void despuesDeConfirmadoEntregaCuandoPasaLaEtapa() {
        when(envios.enCurso()).thenReturn(List.of(pedido(EstadoEnvio.EN_CAMINO, OffsetDateTime.now().minusMinutes(5))));
        tarea.avanzar();
        verify(envios).actualizar("G-1", EstadoEnvio.ENTREGADO);
    }

    @Test
    void recienConfirmadoTodaviaNoSeEntrega() {
        when(envios.enCurso()).thenReturn(List.of(pedido(EstadoEnvio.EN_CAMINO, OffsetDateTime.now().minusMinutes(1))));
        tarea.avanzar();
        verify(envios, never()).actualizar(any(), any());
    }
}
