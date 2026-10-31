package com.glossup.usuarios.application.service;

import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.RecuperarClaveUseCase;
import com.glossup.usuarios.domain.port.out.CorreoRecuperacionPort;
import com.glossup.usuarios.domain.port.out.RecuperacionClaveRepositoryPort;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class RecuperarClaveService implements RecuperarClaveUseCase {

    private static final Duration VIGENCIA_CORREO = Duration.ofHours(1);
    /** El administrador lo entrega por otro medio, que puede tardar más. */
    private static final Duration VIGENCIA_ADMIN = Duration.ofHours(24);
    /** Evita usar el formulario público para llenar de correos a alguien. */
    private static final Duration ESPERA_ENTRE_CORREOS = Duration.ofMinutes(2);

    private final SecureRandom azar = new SecureRandom();
    private final UsuarioRepositoryPort usuarios;
    private final RecuperacionClaveRepositoryPort enlaces;
    private final CorreoRecuperacionPort correo;
    private final PasswordEncoder encoder;
    private final String urlPublica;

    public RecuperarClaveService(UsuarioRepositoryPort usuarios, RecuperacionClaveRepositoryPort enlaces,
                                 CorreoRecuperacionPort correo, PasswordEncoder encoder,
                                 @Value("${glossup.url-publica}") String urlPublica) {
        this.usuarios = usuarios;
        this.enlaces = enlaces;
        this.correo = correo;
        this.encoder = encoder;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    @Override
    public void solicitar(String email) {
        usuarios.buscarPorEmail(email.trim().toLowerCase())
                .filter(Usuario::isActivo)
                .filter(u -> !enlaces.hayRecienteDe(u.getId(), OffsetDateTime.now().minus(ESPERA_ENTRE_CORREOS)))
                .ifPresent(u -> correo.enviarEnlace(u.getEmail(), u.getNombre(), crearEnlace(u, VIGENCIA_CORREO)));
    }

    @Override
    public String generarEnlace(Long usuarioId) {
        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + usuarioId));
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("Reactiva la cuenta antes de generar un enlace de recuperación");
        }
        return crearEnlace(usuario, VIGENCIA_ADMIN);
    }

    @Override
    public void restablecer(String token, String claveNueva) {
        Usuario usuario = enlaces.consumir(hash(token))
                .flatMap(usuarios::buscarPorId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("Este enlace no es válido o ya venció. Pide uno nuevo."));
        usuario.cambiarPasswordHash(encoder.encode(claveNueva));
        usuario.cerrarSesiones(OffsetDateTime.now()); // si alguien había entrado con la clave anterior, queda afuera
        usuarios.guardar(usuario);
    }

    private String crearEnlace(Usuario usuario, Duration vigencia) {
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        enlaces.guardar(hash(token), usuario.getId(), OffsetDateTime.now().plus(vigencia));
        return urlPublica + "/restablecer?token=" + token;
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
