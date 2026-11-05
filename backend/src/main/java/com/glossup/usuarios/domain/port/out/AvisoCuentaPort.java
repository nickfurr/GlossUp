package com.glossup.usuarios.domain.port.out;

/** Puerto de salida: avisa al titular de cambios en su cuenta. No debe lanzar si falla el correo. */
public interface AvisoCuentaPort {

    /** El administrador aprobó la cuenta de vendedor: ya puede entrar (auditoría OBS-T08). */
    void avisarAprobacion(String email, String nombre);
}
