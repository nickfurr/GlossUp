package com.glossup.shared.security;

import java.time.Instant;

/**
 * Un token firmado no se puede retirar: esto permite rechazar el de un usuario suspendido, o el emitido
 * antes de cambiarle el rol o restablecerle la contraseña, sin esperar a que venza.
 */
public interface SesionVigentePort {

    boolean vigente(String subject, Instant emitidoEn);
}
