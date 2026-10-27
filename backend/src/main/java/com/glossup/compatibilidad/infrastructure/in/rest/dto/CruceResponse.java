package com.glossup.compatibilidad.infrastructure.in.rest.dto;

import com.glossup.compatibilidad.domain.model.Cruce;

/** Choque entre dos productos (HU8), con su causa (RNF-05). */
public record CruceResponse(Long productoA, Long productoB, String severidad, String mensaje, String causa) {

    public static CruceResponse fromDomain(Cruce c) {
        return new CruceResponse(c.productoA(), c.productoB(), c.alerta().severidad().name(),
                c.alerta().mensaje(), c.alerta().causa());
    }
}
