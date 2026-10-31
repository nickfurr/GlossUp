package com.glossup.usuarios.domain.port.in;

import com.glossup.usuarios.domain.model.Usuario;

/** Puerto de entrada: lo que el administrador hace con las cuentas. {@code adminId} es quien actúa. */
public interface AdministrarUsuariosUseCase {

    Usuario actualizarDatos(Long id, String nombre, String email);

    Usuario cambiarRol(Long adminId, Long id, String rol);

    Usuario suspender(Long adminId, Long id, String motivo);

    Usuario reactivar(Long id);
}
