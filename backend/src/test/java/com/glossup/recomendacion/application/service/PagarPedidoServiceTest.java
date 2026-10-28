package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.model.PedidoPagado;
import com.glossup.recomendacion.domain.model.TransaccionPago;
import com.glossup.recomendacion.domain.model.TransaccionPago.Estado;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.recomendacion.domain.port.out.PasarelaPagosPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagarPedidoServiceTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 10, 27, 15, 0, 0, 0, ZoneOffset.UTC);

    @Mock private PedidoRepositoryPort pedidos;
    @Mock private PasarelaPagosPort pasarela;
    @Mock private InventarioPort inventario;
    @Mock private ApplicationEventPublisher eventos;

    private PagarPedidoService service;

    @BeforeEach
    void setUp() {
        service = new PagarPedidoService(pedidos, pasarela, inventario, eventos, "https://glossup.co/",
                Clock.fixed(Instant.from(AHORA), ZoneOffset.UTC));
        lenient().when(pedidos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Pedido #7 del usuario 1: 2 × $10.000 + 1 × $5.000 = $25.000 (2.500.000 centavos). */
    private Pedido pedido(EstadoPedido estado, OffsetDateTime creado) {
        Pedido p = new Pedido(7L, 1L, estado, List.of(
                new PedidoItem(100L, 2, Dinero.de(10000.0)), new PedidoItem(200L, 1, Dinero.de(5000.0))), creado);
        when(pedidos.buscarPorId(7L)).thenReturn(Optional.of(p));
        return p;
    }

    private static TransaccionPago transaccion(Estado estado, long centavos) {
        return new TransaccionPago("tx-1", "GLOSSUP-7-1761577200000", estado, centavos, "COP", "CARD");
    }

    @Test
    void iniciarPago_cobraElTotalEnCentavosYVuelveALaPaginaDeResultado() {
        pedido(EstadoPedido.CREADO, AHORA.minusMinutes(5));
        when(pasarela.urlPago(anyString(), anyLong(), anyString(), any())).thenReturn("https://checkout.wompi.co/p/?x");

        assertEquals("https://checkout.wompi.co/p/?x", service.iniciarPago(1L, 7L));
        verify(pasarela).urlPago(eq("GLOSSUP-7-" + AHORA.toInstant().toEpochMilli()), eq(2_500_000L),
                eq("https://glossup.co/pago/resultado/7"), eq(AHORA.minusMinutes(5).plusMinutes(Pedido.MINUTOS_PARA_PAGAR)));
    }

    @Test
    void iniciarPago_rechazaPedidoAjenoPagadoOVencido() {
        pedido(EstadoPedido.CREADO, AHORA.minusMinutes(5));
        assertThrows(NotFoundException.class, () -> service.iniciarPago(99L, 7L), "pedido de otro usuario");

        pedido(EstadoPedido.PAGADO, AHORA.minusMinutes(5));
        assertThrows(ReglaNegocioException.class, () -> service.iniciarPago(1L, 7L));

        pedido(EstadoPedido.CREADO, AHORA.minusMinutes(Pedido.MINUTOS_PARA_PAGAR + 1));
        assertThrows(ReglaNegocioException.class, () -> service.iniciarPago(1L, 7L), "plazo vencido");
        verifyNoInteractions(pasarela);
    }

    @Test
    void confirmarPago_aprobadaPasaAPagadoUnaSolaVez() {
        Pedido p = pedido(EstadoPedido.CREADO, AHORA.minusMinutes(5));
        when(pasarela.consultar("tx-1")).thenReturn(Optional.of(transaccion(Estado.APROBADA, 2_500_000)));

        var r = service.confirmarPago(1L, 7L, "tx-1");
        assertEquals(EstadoPedido.PAGADO, r.pedido().getEstado());
        assertEquals("tx-1", p.getTransaccionId());
        assertEquals("CARD", p.getMetodoPago());
        assertEquals(AHORA, p.getPagadoEn());

        // El webhook y el regreso del cliente pueden aplicar la misma transacción: no se repite.
        service.confirmarPago(1L, 7L, "tx-1");
        verify(pedidos, times(1)).guardar(any());
        verify(eventos, times(1)).publishEvent(new PedidoPagado(7L)); // un solo comprobante (HU11)
    }

    @Test
    void confirmarPago_rechazadaOConMontoDistintoNoPaga() {
        pedido(EstadoPedido.CREADO, AHORA.minusMinutes(5));
        when(pasarela.consultar("tx-1")).thenReturn(Optional.of(transaccion(Estado.RECHAZADA, 2_500_000)));
        var rechazada = service.confirmarPago(1L, 7L, "tx-1");
        assertEquals(EstadoPedido.CREADO, rechazada.pedido().getEstado(), "puede reintentar");
        assertEquals(Estado.RECHAZADA, rechazada.estadoTransaccion());

        when(pasarela.consultar("tx-1")).thenReturn(Optional.of(transaccion(Estado.APROBADA, 100)));
        assertEquals(EstadoPedido.CREADO, service.confirmarPago(1L, 7L, "tx-1").pedido().getEstado(),
                "un pago aprobado por otro monto no paga el pedido");
        verify(pedidos, never()).guardar(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void confirmarPago_noAceptaTransaccionDeOtroPedido() {
        pedido(EstadoPedido.CREADO, AHORA.minusMinutes(5));
        when(pasarela.consultar("tx-1")).thenReturn(Optional.of(
                new TransaccionPago("tx-1", "GLOSSUP-8-1", Estado.APROBADA, 2_500_000, "COP", "CARD")));
        assertThrows(ReglaNegocioException.class, () -> service.confirmarPago(1L, 7L, "tx-1"));
    }

    @Test
    void evento_firmaInvalidaSeIgnoraYAprobadoPaga() {
        when(pasarela.leerEvento("malo")).thenReturn(Optional.empty());
        assertFalse(service.procesarEvento("malo"));

        Pedido p = pedido(EstadoPedido.CREADO, AHORA.minusMinutes(5));
        when(pasarela.leerEvento("bueno")).thenReturn(Optional.of(transaccion(Estado.APROBADA, 2_500_000)));
        assertTrue(service.procesarEvento("bueno"));
        assertEquals(EstadoPedido.PAGADO, p.getEstado());
    }

    @Test
    void pagoAprobadoDeUnPedidoYaCanceladoNoLoRevive() {
        Pedido p = pedido(EstadoPedido.CANCELADO, AHORA.minusHours(2));
        when(pasarela.leerEvento("tarde")).thenReturn(Optional.of(transaccion(Estado.APROBADA, 2_500_000)));
        assertTrue(service.procesarEvento("tarde"));
        assertEquals(EstadoPedido.CANCELADO, p.getEstado());
    }

    @Test
    void cancelar_devuelveElInventarioDeCadaLinea() {
        pedido(EstadoPedido.CREADO, AHORA.minusMinutes(5));
        assertEquals(EstadoPedido.CANCELADO, service.cancelar(1L, 7L).getEstado());
        verify(inventario).liberar(100L, 2);
        verify(inventario).liberar(200L, 1);

        pedido(EstadoPedido.PAGADO, AHORA.minusMinutes(5));
        assertThrows(ReglaNegocioException.class, () -> service.cancelar(1L, 7L), "un pedido pagado no se cancela aquí");
    }

    @Test
    void cancelarVencidos_esperaElPlazoMasUnMargenYLiberaStock() {
        Pedido vencido = new Pedido(9L, 1L, EstadoPedido.CREADO,
                List.of(new PedidoItem(100L, 3, Dinero.de(1000.0))), AHORA.minusHours(1));
        when(pedidos.buscarSinPagarCreadosAntesDe(AHORA.minusMinutes(Pedido.MINUTOS_PARA_PAGAR + 5)))
                .thenReturn(List.of(vencido));

        assertEquals(1, service.cancelarVencidos());
        assertEquals(EstadoPedido.CANCELADO, vencido.getEstado());
        verify(inventario).liberar(100L, 3);
    }

    @Test
    void referencia_soloAceptaLasDeGlossup() {
        assertEquals(Optional.of(7L), PagarPedidoService.pedidoDeReferencia("GLOSSUP-7-123"));
        assertTrue(PagarPedidoService.pedidoDeReferencia("OTRA-7-123").isEmpty());
        assertTrue(PagarPedidoService.pedidoDeReferencia("GLOSSUP-x-1").isEmpty());
        assertTrue(PagarPedidoService.pedidoDeReferencia(null).isEmpty());
    }
}
