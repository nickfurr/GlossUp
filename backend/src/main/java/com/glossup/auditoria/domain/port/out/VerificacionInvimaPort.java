package com.glossup.auditoria.domain.port.out;

import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c.ProductoInvimaDetalle;

import java.util.List;

/**
 * Puerto de salida para contrastar registros sanitarios de productos contra el regulador oficial (INVIMA / datos.gov.co).
 */
public interface VerificacionInvimaPort {

    record ResultadoVerificacionInvima(
            String fuente,
            int totalProductos,
            int productosVigentes,
            List<ProductoInvimaDetalle> detalles
    ) {}

    ResultadoVerificacionInvima verificarProductosActivos();
}
