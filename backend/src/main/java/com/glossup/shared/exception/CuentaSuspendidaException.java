package com.glossup.shared.exception;

/** La contraseña era correcta pero el administrador suspendió la cuenta. {@code motivo} puede ser null en cuentas antiguas. */
public class CuentaSuspendidaException extends RuntimeException {

    private final String motivo;

    public CuentaSuspendidaException(String motivo) {
        super("Tu cuenta está suspendida");
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }
}
