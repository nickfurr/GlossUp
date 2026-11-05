package com.glossup.shared.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * Id del usuario de la sesion: el subject del JWT. Falla si no hay sesion o el subject no es
 * numerico, en vez de suponer el usuario 1 o derivar un id con hashCode() (auditoria OBS-T01).
 */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    public static Long id(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Sesion no valida");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Sesion no valida");
        }
    }
}
