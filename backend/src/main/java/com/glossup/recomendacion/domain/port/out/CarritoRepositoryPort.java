package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.Carrito;

import java.util.Optional;

/**
 * Puerto de salida: persistencia del carrito activo del usuario (uno por usuario).
 */
public interface CarritoRepositoryPort {

    Optional<Carrito> buscarPorUsuario(Long usuarioId);

    Carrito guardar(Carrito carrito);
}
