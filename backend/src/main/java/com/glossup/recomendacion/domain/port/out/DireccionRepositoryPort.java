package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Direccion;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: libreta de direcciones del cliente (HU14). */
public interface DireccionRepositoryPort {

    /** Direcciones del cliente, la principal primero. */
    List<Direccion> listar(Long usuarioId);

    Optional<Direccion> buscar(Long id);

    Direccion guardar(Direccion direccion);

    void eliminar(Long id);

    void quitarPrincipal(Long usuarioId);
}
