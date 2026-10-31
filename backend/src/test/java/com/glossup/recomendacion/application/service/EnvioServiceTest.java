package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.EnvioActualizado;
import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MetodoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.NotificacionPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.TransportadoraPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock private PedidoRepositoryPort pedidos;
    @Mock private TransportadoraPort transportadora;
    @Mock private NotificacionPort notificacion;
    @Mock private ClientePort clientes;
    @Mock private ApplicationEventPublisher eventos;

    private EnvioService service;

    @BeforeEach
    void setUp() {
        service = new EnvioService(pedidos, transportadora, notificacion, clientes, eventos);
    }

    private static Pedido pagado(Envio envio) {
        return new Pedido(5L, 7L, EstadoPedido.PAGADO, List.of(new PedidoItem(100L, 1, Dinero.de(10000))),
                OffsetDateTime.now(), envio);
    }

    private static Envio envio() {
        return new Envio(new Direccion(null, null, "Ana", "3001234567", "Quindío", "Armenia", "Centro", "Calle 1", null, null, false),
                MetodoEnvio.ESTANDAR, Dinero.de(6000), LocalDate.of(2026, 11, 4));
    }

    @Test
    void notificar_guardaLaGuiaQueAsignaLaTransportadora() {
        Pedido pedido = pagado(envio());
        when(pedidos.buscarPorId(5L)).thenReturn(Optional.of(pedido));
        when(transportadora.solicitarEnvio(pedido)).thenReturn(new TransportadoraPort.Aceptacion("Demo", "G-5"));

        service.notificar(5L);

        assertEquals(EstadoEnvio.LISTO_PARA_ENVIO, pedido.getEnvio().getEstado());
        assertEquals("G-5", pedido.getEnvio().getGuia());
        assertNotNull(pedido.getEnvio().getNotificadoEn());
        verify(pedidos).guardar(pedido);
    }

    @Test
    void notificar_siLaTransportadoraFalla_noLanzaYQuedaParaReintentar() {
        Pedido pedido = pagado(envio());
        when(pedidos.buscarPorId(5L)).thenReturn(Optional.of(pedido));
        when(transportadora.solicitarEnvio(pedido)).thenThrow(new RuntimeException("503"));

        assertDoesNotThrow(() -> service.notificar(5L));

        assertEquals(EstadoEnvio.PREPARANDO, pedido.getEnvio().getEstado());
        assertEquals(1, pedido.getEnvio().getIntentos());
        assertEquals(EstadoPedido.PAGADO, pedido.getEstado(), "el pago no se revierte");
        verify(pedidos).guardar(pedido);
    }

    @Test
    void notificar_yaAvisadoOSinEnvio_noLlamaALaTransportadora() {
        Envio avisado = envio();
        avisado.aceptado("Demo", "G-5", OffsetDateTime.now());
        when(pedidos.buscarPorId(5L)).thenReturn(Optional.of(pagado(avisado)));
        service.notificar(5L);

        when(pedidos.buscarPorId(6L)).thenReturn(Optional.of(pagado(null)));
        service.notificar(6L);

        verifyNoInteractions(transportadora);
    }

    @Test
    void reintentarPendientes_avisaLosQueQuedaronSinAceptar() {
        Pedido pedido = pagado(envio());
        when(pedidos.buscarEnviosSinNotificar(any(), eq(EnvioService.MAX_INTENTOS))).thenReturn(List.of(pedido));
        when(pedidos.buscarPorId(5L)).thenReturn(Optional.of(pedido));
        when(transportadora.solicitarEnvio(pedido)).thenReturn(new TransportadoraPort.Aceptacion("Demo", "G-5"));

        service.reintentarPendientes();

        assertEquals("G-5", pedido.getEnvio().getGuia());
    }

    @Test
    void actualizar_cambiaLaEtapaYPublicaElAviso() {
        Envio envio = envio();
        envio.aceptado("Demo", "G-5", OffsetDateTime.now());
        Pedido pedido = pagado(envio);
        when(pedidos.buscarPorGuia("G-5")).thenReturn(Optional.of(pedido));

        service.actualizar("G-5", EstadoEnvio.EN_CAMINO);

        assertEquals(EstadoPedido.ENVIADO, pedido.getEstado());
        verify(pedidos).guardar(pedido);
        verify(eventos).publishEvent(new EnvioActualizado(5L));
    }

    @Test
    void actualizar_eventoRepetido_noGuardaNiAvisa() {
        Envio envio = envio();
        envio.aceptado("Demo", "G-5", OffsetDateTime.now());
        Pedido pedido = pagado(envio);
        pedido.actualizarEnvio(EstadoEnvio.EN_CAMINO, OffsetDateTime.now());
        when(pedidos.buscarPorGuia("G-5")).thenReturn(Optional.of(pedido));

        service.actualizar("G-5", EstadoEnvio.EN_CAMINO);

        verify(pedidos, never()).guardar(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void actualizar_guiaDesconocida_falla() {
        when(pedidos.buscarPorGuia("X")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.actualizar("X", EstadoEnvio.EN_CAMINO));
    }
}
