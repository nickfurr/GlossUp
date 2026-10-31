package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.CredencialesInvalidasException;
import com.glossup.shared.security.JwtService;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.AutenticacionResultado;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioUseCase;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticarUsuarioService implements AutenticarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticarUsuarioService(UsuarioRepositoryPort usuarioRepository,
                                    PasswordEncoder passwordEncoder,
                                    JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AutenticacionResultado autenticar(AutenticarUsuarioCommand command) {
        Usuario usuario = usuarioRepository.buscarPorEmail(command.email())
                .orElseThrow(CredencialesInvalidasException::new);

        if (usuario.getRoles().isEmpty() || !passwordEncoder.matches(command.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        // Solo después de comprobar la contraseña: así nadie averigua qué correos están suspendidos.
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("Tu cuenta está suspendida"
                    + (usuario.getMotivoSuspension() != null ? ". Motivo: " + usuario.getMotivoSuspension() : "."));
        }

        String token = jwtService.generarToken(
                usuario.getId().toString(),
                usuario.getRoles().stream().map(Enum::name).toList());
        return new AutenticacionResultado(token, usuario);
    }
}
