package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.in.ModerarProductoUseCase;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ModerarProductoService implements ModerarProductoUseCase {

    private final ProductoRepositoryPort productos;

    public ModerarProductoService(ProductoRepositoryPort productos) {
        this.productos = productos;
    }

    @Override
    public Producto banear(Long productoId, String motivo) {
        Producto producto = buscar(productoId);
        producto.banear(motivo);
        return productos.guardar(producto);
    }

    @Override
    public Producto desbanear(Long productoId) {
        Producto producto = buscar(productoId);
        producto.desbanear();
        return productos.guardar(producto);
    }

    @Override
    public void suspenderVendedor(Long vendedorId) {
        productos.buscarPorVendedor(vendedorId).stream()
                .filter(p -> !p.isBaneado())
                .forEach(p -> {
                    p.banear(Producto.MOTIVO_VENDEDOR_SUSPENDIDO);
                    productos.guardar(p);
                });
    }

    @Override
    public void reactivarVendedor(Long vendedorId) {
        productos.buscarPorVendedor(vendedorId).stream()
                .filter(p -> Producto.MOTIVO_VENDEDOR_SUSPENDIDO.equals(p.getMotivoBaneo()))
                .forEach(p -> {
                    p.desbanear();
                    productos.guardar(p);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tienePublicados(Long vendedorId) {
        return productos.buscarPorVendedor(vendedorId).stream().anyMatch(p -> p.isActivo() && !p.isBaneado());
    }

    private Producto buscar(Long productoId) {
        return productos.buscarPorId(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + productoId));
    }
}
