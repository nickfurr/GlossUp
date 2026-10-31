package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.ResumenVentas;
import com.glossup.recomendacion.domain.model.VentaVendedor;
import com.glossup.recomendacion.domain.port.in.VentasVendedorUseCase;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.DespachoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.ProductosVendedorPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Ventas del vendedor. Solo cuenta pedidos pagados: el vendedor no ve datos del cliente hasta que
 * hay una venta real que despachar, y solo recibe sus propias líneas.
 */
@Service
@Transactional(readOnly = true)
public class VentasVendedorService implements VentasVendedorUseCase {

    private final PedidoRepositoryPort pedidos;
    private final ProductosVendedorPort productos;
    private final ClientePort clientes;
    private final DespachoRepositoryPort despachos;

    public VentasVendedorService(PedidoRepositoryPort pedidos, ProductosVendedorPort productos, ClientePort clientes,
                                 DespachoRepositoryPort despachos) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.clientes = clientes;
        this.despachos = despachos;
    }

    @Override
    public Ventas ventas(Long vendedorId) {
        Set<Long> propios = productos.idsDe(vendedorId);
        Set<Long> enviados = despachos.pedidosDespachadosPor(vendedorId);
        // shortcut: trae todo el historial y un cliente por pedido; paginar y agregar en SQL cuando un vendedor pase de miles de pedidos.
        List<VentaVendedor> ventas = pedidos.buscarPagadosConProductos(propios).stream()
                .map(p -> new VentaVendedor(p,
                        clientes.buscar(p.getUsuarioId()).map(ClientePort.Cliente::nombre).orElse("Cliente"),
                        p.getItems().stream().filter(i -> propios.contains(i.productoId())).toList(),
                        enviados.contains(p.getId())))
                .toList();
        return new Ventas(ResumenVentas.de(ventas, LocalDate.now(ResumenVentas.HORA_COLOMBIA)), ventas);
    }
}
