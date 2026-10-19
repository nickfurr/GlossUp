package com.glossup.usuarios.domain.port.out;

import com.glossup.usuarios.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorEmail(String email);

    Usuario guardar(Usuario usuario);
}
