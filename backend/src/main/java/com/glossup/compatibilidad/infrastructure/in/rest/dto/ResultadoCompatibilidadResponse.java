package com.glossup.compatibilidad.infrastructure.in.rest.dto;

import com.glossup.compatibilidad.domain.model.Alerta;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;

import java.util.List;

/**
 * Respuesta de una evaluación de compatibilidad (HU6). {@code compatible} es false si hay alguna
 * alerta ALTA; las alertas BAJA/MEDIA informan sin bloquear.
 */
/** @param productoId producto evaluado (sirve para cruzar la respuesta por lote con el catálogo). */
public record ResultadoCompatibilidadResponse(Long productoId, boolean compatible, boolean recomendado,
                                              List<AlertaResponse> alertas, List<String> beneficios) {

    public record AlertaResponse(String severidad, String mensaje, String causa) {}

    public static ResultadoCompatibilidadResponse fromDomain(Long productoId, ResultadoCompatibilidad r) {
        List<AlertaResponse> alertas = r.getAlertas().stream()
                .map(ResultadoCompatibilidadResponse::aResponse)
                .toList();
        return new ResultadoCompatibilidadResponse(productoId, r.esCompatible(), r.esRecomendado(), alertas, r.getBeneficios());
    }

    private static AlertaResponse aResponse(Alerta a) {
        return new AlertaResponse(a.severidad().name(), a.mensaje(), a.causa());
    }
}
