package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AdministrarUsuariosUseCase;
import com.glossup.usuarios.domain.port.out.ModeracionCatalogoPort;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Regla central: un administrador no puede suspenderse ni quitarse el rol a sí mismo. Como quien actúa
 * siempre es un administrador activo, eso garantiza que nunca se quede el sistema sin ninguno.
 */
@Service
@Transactional
public class AdministrarUsuariosService implements AdministrarUsuariosUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdministrarUsuariosService.class);

    private final UsuarioRepositoryPort usuarios;
    private final ModeracionCatalogoPort catalogo;

    public AdministrarUsuariosService(UsuarioRepositoryPort usuarios, ModeracionCatalogoPort catalogo) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
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
        log.info("Admin {} cambió el rol del usuario {} a {}", adminId, id, nuevo);
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
        log.info("Admin {} suspendió al usuario {}", adminId, id);
        return usuarios.guardar(usuario);
    }

    @Override
    public Usuario reactivar(Long id) {
        Usuario usuario = buscar(id);
        usuario.reactivar();
        if (usuario.tieneRol(Rol.VENDEDOR)) {
            catalogo.reactivarVendedor(id);
        }
        return usuarios.guardar(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));
    }
}
