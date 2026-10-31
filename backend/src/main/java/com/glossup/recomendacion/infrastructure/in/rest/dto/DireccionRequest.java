package com.glossup.recomendacion.infrastructure.in.rest.dto;

import com.glossup.recomendacion.domain.model.Direccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos de una dirección de la libreta (HU14). */
public record DireccionRequest(
        @NotBlank(message = "Escribe quién recibe") @Size(max = 120) String nombreRecibe,
        @NotBlank(message = "Escribe un teléfono de contacto")
        @Pattern(regexp = "3\\d{9}|60\\d{8}", message = "Escribe un celular o fijo colombiano de 10 dígitos") String telefono,
        @NotBlank(message = "Elige el departamento") @Size(max = 40) String departamento,
        @NotBlank(message = "Escribe la ciudad o municipio") @Size(max = 80) String ciudad,
        @NotBlank(message = "Escribe el barrio") @Size(max = 80) String barrio,
        @NotBlank(message = "Escribe la dirección") @Size(max = 160) String direccion,
        @Size(max = 120) String complemento,
        @Size(max = 300) String indicaciones,
        boolean principal
) {
    public Direccion aDominio(Long usuarioId) {
        return new Direccion(null, usuarioId, nombreRecibe.trim(), telefono, departamento, ciudad.trim(), barrio.trim(), direccion.trim(),
                vacioANulo(complemento), vacioANulo(indicaciones), principal);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
