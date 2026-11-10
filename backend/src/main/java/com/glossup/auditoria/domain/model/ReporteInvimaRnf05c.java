package com.glossup.auditoria.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resultado de evaluación del RNF-05c:
 * Verificación regulatoria del 100% de productos activos del catálogo
 * contrastados contra el registro legal sanitario oficial del INVIMA (datos.gov.co).
 */
public record ReporteInvimaRnf05c(
        String requisito,
        boolean cumple,
        OffsetDateTime fechaVerificacion,
        String fuenteVerificacion,
        int totalProductosActivos,
        int productosConRegistroVigente,
        double porcentajeCumplimiento,
        List<ProductoInvimaDetalle> productosIncumplidos,
        List<ProductoInvimaDetalle> detalleProductos
) {
    public record ProductoInvimaDetalle(
            Long productoId,
            String nombre,
            String marca,
            String registroInvima,
            boolean formatoNsocValido,
            String estadoInvima,
            String titularInvima,
            String fechaVencimiento,
            String grupoInvima,
            boolean verificadoEnLinea
    ) {}
}
