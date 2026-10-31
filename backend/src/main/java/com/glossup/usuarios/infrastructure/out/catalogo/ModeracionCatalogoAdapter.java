package com.glossup.usuarios.infrastructure.out.catalogo;

import com.glossup.catalogo.domain.port.in.ModerarProductoUseCase;
import com.glossup.usuarios.domain.port.out.ModeracionCatalogoPort;
import org.springframework.stereotype.Component;

@Component
public class ModeracionCatalogoAdapter implements ModeracionCatalogoPort {

    private final ModerarProductoUseCase productos;

    public ModeracionCatalogoAdapter(ModerarProductoUseCase productos) {
        this.productos = productos;
    }

    @Override
    public void suspenderVendedor(Long vendedorId) {
        productos.suspenderVendedor(vendedorId);
    }

    @Override
    public void reactivarVendedor(Long vendedorId) {
        productos.reactivarVendedor(vendedorId);
    }

    @Override
    public boolean tienePublicados(Long vendedorId) {
        return productos.tienePublicados(vendedorId);
    }
}
