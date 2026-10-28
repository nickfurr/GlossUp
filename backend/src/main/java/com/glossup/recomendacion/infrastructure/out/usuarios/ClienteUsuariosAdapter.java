package com.glossup.recomendacion.infrastructure.out.usuarios;

import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Implementa {@link ClientePort} con el módulo de usuarios: nombre y correo para el comprobante (HU11). */
@Component
public class ClienteUsuariosAdapter implements ClientePort {

    private final UsuarioRepositoryPort usuarios;

    public ClienteUsuariosAdapter(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<Cliente> buscar(Long usuarioId) {
        return usuarios.buscarPorId(usuarioId).map(u -> new Cliente(u.getNombre(), u.getEmail()));
    }
}
