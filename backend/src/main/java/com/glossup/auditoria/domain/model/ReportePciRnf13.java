package com.glossup.auditoria.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resultado de evaluación del RNF-13:
 * Cumplimiento del estándar PCI-DSS garantizando que la plataforma NO almacena
 * datos sensibles de tarjetas (PAN, CVV/CVC, track, PIN) en bases de datos ni logs.
 */
public record ReportePciRnf13(
        String requisito,
        boolean cumple,
        OffsetDateTime fechaAuditoria,
        int totalTablasInspeccionadas,
        int totalColumnasInspeccionadas,
        List<String> columnasProhibidasDetectadas,
        List<String> columnasPasarelaVerificadas,
        long totalPedidosInspeccionados,
        long registrosPedidoConPatronesPan,
        int archivosLogAnalizados,
        int fugasEnLogsDetectadas,
        String conclusion
) {}
