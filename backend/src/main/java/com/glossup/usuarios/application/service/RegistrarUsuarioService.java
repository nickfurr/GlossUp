package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrarUsuarioService(UsuarioRepositoryPort usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Usuario registrar(RegistrarUsuarioCommand command) {
        String email = command.email() == null ? null : command.email().trim().toLowerCase();
        if (email != null && usuarioRepository.buscarPorEmail(email).isPresent()) {
            throw new ReglaNegocioException("Ese correo ya está registrado");
        }

        Rol rol = parseRolPublico(command.rol());
        Usuario usuario = Usuario.registrar(
                email,
                passwordEncoder.encode(command.password()),
                command.nombre(),
                rol);
        return usuarioRepository.guardar(usuario);
    }

    private Rol parseRolPublico(String valor) {
        try {
            Rol rol = Rol.valueOf(valor.trim().toUpperCase());
            if (rol == Rol.ADMIN) {
                throw new ReglaNegocioException("No es posible registrarse como administrador");
            }
            return rol;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ReglaNegocioException("El rol debe ser CLIENTE o VENDEDOR");
        }
    }
}
