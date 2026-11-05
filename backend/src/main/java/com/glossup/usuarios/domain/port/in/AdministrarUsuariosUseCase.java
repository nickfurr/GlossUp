package com.glossup.usuarios.domain.port.in;

import com.glossup.usuarios.domain.model.Usuario;

import java.util.List;

/** Puerto de entrada: lo que el administrador hace con las cuentas. {@code adminId} es quien actúa. */
public interface AdministrarUsuariosUseCase {

    Usuario actualizarDatos(Long id, String nombre, String email);

    Usuario cambiarRol(Long adminId, Long id, String rol);

    Usuario suspender(Long adminId, Long id, String motivo);

    Usuario reactivar(Long id);

    /** Todas las cuentas, por id. */
    List<Usuario> listar();

    /** Cuenta VENDEDOR creada por el administrador: queda aprobada. */
    Usuario crearVendedor(String nombre, String email, String clave);

    /** Revoca todas las sesiones abiertas de la cuenta, p. ej. si se sospecha que la robaron (auditoría OBS-T09). */
    Usuario cerrarSesiones(Long id);

    /** Aprueba un vendedor que se registró solo; ya puede entrar (auditoría OBS-T08). */
    Usuario aprobarVendedor(Long adminId, Long id);
}
