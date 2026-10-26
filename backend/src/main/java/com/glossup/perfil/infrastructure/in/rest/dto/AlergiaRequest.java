package com.glossup.perfil.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para agregar o quitar una alergia del perfil (HU2). Recibe el código del
 * catálogo cerrado {@code Alergeno} (ej. PARABENOS), no texto libre.
 */
public record AlergiaRequest(
        @NotBlank(message = "El código de la alergia es obligatorio")
        @Size(max = 120, message = "El código de la alergia no puede superar 120 caracteres")
        String alergia
) {}
