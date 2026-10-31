package com.glossup.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Servicio transversal de emision y validacion de JSON Web Tokens (RNF-11).
 *
 * <p>El secreto y la expiracion se inyectan desde la configuracion (application.yml / variables
 * de entorno), de modo que ningun valor sensible queda escrito en el codigo.</p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${glossup.security.jwt.secret}") String secret,
            @Value("${glossup.security.jwt.expiration-ms}") long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /** Genera un token firmado para el usuario indicado, incluyendo sus roles como claim. */
    public String generarToken(String subject, List<String> roles) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(key)
                .compact();
    }

    /** Devuelve el subject (identificador del usuario) si el token es valido. */
    public String extraerSubject(String token) {
        return parse(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        Object roles = parse(token).get("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }

    public java.time.Instant extraerEmision(String token) {
        java.util.Date emitido = parse(token).getIssuedAt();
        return emitido != null ? emitido.toInstant() : java.time.Instant.EPOCH;
    }

    public boolean esValido(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
