package com.glossup.auditoria.domain.model;

import java.time.OffsetDateTime;

/**
 * Reporte consolidado de todas las pruebas RNF administrativas.
 */
public record ReporteConsolidadoRnf(
        OffsetDateTime generadoEn,
        boolean todosAprobados,
        ReporteCoberturaRnf24 rnf24,
        ReportePciRnf13 rnf13,
        ReporteInvimaRnf05c rnf05c
) {}
