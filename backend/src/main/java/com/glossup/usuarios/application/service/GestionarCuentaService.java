package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.GestionarCuentaUseCase;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cambios de la cuenta propia. Cambiar la contraseña exige la actual. */
@Service
@Transactional
public class GestionarCuentaService implements GestionarCuentaUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final PasswordEncoder encoder;

    public GestionarCuentaService(UsuarioRepositoryPort usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void cambiarNombre(Long usuarioId, String nombre) {
        Usuario usuario = buscar(usuarioId);
        usuario.cambiarNombre(nombre);
        usuarios.guardar(usuario);
    }

    @Override
    public void cambiarClave(Long usuarioId, String claveActual, String claveNueva) {
        Usuario usuario = buscar(usuarioId);
        if (!encoder.matches(claveActual, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        // shortcut: los tokens ya emitidos siguen válidos hasta vencer (1 h); revocarlos requiere una versión de sesión en el JWT.
        usuario.cambiarPasswordHash(encoder.encode(claveNueva));
        usuarios.guardar(usuario);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + usuarioId));
    }
}
