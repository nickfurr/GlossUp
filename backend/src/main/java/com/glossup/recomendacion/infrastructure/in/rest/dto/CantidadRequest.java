package com.glossup.recomendacion.infrastructure.in.rest.dto;

import jakarta.validation.constraints.Positive;

/** Nueva cantidad de un ítem del carrito (HU9 - cambiar cantidad). */
public record CantidadRequest(
        @Positive(message = "La cantidad debe ser mayor que cero")
        int cantidad
) {}
