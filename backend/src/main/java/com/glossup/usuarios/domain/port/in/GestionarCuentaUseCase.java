package com.glossup.usuarios.domain.port.in;

/** Puerto de entrada: el usuario administra su propia cuenta. */
public interface GestionarCuentaUseCase {

    void cambiarNombre(Long usuarioId, String nombre);

    /** Cambia la contraseña y cierra las sesiones abiertas en otros dispositivos. */
    void cambiarClave(Long usuarioId, String claveActual, String claveNueva);

    /** Revoca todas las sesiones abiertas de la cuenta, en cualquier dispositivo (auditoría OBS-T09). */
    void cerrarSesiones(Long usuarioId);

    /** El titular suprime su cuenta; exige la contraseña actual (Ley 1581 art. 8 e). */
    void eliminarCuenta(Long usuarioId, String claveActual);
}
