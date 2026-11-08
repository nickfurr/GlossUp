package com.glossup.shared.config;

import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaveBcryptTest {

    private final SecurityConfig.ClaveBcrypt encoder = new SecurityConfig.ClaveBcrypt();

    @Test
    void claveDe72BytesSeGuardaYCoincide() {
        String clave = "a".repeat(72);
        assertTrue(encoder.matches(clave, encoder.encode(clave)));
    }

    @Test
    void claveDeMasDe72BytesSeRechazaAlGuardar() {
        assertThrows(ReglaNegocioException.class, () -> encoder.encode("a".repeat(73)));
        // 37 eñes son 74 bytes en UTF-8 aunque sean 37 caracteres.
        assertThrows(ReglaNegocioException.class, () -> encoder.encode("ñ".repeat(37)));
    }

    @Test
    void claveLargaAlIniciarSesionNoCoincideNiFalla() {
        String hash = encoder.encode("a".repeat(72));
        // Antes de CVE-2025-22228 esta clave coincidía: BCrypt ignoraba lo que pasaba de 72 bytes.
        assertFalse(encoder.matches("a".repeat(72) + "otra-cosa", hash));
    }
}
