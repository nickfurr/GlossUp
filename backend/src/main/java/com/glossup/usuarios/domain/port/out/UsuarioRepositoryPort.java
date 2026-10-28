package com.glossup.usuarios.domain.port.out;

import com.glossup.usuarios.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(Long id);

    Usuario guardar(Usuario usuario);
}
