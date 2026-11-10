package com.glossup.auditoria.infrastructure.in.rest;

import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24;
import com.glossup.auditoria.domain.model.ReporteConsolidadoRnf;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c;
import com.glossup.auditoria.domain.model.ReportePciRnf13;
import com.glossup.auditoria.domain.port.in.EjecutarPruebasRnfUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para administración:
 * Expone la ejecución de pruebas técnicas y auditorías de Requisitos No Funcionales (RNF).
 */
@RestController
@RequestMapping("/api/admin/pruebas-rnf")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoría RNF", description = "Endpoints de auditoría y ejecución de pruebas de Requisitos No Funcionales (RNF-24, RNF-13, RNF-05c)")
@SecurityRequirement(name = "bearerAuth")
public class AdminPruebasRnfController {

    private final EjecutarPruebasRnfUseCase pruebasUseCase;

    public AdminPruebasRnfController(EjecutarPruebasRnfUseCase pruebasUseCase) {
        this.pruebasUseCase = pruebasUseCase;
    }

    @GetMapping
    @Operation(summary = "Ejecutar y consolidar todas las pruebas RNF administrativas (RNF-24, RNF-13, RNF-05c)")
    public ReporteConsolidadoRnf ejecutarTodas(
            @RequestParam(name = "ejecutarMaven", defaultValue = "false") boolean ejecutarMaven) {
        return pruebasUseCase.evaluarTodos(ejecutarMaven);
    }

    @GetMapping("/rnf-24")
    @Operation(summary = "RNF-24 · Cobertura de pruebas de dominio/aplicación > 85% y testabilidad aislada")
    public ReporteCoberturaRnf24 evaluarRnf24(
            @RequestParam(name = "ejecutarMaven", defaultValue = "false") boolean ejecutarMaven) {
        return pruebasUseCase.evaluarRnf24(ejecutarMaven);
    }

    @GetMapping("/rnf-13")
    @Operation(summary = "RNF-13 · Auditoría forense PCI-DSS: cero almacenamiento físico de tarjetas en PostgreSQL y logs")
    public ReportePciRnf13 evaluarRnf13() {
        return pruebasUseCase.evaluarRnf13();
    }

    @GetMapping("/rnf-05c")
    @Operation(summary = "RNF-05c · Auditoría de registros sanitarios INVIMA en tiempo real contra datos.gov.co")
    public ReporteInvimaRnf05c evaluarRnf05c() {
        return pruebasUseCase.evaluarRnf05c();
    }
}
