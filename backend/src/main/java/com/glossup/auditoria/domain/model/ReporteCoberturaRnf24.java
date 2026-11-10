package com.glossup.auditoria.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Resultado de evaluación del RNF-24:
 * Cobertura de pruebas del módulo de perfil y capas de dominio/aplicación > 85%,
 * sumado a la testabilidad aislada de la lógica de negocio sin infraestructura.
 */
public record ReporteCoberturaRnf24(
        String requisito,
        boolean cumple,
        double umbralMinimoPorcentaje,
        double porcentajeCoberturaPerfil,
        double porcentajeCoberturaGlobalDominioAplicacion,
        int lineasCubiertas,
        int lineasPerdidas,
        int lineasTotales,
        int casosAisladosEjecutadosEnVivo,
        int casosAisladosExitosos,
        String fuenteReporte,
        List<ModuloCobertura> desglosePorModulo
) {
    public record ModuloCobertura(
            String modulo,
            int lineasCubiertas,
            int lineasPerdidas,
            double porcentaje
    ) {}
}
