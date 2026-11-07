package com.glossup.usuarios.domain.model;

import com.glossup.shared.domain.ReglaNegocioException;

import java.util.regex.Pattern;

/**
 * Objeto de Valor (Value Object) del dominio que representa una contraseña en texto plano
 * antes de ser hasheada para su almacenamiento seguro.
 *
 * <p>Invariantes de complejidad:</p>
 * <ul>
 *   <li>Longitud entre 8 y 72 caracteres.</li>
 *   <li>Al menos una letra mayúscula (A-Z).</li>
 *   <li>Al menos una letra minúscula (a-z).</li>
 *   <li>Al menos un número (0-9).</li>
 *   <li>Al menos un carácter especial (símbolo o signo de puntuación).</li>
 * </ul>
 */
public record Clave(String valor) {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 72; // BCrypt solo usa 72 bytes (CVE-2025-22228); con tildes caben menos y lo frena el codificador

    public static final String REGEX_COMPLEJIDAD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,72}$";
    public static final String MENSAJE_REQUISITOS =
            "La contraseña debe tener entre 8 y 72 caracteres y combinar letras mayúsculas, minúsculas, números y caracteres especiales";

    private static final Pattern PATTERN_MAYUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern PATTERN_MINUSCULA = Pattern.compile("[a-z]");
    private static final Pattern PATTERN_NUMERO = Pattern.compile("[0-9]");
    private static final Pattern PATTERN_ESPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    public Clave {
        validar(valor);
    }

    public static Clave de(String valor) {
        return new Clave(valor);
    }

    public static void validar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("La contraseña es obligatoria");
        }
        if (valor.length() < LONGITUD_MINIMA) {
            throw new ReglaNegocioException("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new ReglaNegocioException("La contraseña no puede superar " + LONGITUD_MAXIMA + " caracteres");
        }
        if (!PATTERN_MAYUSCULA.matcher(valor).find()) {
            throw new ReglaNegocioException("La contraseña debe contener al menos una letra mayúscula");
        }
        if (!PATTERN_MINUSCULA.matcher(valor).find()) {
            throw new ReglaNegocioException("La contraseña debe contener al menos una letra minúscula");
        }
        if (!PATTERN_NUMERO.matcher(valor).find()) {
            throw new ReglaNegocioException("La contraseña debe contener al menos un número");
        }
        if (!PATTERN_ESPECIAL.matcher(valor).find()) {
            throw new ReglaNegocioException("La contraseña debe contener al menos un carácter especial");
        }
    }

    @Override
    public String toString() {
        return "Clave[PROTEGIDA]";
    }
}
