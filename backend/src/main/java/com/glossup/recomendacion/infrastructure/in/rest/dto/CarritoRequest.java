package com.glossup.recomendacion.infrastructure.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** Carrito que el cliente envía para consolidar y reservar inventario (HU9), con su entrega (HU14). */
public record CarritoRequest(
        @NotEmpty(message = "El carrito debe tener al menos un producto")
        @Valid
        List<LineaCarritoRequest> items,
        @NotNull(message = "Elige una dirección de entrega")
        Long direccionId,
        @NotNull(message = "Elige una forma de envío")
        @Pattern(regexp = "ESTANDAR|EXPRESS", message = "Forma de envío no válida")
        String metodoEnvio
) {}
