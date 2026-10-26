package com.glossup.perfil.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para agregar o quitar una afección del perfil (HU2). Recibe el código del
 * catálogo cerrado {@code Afeccion} (ej. ROSACEA), no texto libre.
 */
public record AfeccionRequest(
        @NotBlank(message = "El código de la afección es obligatorio")
        @Size(max = 120, message = "El nombre de la afección no puede superar 120 caracteres")
        String afeccion
) {}
