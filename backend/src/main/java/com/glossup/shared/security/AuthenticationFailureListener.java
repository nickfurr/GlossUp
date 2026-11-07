package com.glossup.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

/**
 * Listener que intercepta eventos de fallo de autenticación de Spring Security
 * para registrar el fallo en el servicio de tarpitting.
 */
@Component
public class AuthenticationFailureListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFailureListener.class);

    private final LoginTarpitService tarpitService;

    public AuthenticationFailureListener(LoginTarpitService tarpitService) {
        this.tarpitService = tarpitService;
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object details = event.getAuthentication() != null ? event.getAuthentication().getDetails() : null;
        String ip = details instanceof String s ? s : "0.0.0.0";
        int intentos = tarpitService.registrarFallo(ip);
        log.debug("Fallo de credenciales registrado para IP [{}]. Total acumulado: {}", ip, intentos);
    }
}
