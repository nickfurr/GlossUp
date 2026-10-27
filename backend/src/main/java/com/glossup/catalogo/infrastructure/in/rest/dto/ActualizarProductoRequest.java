package com.glossup.catalogo.infrastructure.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para la modificación integral de un producto del catálogo (HU4 - SCRUM-31).
 */
public record ActualizarProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio")
        String nombre,

        String marca,

        String categoria,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        BigDecimal precio,

        @Min(value = 0, message = "El stock no puede ser negativo")
        int stock,

        Double ph,

        String registroInvima,

        List<@NotBlank(message = "La URL de la imagen no puede estar vacía") String> imagenes,

        @NotEmpty(message = "La ficha técnica debe contener al menos un ingrediente INCI")
        List<@Valid IngredienteRequest> ingredientes,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion
) {
    public ActualizarProductoRequest(String nombre, String marca, String categoria, BigDecimal precio, int stock, Double ph, String registroInvima, List<String> imagenes, List<IngredienteRequest> ingredientes) {
        this(nombre, marca, categoria, precio, stock, ph, registroInvima, imagenes, ingredientes, null);
    }
}
