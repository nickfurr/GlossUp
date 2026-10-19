package com.glossup.perfil.infrastructure.in.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/** Datos editables del biotipo dermatologico del cliente. */
public record ActualizarPerfilRequest(
        @DecimalMin(value = "0.0", message = "El pH debe estar entre 0 y 14")
        @DecimalMax(value = "14.0", message = "El pH debe estar entre 0 y 14")
        Double ph,
        String tipoPiel,
        @Size(max = 120, message = "El tipo de cabello no puede superar 120 caracteres")
        String tipoCabello
) {}
