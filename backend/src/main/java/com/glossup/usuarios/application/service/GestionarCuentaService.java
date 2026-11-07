package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import com.glossup.shared.security.EventosSeguridad;
import com.glossup.usuarios.domain.model.Clave;
import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.GestionarCuentaUseCase;
import com.glossup.usuarios.domain.port.out.DatosPersonalesPort;
import com.glossup.usuarios.domain.port.out.ModeracionCatalogoPort;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Cambios de la cuenta propia. Cambiar la contraseña exige la actual. */
@Service
@Transactional
public class GestionarCuentaService implements GestionarCuentaUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final PasswordEncoder encoder;
    private final ModeracionCatalogoPort catalogo;
    private final DatosPersonalesPort datosPersonales;

    public GestionarCuentaService(UsuarioRepositoryPort usuarios, PasswordEncoder encoder,
                                  ModeracionCatalogoPort catalogo, DatosPersonalesPort datosPersonales) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.catalogo = catalogo;
        this.datosPersonales = datosPersonales;
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
        Clave nueva = new Clave(claveNueva);
        usuario.cambiarPasswordHash(encoder.encode(nueva.valor()));
        usuario.cerrarSesiones(OffsetDateTime.now()); // quien tuviera la clave anterior queda afuera; el controlador renueva esta sesión
        usuarios.guardar(usuario);
        EventosSeguridad.registrar("CLAVE_CAMBIADA", "usuario", usuarioId);
    }

    @Override
    public void cerrarSesiones(Long usuarioId) {
        Usuario usuario = buscar(usuarioId);
        usuario.cerrarSesiones(OffsetDateTime.now());
        usuarios.guardar(usuario);
        EventosSeguridad.registrar("SESIONES_CERRADAS", "usuario", usuarioId);
    }

    @Override
    public void eliminarCuenta(Long usuarioId, String claveActual) {
        Usuario usuario = buscar(usuarioId);
        if (!encoder.matches(claveActual, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (usuario.tieneRol(Rol.ADMIN)) {
            // Sin esta regla el sistema podría quedarse sin ningún administrador.
            throw new ReglaNegocioException("Un administrador no puede eliminar su cuenta; pide a otro administrador que le quite el rol");
        }
        if (usuario.tieneRol(Rol.VENDEDOR)) {
            catalogo.suspenderVendedor(usuarioId);
        }
        datosPersonales.suprimir(usuarioId);
        // Contraseña aleatoria que nadie conoce: la cuenta no puede volver a usarse.
        usuario.suprimir(encoder.encode(UUID.randomUUID().toString()), OffsetDateTime.now());
        usuarios.guardar(usuario);
        EventosSeguridad.registrar("CUENTA_ELIMINADA", "usuario", usuarioId);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + usuarioId));
    }
}
