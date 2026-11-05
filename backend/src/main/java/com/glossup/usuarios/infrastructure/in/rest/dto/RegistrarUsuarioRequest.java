package com.glossup.usuarios.infrastructure.in.rest.dto;

import com.glossup.usuarios.domain.model.Clave;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarUsuarioRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = Clave.LONGITUD_MINIMA, max = Clave.LONGITUD_MAXIMA, message = "La contraseña debe tener entre 8 y 120 caracteres")
        @Pattern(regexp = Clave.REGEX_COMPLEJIDAD, message = Clave.MENSAJE_REQUISITOS)
        String password,
        @NotBlank(message = "El rol es obligatorio")
        String rol
) {}
