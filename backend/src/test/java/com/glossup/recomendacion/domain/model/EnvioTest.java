package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnvioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-30T15:00:00Z");

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        Envio envio = new Envio(new Direccion(null, null, "Ana", "3001234567", "Quindío", "Armenia", "Centro", "Calle 1",
                null, null, false), MetodoEnvio.ESTANDAR, Dinero.de(6000), LocalDate.of(2026, 11, 4));
        pedido = new Pedido(1L, 1L, EstadoPedido.PAGADO, List.of(new PedidoItem(100L, 1, Dinero.de(10000))), AHORA, envio);
        envio.aceptado("Demo", "G-1", AHORA);
    }

    @Test
    void alSalirElPaquete_elPedidoPasaAEnviado() {
        assertTrue(pedido.actualizarEnvio(EstadoEnvio.EN_CAMINO, AHORA));
        assertEquals(EstadoPedido.ENVIADO, pedido.getEstado());
        assertEquals(AHORA, pedido.getEnvio().getEnviadoEn());
    }

    @Test
    void entregaSinHaberReportadoEnCamino_marcaAmbas() {
        assertTrue(pedido.actualizarEnvio(EstadoEnvio.ENTREGADO, AHORA));
        assertEquals(EstadoPedido.ENVIADO, pedido.getEstado());
        assertNotNull(pedido.getEnvio().getEnviadoEn());
        assertNotNull(pedido.getEnvio().getEntregadoEn());
    }

    @Test
    void eventoRepetidoOAtrasado_noCambiaNada() {
        pedido.actualizarEnvio(EstadoEnvio.ENTREGADO, AHORA);
        OffsetDateTime despues = AHORA.plusHours(1);
        assertFalse(pedido.actualizarEnvio(EstadoEnvio.ENTREGADO, despues));
        assertFalse(pedido.actualizarEnvio(EstadoEnvio.EN_CAMINO, despues));
        assertFalse(pedido.actualizarEnvio(EstadoEnvio.NO_ENTREGADO, despues), "una etapa final no se reabre");
        assertEquals(EstadoEnvio.ENTREGADO, pedido.getEnvio().getEstado());
        assertEquals(AHORA, pedido.getEnvio().getEntregadoEn());
    }

    @Test
    void laTransportadoraNoPuedeReportarEtapasInternas() {
        assertFalse(pedido.actualizarEnvio(EstadoEnvio.PREPARANDO, AHORA));
        assertFalse(pedido.actualizarEnvio(EstadoEnvio.LISTO_PARA_ENVIO, AHORA));
    }
}
