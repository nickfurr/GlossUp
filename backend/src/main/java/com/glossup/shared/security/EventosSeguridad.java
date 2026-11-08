package com.glossup.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registro de eventos de seguridad (auditoría R-04 / NC-T04; ISO/IEC 27002 8.15 y 8.16): inicios de sesión, accesos
 * denegados y cambios hechos por administradores, con quién actuó ({@code actor}), desde qué IP y cuándo (la hora la
 * pone el log). Van al logger "glossup.seguridad" en una línea {@code evento=... clave=valor} para filtrarlos y
 * alertar en Log Analytics. Solo ids: nunca correos, contraseñas, motivos escritos a mano ni datos de salud.
 */
public final class EventosSeguridad {

    private static final Logger LOG = LoggerFactory.getLogger("glossup.seguridad");

    private EventosSeguridad() {}

    /** Hecho normal que debe quedar trazado. {@code datos}: pares nombre, valor. */
    public static void registrar(String evento, Object... datos) {
        if (LOG.isInfoEnabled()) {
            LOG.info(linea(evento, datos));
        }
    }

    /** Intento fallido o rechazado: lo que cuentan las alertas. */
    public static void alertar(String evento, Object... datos) {
        if (LOG.isWarnEnabled()) {
            LOG.warn(linea(evento, datos));
        }
    }

    static String linea(String evento, Object... datos) {
        StringBuilder s = new StringBuilder("evento=").append(evento);
        for (int i = 0; i + 1 < datos.length; i += 2) {
            s.append(' ').append(datos[i]).append('=').append(limpio(datos[i + 1]));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            s.append(" actor=").append(limpio(auth.getName()));
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes peticion) {
            s.append(" ip=").append(limpio(peticion.getRequest().getRemoteAddr()));
        }
        return s.toString();
    }

    /** Sin espacios, '=' ni saltos de línea: un valor no puede inventar otro campo ni otra línea del registro (CWE-117). */
    private static String limpio(Object valor) {
        return valor == null ? "-" : valor.toString().replaceAll("[\\s=]+", "_");
    }
}
