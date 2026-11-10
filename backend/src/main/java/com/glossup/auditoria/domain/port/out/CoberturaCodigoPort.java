package com.glossup.auditoria.domain.port.out;

import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24.ModuloCobertura;

import java.util.List;

/**
 * Puerto de salida para obtener métricas de cobertura de código (JaCoCo).
 */
public interface CoberturaCodigoPort {

    record DatosCobertura(
            int lineasCubiertasPerfil,
            int lineasPerdidasPerfil,
            int lineasCubiertasGlobal,
            int lineasPerdidasGlobal,
            String fuente,
            List<ModuloCobertura> desglosePorModulo
    ) {}

    DatosCobertura obtenerCobertura(boolean forzarEjecucionMaven);
}
