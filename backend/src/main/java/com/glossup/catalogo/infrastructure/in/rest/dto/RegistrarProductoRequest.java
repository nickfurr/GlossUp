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

public record RegistrarProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        String nombre,

        @Size(max = 150, message = "La marca no puede superar 150 caracteres")
        String marca,

        @Size(max = 120, message = "La categoría no puede superar 120 caracteres")
        String categoria,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        BigDecimal precio,

        @Min(value = 0, message = "El stock no puede ser negativo")
        int stock,

        Double ph,

        @Size(max = 100, message = "El registro INVIMA no puede superar 100 caracteres")
        String registroInvima,

        // Topes de tamaño: se rechaza pronto en vez de procesar listas enormes (auditoría R-15).
        @Size(max = 10, message = "Máximo 10 imágenes por producto")
        List<@NotBlank(message = "La URL de la imagen no puede estar vacía") @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres") String> imagenes,

        @NotEmpty(message = "La ficha técnica debe contener al menos un ingrediente INCI")
        @Size(max = 100, message = "Máximo 100 ingredientes en la ficha técnica")
        List<@Valid IngredienteRequest> ingredientes,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion
) {
    public RegistrarProductoRequest(String nombre, String marca, String categoria, BigDecimal precio, int stock, Double ph, String registroInvima, List<String> imagenes, List<IngredienteRequest> ingredientes) {
        this(nombre, marca, categoria, precio, stock, ph, registroInvima, imagenes, ingredientes, null);
    }
}
