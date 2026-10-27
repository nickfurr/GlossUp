package com.glossup.catalogo.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parámetros de entrada para el caso de uso de registro de producto.
 */
public record RegistrarProductoCommand(
        Long vendedorId,
        String nombre,
        String marca,
        String categoria,
        BigDecimal precio,
        int stock,
        Double ph,
        String registroInvima,
        List<String> urlsImagenes,
        List<IngredienteComando> ingredientes,
        String descripcion
) {
    public RegistrarProductoCommand(Long vendedorId, String nombre, String marca, String categoria, BigDecimal precio,
                                    int stock, Double ph, String registroInvima, List<String> urlsImagenes,
                                    List<IngredienteComando> ingredientes) {
        this(vendedorId, nombre, marca, categoria, precio, stock, ph, registroInvima, urlsImagenes, ingredientes, null);
    }

    /** {@code inci} debe existir en el catálogo; su función/descripción viene del catálogo. */
    public record IngredienteComando(
            String inci,
            Double concentracion
    ) {}
}
