package com.glossup.auditoria.application.service;

import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24;
import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24.ModuloCobertura;
import com.glossup.auditoria.domain.model.ReporteConsolidadoRnf;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c.ProductoInvimaDetalle;
import com.glossup.auditoria.domain.model.ReportePciRnf13;
import com.glossup.auditoria.domain.port.out.AuditoriaForensePciPort;
import com.glossup.auditoria.domain.port.out.AuditoriaForensePciPort.ResultadoAuditoriaPci;
import com.glossup.auditoria.domain.port.out.CoberturaCodigoPort;
import com.glossup.auditoria.domain.port.out.CoberturaCodigoPort.DatosCobertura;
import com.glossup.auditoria.domain.port.out.VerificacionInvimaPort;
import com.glossup.auditoria.domain.port.out.VerificacionInvimaPort.ResultadoVerificacionInvima;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EjecutarPruebasRnfServiceTest {

    @Mock
    private CoberturaCodigoPort coberturaPort;

    @Mock
    private AuditoriaForensePciPort pciPort;

    @Mock
    private VerificacionInvimaPort invimaPort;

    private EjecutarPruebasRnfService service;

    @BeforeEach
    void setUp() {
        service = new EjecutarPruebasRnfService(coberturaPort, pciPort, invimaPort);
    }

    @Test
    void evaluarRnf24_cuandoCoberturaSupera85Porciento_cumpleEsTrue() {
        DatosCobertura cob = new DatosCobertura(
                90, 10,
                880, 120,
                "TEST_JACOCO",
                List.of(new ModuloCobertura("perfil", 90, 10, 90.0))
        );
        when(coberturaPort.obtenerCobertura(false)).thenReturn(cob);

        ReporteCoberturaRnf24 reporte = service.evaluarRnf24(false);

        assertEquals("RNF-24", reporte.requisito());
        assertTrue(reporte.cumple());
        assertEquals(85.0, reporte.umbralMinimoPorcentaje());
        assertEquals(90.0, reporte.porcentajeCoberturaPerfil());
        assertEquals(88.0, reporte.porcentajeCoberturaGlobalDominioAplicacion());
        assertTrue(reporte.casosAisladosEjecutadosEnVivo() > 0);
        assertEquals(reporte.casosAisladosEjecutadosEnVivo(), reporte.casosAisladosExitosos());
    }

    @Test
    void evaluarRnf13_cuandoNoHayColumnasNiFugas_cumpleEsTrue() {
        ResultadoAuditoriaPci pci = new ResultadoAuditoriaPci(
                15, 80,
                List.of(),
                List.of("pedido.pago_transaccion_id"),
                100, 0,
                2, 0
        );
        when(pciPort.ejecutarAuditoria()).thenReturn(pci);

        ReportePciRnf13 reporte = service.evaluarRnf13();

        assertEquals("RNF-13", reporte.requisito());
        assertTrue(reporte.cumple());
        assertEquals(0, reporte.registrosPedidoConPatronesPan());
        assertEquals(0, reporte.fugasEnLogsDetectadas());
        assertTrue(reporte.conclusion().contains("Cumplimiento estricto"));
    }

    @Test
    void evaluarRnf05c_cuandoTodosLosProductosSonVigentes_cumpleEsTrue() {
        ProductoInvimaDetalle det = new ProductoInvimaDetalle(
                1L, "Crema Facial", "GlossQA", "NSOC12345-20CO",
                true, "Vigente", "Titular S.A.S.", "2030-01-01", "COSMETICOS", true
        );
        ResultadoVerificacionInvima invima = new ResultadoVerificacionInvima(
                "DATOS_GOV_CO", 1, 1, List.of(det)
        );
        when(invimaPort.verificarProductosActivos()).thenReturn(invima);

        ReporteInvimaRnf05c reporte = service.evaluarRnf05c();

        assertEquals("RNF-05c", reporte.requisito());
        assertTrue(reporte.cumple());
        assertEquals(100.0, reporte.porcentajeCumplimiento());
        assertTrue(reporte.productosIncumplidos().isEmpty());
    }

    @Test
    void evaluarTodos_consolidaLasTresPruebas() {
        DatosCobertura cob = new DatosCobertura(100, 0, 900, 100, "TEST", List.of());
        when(coberturaPort.obtenerCobertura(true)).thenReturn(cob);

        ResultadoAuditoriaPci pci = new ResultadoAuditoriaPci(10, 50, List.of(), List.of(), 50, 0, 1, 0);
        when(pciPort.ejecutarAuditoria()).thenReturn(pci);

        ResultadoVerificacionInvima invima = new ResultadoVerificacionInvima("TEST", 0, 0, List.of());
        when(invimaPort.verificarProductosActivos()).thenReturn(invima);

        ReporteConsolidadoRnf consolidado = service.evaluarTodos(true);

        assertNotNull(consolidado.generadoEn());
        assertNotNull(consolidado.rnf24());
        assertNotNull(consolidado.rnf13());
        assertNotNull(consolidado.rnf05c());
        verify(coberturaPort).obtenerCobertura(true);
        verify(pciPort).ejecutarAuditoria();
        verify(invimaPort).verificarProductosActivos();
    }
}
