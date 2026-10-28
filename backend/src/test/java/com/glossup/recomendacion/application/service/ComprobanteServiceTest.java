package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.ComprobanteRepositoryPort;
import com.glossup.recomendacion.domain.port.out.DocumentoComprobantePort;
import com.glossup.recomendacion.domain.port.out.NotificacionPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    private static final ClientePort.Cliente ANA = new ClientePort.Cliente("Ana Gómez", "ana@correo.co");
    private static final byte[] PDF = {1, 2, 3};

    @Mock private PedidoRepositoryPort pedidos;
    @Mock private ComprobanteRepositoryPort comprobantes;
    @Mock private ClientePort clientes;
    @Mock private DocumentoComprobantePort documento;
    @Mock private NotificacionPort notificacion;

    private ComprobanteService service;

    @BeforeEach
    void setUp() {
        service = new ComprobanteService(pedidos, comprobantes, clientes, documento, notificacion);
    }

    /** Pedido #7 del usuario 1 en el estado dado. */
    private Pedido pedido(EstadoPedido estado) {
        Pedido p = new Pedido(7L, 1L, estado, List.of(new PedidoItem(100L, "Sérum", 2, Dinero.de(10000.0))),
                OffsetDateTime.now().minusMinutes(10));
        when(pedidos.buscarPorId(7L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void emitir_generaGuardaYEnviaElComprobanteDelPedidoPagado() {
        Pedido p = pedido(EstadoPedido.PAGADO);
        when(comprobantes.buscar(7L)).thenReturn(Optional.empty());
        when(clientes.buscar(1L)).thenReturn(Optional.of(ANA));
        when(documento.generar(eq(p), eq(ANA), eq("GU-0000007"), any())).thenReturn(PDF);
        when(comprobantes.guardarSiNoExiste(any())).thenReturn(true);

        Comprobante c = service.emitir(7L);

        assertEquals("GU-0000007", c.numero());
        assertArrayEquals(PDF, c.pdf());
        verify(notificacion).enviarComprobante(ANA, p, c);
    }

    @Test
    void emitir_siYaExisteDevuelveElMismoSinRegenerarNiReenviar() {
        pedido(EstadoPedido.PAGADO);
        Comprobante guardado = new Comprobante(7L, "GU-0000007", PDF, OffsetDateTime.now());
        when(comprobantes.buscar(7L)).thenReturn(Optional.of(guardado));

        assertSame(guardado, service.emitir(7L));
        verifyNoInteractions(documento, notificacion);
    }

    @Test
    void emitir_siOtraEmisionGanoLaCarreraNoEnviaDosCorreos() {
        pedido(EstadoPedido.PAGADO);
        Comprobante delOtro = new Comprobante(7L, "GU-0000007", new byte[]{9}, OffsetDateTime.now());
        when(comprobantes.buscar(7L)).thenReturn(Optional.empty(), Optional.of(delOtro));
        when(clientes.buscar(1L)).thenReturn(Optional.of(ANA));
        when(documento.generar(any(), any(), any(), any())).thenReturn(PDF);
        when(comprobantes.guardarSiNoExiste(any())).thenReturn(false);

        assertSame(delOtro, service.emitir(7L));
        verifyNoInteractions(notificacion);
    }

    @Test
    void emitir_unFalloDelCorreoNoPierdeElComprobante() {
        pedido(EstadoPedido.PAGADO);
        when(comprobantes.buscar(7L)).thenReturn(Optional.empty());
        when(clientes.buscar(1L)).thenReturn(Optional.of(ANA));
        when(documento.generar(any(), any(), any(), any())).thenReturn(PDF);
        when(comprobantes.guardarSiNoExiste(any())).thenReturn(true);
        doThrow(new RuntimeException("SMTP caído")).when(notificacion).enviarComprobante(any(), any(), any());

        assertEquals("GU-0000007", service.emitir(7L).numero());
    }

    @Test
    void sinPagoNoHayComprobante() {
        pedido(EstadoPedido.CREADO);
        assertThrows(ReglaNegocioException.class, () -> service.emitir(7L));
        pedido(EstadoPedido.CANCELADO);
        assertThrows(ReglaNegocioException.class, () -> service.descargar(1L, 7L));
        verifyNoInteractions(comprobantes, documento, notificacion);
    }

    @Test
    void descargar_soloElDuenoDelPedido() {
        pedido(EstadoPedido.PAGADO);
        assertThrows(NotFoundException.class, () -> service.descargar(99L, 7L));
        verifyNoInteractions(comprobantes);
    }
}
