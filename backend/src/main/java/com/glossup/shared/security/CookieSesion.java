package com.glossup.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/**
 * El token de sesión viaja en una cookie HttpOnly (auditoría OBS-T09 / R-01): el JavaScript de la página no puede
 * leerla, así que un XSS ya no se lleva la sesión como pasaba con localStorage. SameSite=Strict impide que otro sitio
 * la use, y Path=/api la limita a la API.
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "glossup_sesion";

    private final boolean segura;
    private final Duration duracion;

    public CookieSesion(@Value("${glossup.security.cookie-segura:true}") boolean segura,
                        @Value("${glossup.security.jwt.expiration-ms}") long expiracionMs) {
        this.segura = segura;
        this.duracion = Duration.ofMillis(expiracionMs);
    }

    /** Valor de Set-Cookie que abre la sesión con este token. */
    public String abrir(String token) {
        return cookie(token, duracion);
    }

    /** Valor de Set-Cookie que borra la sesión del navegador. */
    public String cerrar() {
        return cookie("", Duration.ZERO);
    }

    public static String leer(HttpServletRequest peticion) {
        Cookie cookie = WebUtils.getCookie(peticion, NOMBRE);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }

    private String cookie(String valor, Duration vida) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path("/api")
                .maxAge(vida)
                .build()
                .toString();
    }
}
