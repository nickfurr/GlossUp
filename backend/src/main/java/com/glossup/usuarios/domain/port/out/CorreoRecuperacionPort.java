package com.glossup.usuarios.domain.port.out;

/** Puerto de salida: envía el enlace de recuperación. No debe lanzar si falla el correo. */
public interface CorreoRecuperacionPort {

    void enviarEnlace(String email, String nombre, String enlace);
}
