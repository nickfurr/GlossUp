package com.glossup.usuarios.domain.port.out;

/** Puerto de salida: lo que suspender o cambiar de rol a un vendedor implica para sus productos. */
public interface ModeracionCatalogoPort {

    void suspenderVendedor(Long vendedorId);

    void reactivarVendedor(Long vendedorId);

    boolean tienePublicados(Long vendedorId);
}
