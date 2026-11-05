package com.glossup.catalogo.infrastructure.in.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngredienteRequest(
        @NotBlank(message = "El nombre INCI del ingrediente es obligatorio")
        @Size(max = 200, message = "El nombre INCI no puede superar 200 caracteres")
        String inci,

        @DecimalMin(value = "0.0", message = "La concentración no puede ser negativa")
        @DecimalMax(value = "100.0", message = "La concentración no puede superar el 100%")
        Double concentracion
) {}
