package com.glossup.auditoria.domain.port.in;

import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24;
import com.glossup.auditoria.domain.model.ReporteConsolidadoRnf;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c;
import com.glossup.auditoria.domain.model.ReportePciRnf13;

/**
 * Puerto de entrada primario para ejecutar pruebas y auditorías RNF administrativas.
 */
public interface EjecutarPruebasRnfUseCase {

    /**
     * Evalúa el RNF-24: testabilidad aislada del perfil y cobertura JaCoCo > 85%.
     * @param ejecutarMaven si es true y Maven está disponible, dispara mvn test jacoco:report en vivo.
     */
    ReporteCoberturaRnf24 evaluarRnf24(boolean ejecutarMaven);

    /**
     * Evalúa el RNF-13: auditoría forense PCI-DSS en PostgreSQL y logs.
     */
    ReportePciRnf13 evaluarRnf13();

    /**
     * Evalúa el RNF-05c: verificación sanitaria del 100% de productos contra datos.gov.co (INVIMA).
     */
    ReporteInvimaRnf05c evaluarRnf05c();

    /**
     * Ejecuta y consolida las tres pruebas en un solo reporte.
     */
    ReporteConsolidadoRnf evaluarTodos(boolean ejecutarMaven);
}
