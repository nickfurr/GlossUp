package com.glossup.usuarios.domain.port.in;

/** Puerto de entrada: el usuario administra su propia cuenta. */
public interface GestionarCuentaUseCase {

    void cambiarNombre(Long usuarioId, String nombre);

    void cambiarClave(Long usuarioId, String claveActual, String claveNueva);

    /** El titular suprime su cuenta; exige la contraseña actual (Ley 1581 art. 8 e). */
    void eliminarCuenta(Long usuarioId, String claveActual);
}
