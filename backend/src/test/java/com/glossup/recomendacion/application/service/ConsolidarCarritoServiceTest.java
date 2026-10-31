package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MetodoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoCommand;
import com.glossup.recomendacion.domain.port.out.DireccionRepositoryPort;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    @Mock
    private DireccionRepositoryPort direcciones;

    private ConsolidarCarritoService service;

    private static final Direccion ARMENIA = new Direccion(9L, 7L, "Ana", "3001234567", "Quindío", "Armenia", "Centro",
            "Carrera 14 # 20-35", null, null, true);

    @BeforeEach
    void setUp() {
        service = new ConsolidarCarritoService(inventario, pedidoRepository, direcciones);
        lenient().when(direcciones.buscar(9L)).thenReturn(Optional.of(ARMENIA));
        lenient().when(pedidoRepository.guardar(any(Pedido.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void consolidar_reservaStockYCreaPedidoCreadoConTotalCorrecto() {
        when(inventario.reservar(100L, 2)).thenReturn(new InventarioPort.Reserva("Sérum", Dinero.de(30000)));
        when(inventario.reservar(200L, 1)).thenReturn(new InventarioPort.Reserva("Crema", Dinero.de(15000)));

        Pedido pedido = service.consolidar(new ConsolidarCarritoCommand(7L, List.of(
                new ConsolidarCarritoCommand.Linea(100L, 2),
                new ConsolidarCarritoCommand.Linea(200L, 1)), 9L, MetodoEnvio.ESTANDAR));

        assertEquals(7L, pedido.getUsuarioId());
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        assertEquals(2, pedido.getItems().size());
        assertEquals(Dinero.de(75000), pedido.getSubtotal());
        assertEquals("Sérum", pedido.getItems().get(0).nombre(), "la línea guarda el nombre al comprar (HU11)");
        verify(pedidoRepository).guardar(any(Pedido.class));
    }

    @Test
    void consolidar_sumaCantidadesDelMismoProductoEnUnaSolaReserva() {
        when(inventario.reservar(eq(100L), eq(5))).thenReturn(new InventarioPort.Reserva("Sérum", Dinero.de(10000)));

        Pedido pedido = service.consolidar(new ConsolidarCarritoCommand(7L, List.of(
                new ConsolidarCarritoCommand.Linea(100L, 2),
                new ConsolidarCarritoCommand.Linea(100L, 3)), 9L, MetodoEnvio.ESTANDAR));

        verify(inventario, times(1)).reservar(100L, 5);
        assertEquals(1, pedido.getItems().size());
        assertEquals(5, pedido.getItems().get(0).cantidad());
    }

    @Test
    void consolidar_carritoVacio_lanzaReglaNegocioYNoReserva() {
        assertThrows(ReglaNegocioException.class,
                () -> service.consolidar(new ConsolidarCarritoCommand(7L, List.of(), 9L, MetodoEnvio.ESTANDAR)));

        verifyNoInteractions(inventario);
        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    void consolidar_sinStock_propagaExcepcionYNoGuardaPedido() {
        when(inventario.reservar(100L, 99))
                .thenThrow(new ReglaNegocioException("Stock insuficiente"));

        assertThrows(ReglaNegocioException.class,
                () -> service.consolidar(new ConsolidarCarritoCommand(7L, List.of(
                        new ConsolidarCarritoCommand.Linea(100L, 99)), 9L, MetodoEnvio.ESTANDAR)));

        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    void consolidar_sumaElEnvioAlTotalYCopiaLaDireccion() {
        when(inventario.reservar(100L, 1)).thenReturn(new InventarioPort.Reserva("Sérum", Dinero.de(30000)));

        Pedido pedido = service.consolidar(new ConsolidarCarritoCommand(7L,
                List.of(new ConsolidarCarritoCommand.Linea(100L, 1)), 9L, MetodoEnvio.EXPRESS));

        assertEquals(Dinero.de(30000), pedido.getSubtotal());
        assertEquals(Dinero.de(10000), pedido.getEnvio().getCosto());
        assertEquals(Dinero.de(40000), pedido.getTotal());
        assertEquals(40000_00L, pedido.totalEnCentavos(), "Wompi cobra el total con envío");
        assertEquals("Carrera 14 # 20-35", pedido.getEnvio().getDestino().direccion());
        assertEquals(EstadoEnvio.PREPARANDO, pedido.getEnvio().getEstado());
    }

    @Test
    void consolidar_conDireccionDeOtroCliente_noReservaNada() {
        assertThrows(NotFoundException.class, () -> service.consolidar(new ConsolidarCarritoCommand(8L,
                List.of(new ConsolidarCarritoCommand.Linea(100L, 1)), 9L, MetodoEnvio.ESTANDAR)));
        verifyNoInteractions(inventario);
    }

    @Test
    void consolidar_conExpressADestinoSinExpress_falla() {
        when(direcciones.buscar(9L)).thenReturn(Optional.of(new Direccion(9L, 7L, "Ana", "3001234567", "Vaupés", "Mitú",
                "Centro", "Calle 1", null, null, true)));
        when(inventario.reservar(100L, 1)).thenReturn(new InventarioPort.Reserva("Sérum", Dinero.de(30000)));

        assertThrows(ReglaNegocioException.class, () -> service.consolidar(new ConsolidarCarritoCommand(7L,
                List.of(new ConsolidarCarritoCommand.Linea(100L, 1)), 9L, MetodoEnvio.EXPRESS)));
        verify(pedidoRepository, never()).guardar(any());
    }
}
