package com.glossup.catalogo.infrastructure.in.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record IngredienteRequest(
        @NotBlank(message = "El nombre INCI del ingrediente es obligatorio")
        String inci,

        @DecimalMin(value = "0.0", message = "La concentración no puede ser negativa")
        @DecimalMax(value = "100.0", message = "La concentración no puede superar el 100%")
        Double concentracion
) {}
