package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.Direccion;

import java.util.List;

/** Puerto de entrada: libreta de direcciones del cliente (HU14). */
public interface GestionarDireccionesUseCase {

    List<Direccion> listar(Long usuarioId);

    Direccion crear(Direccion datos);

    Direccion actualizar(Long id, Direccion datos);

    void eliminar(Long usuarioId, Long id);
}
