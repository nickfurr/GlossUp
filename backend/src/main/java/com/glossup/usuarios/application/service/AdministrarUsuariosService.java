package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import com.glossup.shared.security.EventosSeguridad;
import com.glossup.usuarios.domain.model.Clave;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AdministrarUsuariosUseCase;
import com.glossup.usuarios.domain.port.out.AvisoCuentaPort;
import com.glossup.usuarios.domain.port.out.ModeracionCatalogoPort;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Regla central: un administrador no puede suspenderse ni quitarse el rol a sí mismo. Como quien actúa
 * siempre es un administrador activo, eso garantiza que nunca se quede el sistema sin ninguno.
 */
@Service
@Transactional
public class AdministrarUsuariosService implements AdministrarUsuariosUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final ModeracionCatalogoPort catalogo;
    private final PasswordEncoder encoder;
    private final AvisoCuentaPort avisos;

    public AdministrarUsuariosService(UsuarioRepositoryPort usuarios, ModeracionCatalogoPort catalogo,
                                      PasswordEncoder encoder, AvisoCuentaPort avisos) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.encoder = encoder;
        this.avisos = avisos;
    }

    @Override
    public Usuario actualizarDatos(Long id, String nombre, String email) {
        Usuario usuario = buscar(id);
        String correo = email.trim().toLowerCase();
        usuarios.buscarPorEmail(correo).filter(otro -> !otro.getId().equals(id)).ifPresent(otro -> {
            throw new ReglaNegocioException("El correo '" + correo + "' ya está registrado");
        });
        usuario.cambiarNombre(nombre);
        usuario.cambiarEmail(correo);
        EventosSeguridad.registrar("ADMIN_EDITA_CUENTA", "usuario", id);
        return usuarios.guardar(usuario);
    }

    @Override
    public Usuario cambiarRol(Long adminId, Long id, String rol) {
        if (adminId.equals(id)) {
            throw new ReglaNegocioException("No puedes cambiar tu propio rol");
        }
        Usuario usuario = buscar(id);
        Rol nuevo;
        try {
            nuevo = Rol.valueOf(rol);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Rol no válido: " + rol);
        }
        if (usuario.tieneRol(Rol.VENDEDOR) && nuevo != Rol.VENDEDOR && catalogo.tienePublicados(id)) {
            throw new ReglaNegocioException("Este vendedor tiene productos publicados y se quedarían sin dueño. Bloquéalos o suspende la cuenta.");
        }
        usuario.cambiarRol(nuevo);
        usuario.cerrarSesiones(OffsetDateTime.now()); // el token guarda los roles: el anterior ya no debe valer
        EventosSeguridad.registrar("ADMIN_CAMBIA_ROL", "usuario", id, "rol", nuevo);
        return usuarios.guardar(usuario);
    }

    @Override
    public Usuario suspender(Long adminId, Long id, String motivo) {
        if (adminId.equals(id)) {
            throw new ReglaNegocioException("No puedes suspender tu propia cuenta");
        }
        Usuario usuario = buscar(id);
        usuario.suspender(motivo);
        if (usuario.tieneRol(Rol.VENDEDOR)) {
            catalogo.suspenderVendedor(id);
        }
        EventosSeguridad.registrar("ADMIN_SUSPENDE_CUENTA", "usuario", id);
        return usuarios.guardar(usuario);
    }

    @Override
    public Usuario reactivar(Long id) {
        Usuario usuario = buscar(id);
        usuario.reactivar();
        if (usuario.tieneRol(Rol.VENDEDOR)) {
            catalogo.reactivarVendedor(id);
        }
        EventosSeguridad.registrar("ADMIN_REACTIVA_CUENTA", "usuario", id);
        return usuarios.guardar(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.listarTodos();
    }

    @Override
    public Usuario crearVendedor(String nombre, String email, String clave) {
        String correo = email.trim().toLowerCase();
        if (usuarios.buscarPorEmail(correo).isPresent()) {
            throw new ReglaNegocioException("El correo '" + correo + "' ya está registrado");
        }
        Usuario creado = usuarios.guardar(Usuario.registrar(correo, encoder.encode(new Clave(clave).valor()), nombre, Rol.VENDEDOR));
        EventosSeguridad.registrar("ADMIN_CREA_VENDEDOR", "usuario", creado.getId());
        return creado;
    }

    @Override
    public Usuario cerrarSesiones(Long id) {
        Usuario usuario = buscar(id);
        usuario.cerrarSesiones(OffsetDateTime.now());
        EventosSeguridad.registrar("ADMIN_CIERRA_SESIONES", "usuario", id);
        return usuarios.guardar(usuario);
    }

    @Override
    public Usuario aprobarVendedor(Long adminId, Long id) {
        Usuario usuario = buscar(id);
        usuario.aprobar();
        Usuario aprobado = usuarios.guardar(usuario);
        EventosSeguridad.registrar("ADMIN_APRUEBA_VENDEDOR", "usuario", id);
        avisos.avisarAprobacion(aprobado.getEmail(), aprobado.getNombre());
        return aprobado;
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));
    }
}
