package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.security.EventosSeguridad;
import com.glossup.usuarios.domain.model.Clave;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrarUsuarioService(UsuarioRepositoryPort usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Versión de la política publicada en /privacidad; cambiarla obliga a pedir de nuevo la autorización. */
    public static final String POLITICA_VIGENTE = "1.0";

    @Override
    public Usuario registrar(RegistrarUsuarioCommand command) {
        if (!command.aceptaPolitica()) {
            throw new ReglaNegocioException("Debes aceptar la Política de Tratamiento de Datos para crear la cuenta");
        }
        if (!command.mayorDeEdad()) {
            throw new ReglaNegocioException("Debes ser mayor de 18 años para crear una cuenta");
        }
        String email = command.email() == null ? null : command.email().trim().toLowerCase();
        if (email != null && usuarioRepository.buscarPorEmail(email).isPresent()) {
            throw new ReglaNegocioException("Ese correo ya está registrado");
        }

        Rol rol = parseRolPublico(command.rol());
        Clave clave = new Clave(command.password());
        Usuario usuario = Usuario.registrar(
                email,
                passwordEncoder.encode(clave.valor()),
                command.nombre(),
                rol);
        usuario.aceptarPolitica(POLITICA_VIGENTE, OffsetDateTime.now());
        Usuario creado = usuarioRepository.guardar(usuario);
        EventosSeguridad.registrar("CUENTA_CREADA", "usuario", creado.getId(), "rol", rol);
        return creado;
    }

    // El registro público solo crea clientes; los vendedores los crea un administrador (AdministrarUsuariosService).
    private Rol parseRolPublico(String valor) {
        if (valor == null || valor.isBlank() || valor.trim().equalsIgnoreCase(Rol.CLIENTE.name())) {
            return Rol.CLIENTE;
        }
        throw new ReglaNegocioException("Solo puedes registrarte como cliente; las cuentas de vendedor las crea un administrador");
    }
}
