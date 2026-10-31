package com.glossup.catalogo.infrastructure.in.rest.dto;

import com.glossup.catalogo.domain.model.ImagenProducto;
import com.glossup.catalogo.domain.model.Producto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductoResponse(
        Long id,
        Long vendedorId,
        String nombre,
        String marca,
        String categoria,
        BigDecimal precio,
        int stock,
        Double ph,
        String registroInvima,
        String descripcion,
        boolean activo,
        boolean baneado,
        /** Solo para el dueño y el administrador; ver {@link #conModeracion}. */
        String motivoBaneo,
        List<String> imagenes,
        List<IngredienteResponse> ingredientes,
        OffsetDateTime creadoEn
) {
    /** Para el dueño y el administrador: incluye el motivo del bloqueo, que el cliente no necesita ver. */
    public static ProductoResponse conModeracion(Producto producto) {
        return armar(producto, producto.getMotivoBaneo());
    }

    public static ProductoResponse fromDomain(Producto producto) {
        return armar(producto, null);
    }

    private static ProductoResponse armar(Producto producto, String motivoBaneo) {
        List<String> imagenes = producto.getImagenes().stream()
                .map(ImagenProducto::url)
                .toList();

        List<IngredienteResponse> ingredientes = producto.getComposicion().stream()
                .map(IngredienteResponse::fromDomain)
                .toList();

        return new ProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getMarca(),
                producto.getCategoria(),
                producto.getPrecio().monto(),
                producto.getStock(),
                producto.getPh(),
                producto.getRegistroInvima(),
                producto.getDescripcion(),
                producto.isActivo(),
                producto.isBaneado(),
                motivoBaneo,
                imagenes,
                ingredientes,
                producto.getCreadoEn()
        );
    }
}
