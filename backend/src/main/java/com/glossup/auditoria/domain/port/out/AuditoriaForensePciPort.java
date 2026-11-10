package com.glossup.auditoria.domain.port.out;

import java.util.List;

/**
 * Puerto de salida para ejecutar la auditoría forense PCI-DSS en base de datos y archivos de log.
 */
public interface AuditoriaForensePciPort {

    record ResultadoAuditoriaPci(
            int totalTablas,
            int totalColumnas,
            List<String> columnasProhibidasDetectadas,
            List<String> columnasPasarelaVerificadas,
            long totalPedidos,
            long registrosConPatronesPan,
            int archivosLogAnalizados,
            int fugasEnLogsDetectadas
    ) {}

    ResultadoAuditoriaPci ejecutarAuditoria();
}
