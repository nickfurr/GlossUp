package com.glossup.perfil.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para agregar o quitar una alergia (nombre INCI) del perfil dermatológico (HU2).
 */
public record AlergiaRequest(
        @NotBlank(message = "El nombre INCI de la alergia es obligatorio")
        @Size(max = 120, message = "El nombre de la alergia no puede superar 120 caracteres")
        String alergia
) {}
