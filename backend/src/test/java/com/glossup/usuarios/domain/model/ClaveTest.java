package com.glossup.usuarios.domain.model;

import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ClaveTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "ClaveSegura#1",
            "P@ssw0rd2024",
            "Cosmetico!1",
            "GlossUP.2026",
            "Admin_Seguro_99"
    })
    void claveValida_seCreaSinErrores(String raw) {
        Clave clave = new Clave(raw);
        assertEquals(raw, clave.valor());
    }

    @Test
    void claveNula_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave(null));
        assertEquals("La contraseña es obligatoria", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void claveVacia_lanzaExcepcion(String valor) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave(valor));
        assertEquals("La contraseña es obligatoria", ex.getMessage());
    }

    @Test
    void claveCorta_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave("Aa1!xyz"));
        assertEquals("La contraseña debe tener al menos 8 caracteres", ex.getMessage());
    }

    @Test
    void claveDemasiadoLarga_lanzaExcepcion() {
        String larguisima = "A1!" + "a".repeat(118); // 121 caracteres
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave(larguisima));
        assertEquals("La contraseña no puede superar 120 caracteres", ex.getMessage());
    }

    @Test
    void claveSinMayuscula_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave("clavesegura#1"));
        assertEquals("La contraseña debe contener al menos una letra mayúscula", ex.getMessage());
    }

    @Test
    void claveSinMinuscula_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave("CLAVESEGURA#1"));
        assertEquals("La contraseña debe contener al menos una letra minúscula", ex.getMessage());
    }

    @Test
    void claveSinNumero_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave("ClaveSegura#X"));
        assertEquals("La contraseña debe contener al menos un número", ex.getMessage());
    }

    @Test
    void claveSinCaracterEspecial_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> new Clave("ClaveSegura123"));
        assertEquals("La contraseña debe contener al menos un carácter especial", ex.getMessage());
    }

    @Test
    void toString_noExponeContrasenaEnClaro() {
        Clave clave = new Clave("ClaveSegura#1");
        assertEquals("Clave[PROTEGIDA]", clave.toString());
        assertFalse(clave.toString().contains("ClaveSegura#1"));
    }
}
