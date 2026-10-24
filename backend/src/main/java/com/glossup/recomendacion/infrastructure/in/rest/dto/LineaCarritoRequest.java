package com.glossup.recomendacion.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Línea del carrito enviada por el cliente: producto y cantidad deseada (HU9). */
public record LineaCarritoRequest(
        @NotNull(message = "El id del producto es obligatorio")
        Long productoId,
        @Positive(message = "La cantidad debe ser mayor que cero")
        int cantidad
) {}
