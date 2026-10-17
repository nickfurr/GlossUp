package com.glossup.perfil.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para agregar o quitar una afección o condición dermatológica del perfil (HU2).
 */
public record AfeccionRequest(
        @NotBlank(message = "El nombre de la afección o condición es obligatorio")
        @Size(max = 120, message = "El nombre de la afección no puede superar 120 caracteres")
        String afeccion
) {}
