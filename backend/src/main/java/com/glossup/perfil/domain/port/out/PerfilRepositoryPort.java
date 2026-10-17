package com.glossup.perfil.domain.port.out;

import com.glossup.perfil.domain.model.PerfilDermatologico;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia del agregado {@link PerfilDermatologico}.
 */
public interface PerfilRepositoryPort {

    Optional<PerfilDermatologico> buscarPorUsuarioId(Long usuarioId);

    PerfilDermatologico guardar(PerfilDermatologico perfil);
}
