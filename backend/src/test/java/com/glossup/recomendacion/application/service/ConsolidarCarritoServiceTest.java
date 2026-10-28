package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoCommand;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsolidarCarritoServiceTest {

    @Mock
    private InventarioPort inventario;

    @Mock
    private PedidoRepositoryPort pedidoRepository;

    private ConsolidarCarritoService service;

    @BeforeEach
    void setUp() {
        service = new ConsolidarCarritoService(inventario, pedidoRepository);
        lenient().when(pedidoRepository.guardar(any(Pedido.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void consolidar_reservaStockYCreaPedidoCreadoConTotalCorrecto() {
        when(inventario.reservar(100L, 2)).thenReturn(new InventarioPort.Reserva("Sérum", Dinero.de(30000)));
        when(inventario.reservar(200L, 1)).thenReturn(new InventarioPort.Reserva("Crema", Dinero.de(15000)));

        Pedido pedido = service.consolidar(new ConsolidarCarritoCommand(7L, List.of(
                new ConsolidarCarritoCommand.Linea(100L, 2),
                new ConsolidarCarritoCommand.Linea(200L, 1))));

        assertEquals(7L, pedido.getUsuarioId());
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        assertEquals(2, pedido.getItems().size());
        assertEquals(Dinero.de(75000), pedido.getTotal());
        assertEquals("Sérum", pedido.getItems().get(0).nombre(), "la línea guarda el nombre al comprar (HU11)");
        verify(pedidoRepository).guardar(any(Pedido.class));
    }

    @Test
    void consolidar_sumaCantidadesDelMismoProductoEnUnaSolaReserva() {
        when(inventario.reservar(eq(100L), eq(5))).thenReturn(new InventarioPort.Reserva("Sérum", Dinero.de(10000)));

        Pedido pedido = service.consolidar(new ConsolidarCarritoCommand(7L, List.of(
                new ConsolidarCarritoCommand.Linea(100L, 2),
                new ConsolidarCarritoCommand.Linea(100L, 3))));

        verify(inventario, times(1)).reservar(100L, 5);
        assertEquals(1, pedido.getItems().size());
        assertEquals(5, pedido.getItems().get(0).cantidad());
    }

    @Test
    void consolidar_carritoVacio_lanzaReglaNegocioYNoReserva() {
        assertThrows(ReglaNegocioException.class,
                () -> service.consolidar(new ConsolidarCarritoCommand(7L, List.of())));

        verifyNoInteractions(inventario);
        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    void consolidar_sinStock_propagaExcepcionYNoGuardaPedido() {
        when(inventario.reservar(100L, 99))
                .thenThrow(new ReglaNegocioException("Stock insuficiente"));

        assertThrows(ReglaNegocioException.class,
                () -> service.consolidar(new ConsolidarCarritoCommand(7L, List.of(
                        new ConsolidarCarritoCommand.Linea(100L, 99)))));

        verify(pedidoRepository, never()).guardar(any());
    }
}
