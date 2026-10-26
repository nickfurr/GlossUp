package com.glossup.compatibilidad.infrastructure.in.rest.dto;

import com.glossup.compatibilidad.domain.model.Alerta;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;

import java.util.List;

/**
 * Respuesta de una evaluación de compatibilidad (HU6). {@code compatible} es false si hay alguna
 * alerta ALTA; las alertas BAJA/MEDIA informan sin bloquear.
 */
public record ResultadoCompatibilidadResponse(boolean compatible, List<AlertaResponse> alertas) {

    public record AlertaResponse(String severidad, String mensaje, String causa) {}

    public static ResultadoCompatibilidadResponse fromDomain(ResultadoCompatibilidad r) {
        List<AlertaResponse> alertas = r.getAlertas().stream()
                .map(ResultadoCompatibilidadResponse::aResponse)
                .toList();
        return new ResultadoCompatibilidadResponse(r.esCompatible(), alertas);
    }

    private static AlertaResponse aResponse(Alerta a) {
        return new AlertaResponse(a.severidad().name(), a.mensaje(), a.causa());
    }
}
