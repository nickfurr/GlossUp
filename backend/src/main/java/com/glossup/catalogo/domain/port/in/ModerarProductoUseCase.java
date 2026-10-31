package com.glossup.catalogo.domain.port.in;

import com.glossup.catalogo.domain.model.Producto;

/** Puerto de entrada: el administrador bloquea productos, por encima de lo que decida el vendedor. */
public interface ModerarProductoUseCase {

    Producto banear(Long productoId, String motivo);

    Producto desbanear(Long productoId);

    /** Saca del catálogo lo que venda un vendedor suspendido. */
    void suspenderVendedor(Long vendedorId);

    /** Devuelve solo lo que se bloqueó por suspender al vendedor, no lo bloqueado por otro motivo. */
    void reactivarVendedor(Long vendedorId);

    boolean tienePublicados(Long vendedorId);
}
