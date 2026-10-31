package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.model.VentaVendedor;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.DespachoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.ProductosVendedorPort;
import com.glossup.shared.domain.Dinero;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VentasVendedorServiceTest {

    @Test
    void elVendedorSoloVeSusLineasEIngresos() {
        PedidoRepositoryPort pedidos = mock(PedidoRepositoryPort.class);
        ProductosVendedorPort productos = mock(ProductosVendedorPort.class);
        ClientePort clientes = mock(ClientePort.class);
        DespachoRepositoryPort despachos = mock(DespachoRepositoryPort.class);
        when(despachos.pedidosDespachadosPor(5L)).thenReturn(Set.of(9L));
        OffsetDateTime ahora = OffsetDateTime.now();
        Pedido compartido = new Pedido(9L, 7L, EstadoPedido.PAGADO, List.of(
                new PedidoItem(1L, "Mío", 2, Dinero.de(10000)),
                new PedidoItem(2L, "De otro vendedor", 1, Dinero.de(99000))), ahora).conPago("t", "CARD", ahora);
        when(productos.idsDe(5L)).thenReturn(Set.of(1L));
        when(pedidos.buscarPagadosConProductos(Set.of(1L))).thenReturn(List.of(compartido));
        when(clientes.buscar(7L)).thenReturn(Optional.of(new ClientePort.Cliente("Ana", "ana@correo.co")));

        var resultado = new VentasVendedorService(pedidos, productos, clientes, despachos).ventas(5L);

        VentaVendedor venta = resultado.ventas().get(0);
        assertEquals("Ana", venta.cliente());
        org.junit.jupiter.api.Assertions.assertTrue(venta.enviadaPorMi());
        assertEquals(List.of(1L), venta.lineas().stream().map(PedidoItem::productoId).toList());
        assertEquals(Dinero.de(20000), venta.ingresos());
        assertEquals(Dinero.de(20000), resultado.resumen().ingresosMes());
    }
}
