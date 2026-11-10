package com.glossup.auditoria.infrastructure.out.jacoco;

import com.glossup.auditoria.domain.port.out.CoberturaCodigoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JacocoCoberturaAdapter - Pruebas del adaptador de JaCoCo")
class JacocoCoberturaAdapterTest {

    @Test
    @DisplayName("Debe leer exitosamente las métricas de jacoco.csv")
    void debeLeerMetricasDeJacoco() {
        JacocoCoberturaAdapter adapter = new JacocoCoberturaAdapter();
        CoberturaCodigoPort.DatosCobertura datos = adapter.obtenerCobertura(false);

        assertNotNull(datos);
        assertNotEquals("NO_DISPONIBLE", datos.fuente());
        assertTrue(datos.lineasCubiertasGlobal() > 0);
        assertFalse(datos.desglosePorModulo().isEmpty());
    }
}
