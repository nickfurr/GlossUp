package com.glossup.recomendacion.infrastructure.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Carrito que el cliente envía para consolidar y reservar inventario (HU9). */
public record CarritoRequest(
        @NotEmpty(message = "El carrito debe tener al menos un producto")
        @Valid
        List<LineaCarritoRequest> items
) {}
