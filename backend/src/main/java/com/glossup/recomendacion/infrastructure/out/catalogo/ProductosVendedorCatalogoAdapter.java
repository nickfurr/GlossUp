package com.glossup.recomendacion.infrastructure.out.catalogo;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.ProductosVendedorPort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductosVendedorCatalogoAdapter implements ProductosVendedorPort {

    private final ProductoRepositoryPort productos;

    public ProductosVendedorCatalogoAdapter(ProductoRepositoryPort productos) {
        this.productos = productos;
    }

    @Override
    public Set<Long> idsDe(Long vendedorId) {
        return productos.buscarPorVendedor(vendedorId).stream().map(Producto::getId).collect(Collectors.toSet());
    }
}
