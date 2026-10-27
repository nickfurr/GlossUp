package com.glossup.catalogo.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Comando de entrada para actualizar un producto existente del catálogo (HU4 - SCRUM-31).
 */
public record ActualizarProductoCommand(
        Long productoId,
        Long vendedorId,
        String nombre,
        String marca,
        String categoria,
        BigDecimal precio,
        int stock,
        Double ph,
        String registroInvima,
        List<String> urlsImagenes,
        List<RegistrarProductoCommand.IngredienteComando> ingredientes,
        String descripcion
) {
    public ActualizarProductoCommand(Long productoId, Long vendedorId, String nombre, String marca, String categoria,
                                     BigDecimal precio, int stock, Double ph, String registroInvima,
                                     List<String> urlsImagenes, List<RegistrarProductoCommand.IngredienteComando> ingredientes) {
        this(productoId, vendedorId, nombre, marca, categoria, precio, stock, ph, registroInvima, urlsImagenes,
                ingredientes, null);
    }
}
