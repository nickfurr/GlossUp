package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TarifaEnvioTest {

    private static Dinero estandar(String departamento, double subtotal) {
        return TarifaEnvio.opciones(departamento, Dinero.de(subtotal)).get(0).costo();
    }

    @Test
    void elCostoDependeDeLaZona() {
        assertEquals(Dinero.de(6000), estandar("Quindío", 50000));
        assertEquals(Dinero.de(12000), estandar("Antioquia", 50000));
        assertEquals(Dinero.de(28000), estandar("Amazonas", 50000));
    }

    @Test
    void elEstandarEsGratisDesdeElMinimoSalvoEnDificilAcceso() {
        assertEquals(Dinero.de(12000), estandar("Antioquia", 149999));
        assertEquals(Dinero.cero(), estandar("Antioquia", 150000));
        assertEquals(Dinero.de(28000), estandar("Amazonas", 500000));
    }

    @Test
    void elExpressNoEsGratisYNoLlegaADificilAcceso() {
        assertEquals(Dinero.de(22000), TarifaEnvio.opciones("Antioquia", Dinero.de(500000)).get(1).costo());
        assertEquals(List.of(MetodoEnvio.ESTANDAR),
                TarifaEnvio.opciones("Vaupés", Dinero.de(50000)).stream().map(TarifaEnvio.Opcion::metodo).toList());
    }

    @Test
    void departamentoInventado_falla() {
        assertThrows(ReglaNegocioException.class, () -> TarifaEnvio.opciones("Narnia", Dinero.de(50000)));
    }

    @Test
    void laFechaEstimadaSaltaElFinDeSemana() {
        LocalDate viernes = LocalDate.of(2026, 10, 30);
        assertEquals(LocalDate.of(2026, 11, 2), TarifaEnvio.fechaEstimada(viernes, 1));
        assertEquals(LocalDate.of(2026, 11, 3), TarifaEnvio.fechaEstimada(viernes, 2));
        assertEquals(LocalDate.of(2026, 11, 5), TarifaEnvio.fechaEstimada(LocalDate.of(2026, 10, 31), 4));
    }
}
