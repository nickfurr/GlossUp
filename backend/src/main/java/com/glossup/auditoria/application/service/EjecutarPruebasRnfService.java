package com.glossup.auditoria.application.service;

import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24;
import com.glossup.auditoria.domain.model.ReporteConsolidadoRnf;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c.ProductoInvimaDetalle;
import com.glossup.auditoria.domain.model.ReportePciRnf13;
import com.glossup.auditoria.domain.port.in.EjecutarPruebasRnfUseCase;
import com.glossup.auditoria.domain.port.out.AuditoriaForensePciPort;
import com.glossup.auditoria.domain.port.out.AuditoriaForensePciPort.ResultadoAuditoriaPci;
import com.glossup.auditoria.domain.port.out.CoberturaCodigoPort;
import com.glossup.auditoria.domain.port.out.CoberturaCodigoPort.DatosCobertura;
import com.glossup.auditoria.domain.port.out.VerificacionInvimaPort;
import com.glossup.auditoria.domain.port.out.VerificacionInvimaPort.ResultadoVerificacionInvima;
import com.glossup.perfil.application.service.GestionarPerfilService;
import com.glossup.perfil.domain.model.Afeccion;
import com.glossup.perfil.domain.model.Alergeno;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.Ph;
import com.glossup.perfil.domain.model.TipoCabello;
import com.glossup.perfil.domain.model.TipoPiel;
import com.glossup.perfil.domain.port.out.PerfilRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de aplicación hexagonal que coordina la ejecución de pruebas y auditorías RNF.
 */
@Service
public class EjecutarPruebasRnfService implements EjecutarPruebasRnfUseCase {

    private final CoberturaCodigoPort coberturaPort;
    private final AuditoriaForensePciPort pciPort;
    private final VerificacionInvimaPort invimaPort;

    public EjecutarPruebasRnfService(CoberturaCodigoPort coberturaPort,
                                    AuditoriaForensePciPort pciPort,
                                    VerificacionInvimaPort invimaPort) {
        this.coberturaPort = coberturaPort;
        this.pciPort = pciPort;
        this.invimaPort = invimaPort;
    }

    @Override
    public ReporteCoberturaRnf24 evaluarRnf24(boolean ejecutarMaven) {
        // 1. Ejecutar batería de pruebas aisladas en memoria (testabilidad desacoplada de RNF-24)
        int[] resultadoAislado = ejecutarPruebasAisladasPerfil();
        int totalCasos = resultadoAislado[0];
        int casosExitosos = resultadoAislado[1];

        // 2. Obtener métricas de bytecode JaCoCo
        DatosCobertura cob = coberturaPort.obtenerCobertura(ejecutarMaven);

        double pctPerfil = calcularPorcentaje(cob.lineasCubiertasPerfil(), cob.lineasPerdidasPerfil());
        double pctGlobal = calcularPorcentaje(cob.lineasCubiertasGlobal(), cob.lineasPerdidasGlobal());

        // Criterio RNF-24: Cobertura > 85% y testabilidad 100% exitosa
        boolean cumple = (pctPerfil >= 85.0 || pctGlobal >= 85.0) && (casosExitosos == totalCasos && totalCasos > 0);

        return new ReporteCoberturaRnf24(
                "RNF-24",
                cumple,
                85.0,
                pctPerfil,
                pctGlobal,
                cob.lineasCubiertasGlobal(),
                cob.lineasPerdidasGlobal(),
                cob.lineasCubiertasGlobal() + cob.lineasPerdidasGlobal(),
                totalCasos,
                casosExitosos,
                cob.fuente(),
                cob.desglosePorModulo()
        );
    }

    @Override
    public ReportePciRnf13 evaluarRnf13() {
        ResultadoAuditoriaPci res = pciPort.ejecutarAuditoria();

        boolean cumple = res.columnasProhibidasDetectadas().isEmpty()
                && res.registrosConPatronesPan() == 0
                && res.fugasEnLogsDetectadas() == 0;

        String conclusion = cumple
                ? "Cumplimiento estricto PCI-DSS: Cero almacenamiento de PAN/CVV en base de datos ni logs del sistema."
                : "Se detectaron posibles vulnerabilidades o columnas no conformes con PCI-DSS.";

        return new ReportePciRnf13(
                "RNF-13",
                cumple,
                OffsetDateTime.now(),
                res.totalTablas(),
                res.totalColumnas(),
                res.columnasProhibidasDetectadas(),
                res.columnasPasarelaVerificadas(),
                res.totalPedidos(),
                res.registrosConPatronesPan(),
                res.archivosLogAnalizados(),
                res.fugasEnLogsDetectadas(),
                conclusion
        );
    }

    @Override
    public ReporteInvimaRnf05c evaluarRnf05c() {
        ResultadoVerificacionInvima res = invimaPort.verificarProductosActivos();

        List<ProductoInvimaDetalle> incumplidos = new ArrayList<>();
        for (ProductoInvimaDetalle det : res.detalles()) {
            if (!det.formatoNsocValido() || !"Vigente".equalsIgnoreCase(det.estadoInvima())) {
                incumplidos.add(det);
            }
        }

        double pct = res.totalProductos() > 0
                ? BigDecimal.valueOf((double) res.productosVigentes() * 100.0 / res.totalProductos())
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        boolean cumple = res.totalProductos() > 0 && incumplidos.isEmpty();

        return new ReporteInvimaRnf05c(
                "RNF-05c",
                cumple,
                OffsetDateTime.now(),
                res.fuente(),
                res.totalProductos(),
                res.productosVigentes(),
                pct,
                incumplidos,
                res.detalles()
        );
    }

    @Override
    public ReporteConsolidadoRnf evaluarTodos(boolean ejecutarMaven) {
        ReporteCoberturaRnf24 rnf24 = evaluarRnf24(ejecutarMaven);
        ReportePciRnf13 rnf13 = evaluarRnf13();
        ReporteInvimaRnf05c rnf05c = evaluarRnf05c();

        boolean todosAprobados = rnf24.cumple() && rnf13.cumple() && rnf05c.cumple();
        return new ReporteConsolidadoRnf(OffsetDateTime.now(), todosAprobados, rnf24, rnf13, rnf05c);
    }

    /**
     * Ejecuta en memoria una batería completa de pruebas aisladas del núcleo de dominio y aplicación
     * de perfil, certificando el desacoplamiento de infraestructura exigido en RNF-24.
     */
    private int[] ejecutarPruebasAisladasPerfil() {
        int total = 0;
        int exitosos = 0;

        // Caso 1: Creación de agregado y Value Object Ph válido
        try {
            total++;
            PerfilDermatologico p = PerfilDermatologico.crear(999L, new Ph(5.5), TipoPiel.MIXTA, TipoCabello.LISO_1A);
            if (p.getUsuarioId().equals(999L) && p.getPh().esFisiologico()) exitosos++;
        } catch (Exception ignored) {}

        // Caso 2: Rechazo estricto de pH fuera de rango
        try {
            total++;
            new Ph(15.5);
        } catch (ReglaNegocioException e) {
            exitosos++;
        }

        // Caso 3: Gestión dinámica de alergias sin duplicados
        try {
            total++;
            PerfilDermatologico p = PerfilDermatologico.crear(999L, null, null, null);
            p.agregarAlergia(Alergeno.FRAGANCIA);
            p.agregarAlergia(Alergeno.FRAGANCIA);
            if (p.getAlergias().size() == 1 && p.esAlergicoA(Alergeno.FRAGANCIA)) {
                p.quitarAlergia(Alergeno.FRAGANCIA);
                if (!p.esAlergicoA(Alergeno.FRAGANCIA)) exitosos++;
            }
        } catch (Exception ignored) {}

        // Caso 4: Gestión de afecciones dermatológicas
        try {
            total++;
            PerfilDermatologico p = PerfilDermatologico.crear(999L, null, null, null);
            p.agregarAfeccion(Afeccion.ACNE);
            if (p.tieneAfeccion(Afeccion.ACNE)) {
                p.quitarAfeccion(Afeccion.ACNE);
                if (!p.tieneAfeccion(Afeccion.ACNE)) exitosos++;
            }
        } catch (Exception ignored) {}

        // Caso 5: Consentimiento previo para datos sensibles de salud (Ley 1581)
        try {
            total++;
            PerfilDermatologico p = PerfilDermatologico.crear(999L, null, null, null);
            if (!p.tieneAutorizacionSensibles()) {
                p.autorizarDatosSensibles(OffsetDateTime.now());
                if (p.tieneAutorizacionSensibles()) exitosos++;
            }
        } catch (Exception ignored) {}

        // Caso 6: Orquestación del servicio con repositorio en memoria puro (Sin BD)
        try {
            total++;
            RepositorioEnMemoria repo = new RepositorioEnMemoria();
            GestionarPerfilService service = new GestionarPerfilService(repo);
            service.autorizarDatosSensibles(101L);
            service.actualizarBiotipo(101L, 6.0, "GRASA", "ONDULADO_2B");
            service.agregarAlergia(101L, "PARABENOS");

            PerfilDermatologico guardado = service.obtenerPerfil(101L);
            if (guardado.getTipoPiel() == TipoPiel.GRASA && guardado.esAlergicoA(Alergeno.PARABENOS)) {
                exitosos++;
            }
        } catch (Exception ignored) {}

        return new int[]{total, exitosos};
    }

    private static double calcularPorcentaje(int cubiertas, int perdidas) {
        int total = cubiertas + perdidas;
        if (total == 0) return 0.0;
        return BigDecimal.valueOf((double) cubiertas * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static class RepositorioEnMemoria implements PerfilRepositoryPort {
        private final Map<Long, PerfilDermatologico> tabla = new ConcurrentHashMap<>();

        @Override
        public Optional<PerfilDermatologico> buscarPorUsuarioId(Long usuarioId) {
            return Optional.ofNullable(tabla.get(usuarioId));
        }

        @Override
        public PerfilDermatologico guardar(PerfilDermatologico perfil) {
            tabla.put(perfil.getUsuarioId(), perfil);
            return perfil;
        }

        @Override
        public void eliminarPorUsuarioId(Long usuarioId) {
            tabla.remove(usuarioId);
        }
    }
}
