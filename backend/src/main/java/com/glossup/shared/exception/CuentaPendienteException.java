package com.glossup.shared.exception;

/** La contraseña era correcta pero la cuenta de vendedor todavía espera la aprobación del administrador. */
public class CuentaPendienteException extends RuntimeException {

    public CuentaPendienteException() {
        super("Tu cuenta de vendedor está en revisión. Te avisaremos por correo cuando un administrador la apruebe.");
    }
}
